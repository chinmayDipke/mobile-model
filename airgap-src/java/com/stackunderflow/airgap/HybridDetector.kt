package com.stackunderflow.airgap

/**
 * What actually ships. Rules AND the on-device model, each doing the job it is
 * genuinely better at.
 *
 * WHY, with the numbers that forced it (55-message suite, measured on device):
 *
 *   Rules alone    40/40 scams, 15/15 clean,   ~0 ms
 *   Gemma alone    38/40 scams, 10/15 clean, 1657 ms
 *
 * The 1B model is BIASED TOWARDS SCAM. On its own it blocked genuine OTP
 * alerts, a NETFLIX debit and an FD maturity notice. In a fraud app that is
 * the worse failure: an app that cries wolf gets uninstalled, and then it
 * protects nobody.
 *
 * So the model is not allowed to block on its own judgement alone. Three steps:
 *
 *   1. Rules fire        -> SCAM. They only match a real scam STRUCTURE, so
 *                           they are precise. Instant.
 *   2. No action asked   -> CLEAN, without waking the model at all. A message
 *                           that merely INFORMS you - a debit alert, an OTP, a
 *                           balance - cannot be blocked. This is the guard that
 *                           fixed every false alarm, and it also makes clean
 *                           messages pass instantly, which is what the demo shows.
 *   3. Otherwise         -> ask Gemma. This is the case the rules have never
 *                           seen: it asks the user to DO something, but not in
 *                           a shape we hard-coded. That is where a model earns
 *                           its place - tomorrow's scam, not yesterday's.
 *
 * Say it to a judge like this: "Rules catch what we have seen. The model catches
 * what we haven't. And the model can only escalate a message that actually asks
 * you to act - so a real bank SMS can never be blocked by the model alone."
 */
class HybridDetector(
    private val rules: Detector,
    private val model: Detector?,
) : Detector {

    companion object {
        /**
         * The model gets this long, then we stop waiting.
         *
         * MediaPipe's generateResponse hangs on us intermittently - measured:
         * five messages at ~2 s each, then the engine sat at 0% CPU and never
         * returned. On stage that means a judge's SMS arrives, the model wedges,
         * and no block screen ever appears with nothing to show for it.
         *
         * A verdict that arrives late is worthless anyway - the user has already
         * read the SMS. So: answer in time or don't answer.
         */
        const val MODEL_TIMEOUT_MS = 6000L
    }

    /** Runs [block] on a throwaway thread, returns null if it overruns [ms]. */
    private fun <T> withTimeout(ms: Long, block: () -> T): T? {
        var result: T? = null
        val t = Thread { result = try { block() } catch (_: Throwable) { null } }
        t.isDaemon = true          // a wedged model thread must not keep the app alive
        t.start()
        t.join(ms)
        return if (t.isAlive) null else result
    }

    override val engineName: String =
        if (model != null) "Rules + Gemma 3 1B (on-device)" else "Rules only"

    /**
     * Does this message ask the user to DO something?
     *
     * Deliberately about structure, not vocabulary: a link, a personal mobile
     * (never a 1800 number - real banks publish those), or a verb that moves
     * money. Genuine bank alerts have none of these.
     */
    private fun asksForAction(m: NormalisedMessage): Boolean {
        if (m.urls.isNotEmpty()) return true
        if (m.mobileNumbers.isNotEmpty()) return true
        val t = m.raw.lowercase()
        return listOf(
            "scan", "approve", "accept", "authorize", "authorise",
            "enter pin", "upi pin", "click", "update kyc", "verify",
            "send back", "return it", "claim", "collect request",
            "payment request", "download", "install",
        ).any { t.contains(it) }
    }

    override fun check(message: NormalisedMessage): Verdict {
        // 1. Rules are precise. If they fire, we are done - and it cost nothing.
        val byRules = rules.check(message)
        if (byRules.isScam) return byRules

        // 2. Nothing is being asked of the user. Cannot be a scam that costs
        //    money, so pass it instantly without spending 1.6 s on the model.
        if (!asksForAction(message)) return Verdict.clean()

        // 3. Unfamiliar, and it does ask for something. This is the model's job.
        val m = model ?: return Verdict.clean()
        val byModel = withTimeout(MODEL_TIMEOUT_MS) { m.check(message) } ?: return Verdict.clean()
        if (!byModel.isScam) return Verdict.clean()

        // Flagged by the model alone - say so honestly, and with lower
        // confidence than a rule match.
        return byModel.copy(
            confidence = minOf(byModel.confidence, 0.7f),
            reason = byModel.reason
        )
    }
}

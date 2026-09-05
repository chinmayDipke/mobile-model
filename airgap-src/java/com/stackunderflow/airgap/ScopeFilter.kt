package com.stackunderflow.airgap

/**
 * GATE 0. Runs before the normaliser, before the rules, before the model.
 *
 * The direct answer to "it reads all my SMS - put filters on it", asked by an
 * evaluator at Eval Round 1. A fraud detector has no business reading a message
 * about dinner. If a message is in neither category below we drop it here: it is
 * never normalised, never scored, never stored, never counted as anything but a
 * number.
 *
 * TWO ways in, not one:
 *
 *  - it mentions MONEY, or
 *  - it asks you to DO something
 *
 * The second is not padding. "Cyber Cell notice. A case is registered against
 * your Aadhaar. Join the video call now to verify your identity" is a digital
 * arrest scam and one of the ones only the model catches - and it names no
 * amount, no bank, no link. A money-only filter drops it and we lose the best
 * evidence we have that the model earns its place. Every scam asks you to act,
 * even when it never mentions rupees.
 *
 * Deliberately cheap and deliberately dumb. This gate is allowed to let through
 * things that turn out to be innocent. It is NOT allowed to be a second
 * detector - deciding is the job of the two behind it.
 */
object ScopeFilter {

    /** Payment apps whose notifications we watch. Always in scope. */
    private val PAYMENT_APPS = setOf(
        "com.google.android.apps.nbu.paisa.user",
        "net.one97.paytm",
        "com.phonepe.app",
        "in.org.npci.upiapp",
    )

    /**
     * Indian commercial SMS arrives from a DLT header - AX-HDFCBK, VM-SBIINB,
     * JD-PAYTM. A personal message arrives from a 10-digit number. Bank traffic
     * is almost all the first kind.
     */
    private val DLT_HEADER = Regex("^[A-Z]{2}-?[A-Z0-9]{4,8}$", RegexOption.IGNORE_CASE)

    private val CURRENCY = Regex("(?:rs\\.?|inr|₹)\\s?[0-9]", RegexOption.IGNORE_CASE)

    private val LINK = Regex(
        "(?:https?://|www\\.|\\b[a-z0-9-]{2,}\\.(?:in|com|xyz|top|net|link|info|site|online))",
        RegexOption.IGNORE_CASE
    )

    private val MONEY_WORDS = listOf(
        "upi", "bank", "a/c", "acct", "account", "debit", "debited", "credit",
        "credited", "payment", "paytm", "phonepe", "gpay", "npci", "kyc",
        "atm", "card", "wallet", "refund", "cashback", "txn", "transaction",
        "balance", "otp", "imps", "neft", "rtgs", "emi", "loan", "rupee",
        "money", "subsidy", "fastag", "toll", "bill", "recharge", "cash",
    )

    /**
     * Verbs that move money or hand over control. A scam always has one of
     * these, because a scam needs the victim to act.
     */
    private val ACTION_WORDS = listOf(
        "scan", "approve", "accept", "authorize", "authorise", "enter pin",
        "click", "verify", "update", "download", "install", "renew",
        "send back", "return it", "wapas", "claim", "collect request",
        "payment request", "join the", "call our", "call now", "contact",
        "blocked", "suspend", "expire", "deactivat", "disconnect", "arrest",
    )

    /** Why a message was or was not examined. Numbers only - never the text. */
    data class Decision(val examine: Boolean, val why: String)

    fun decide(sender: String, body: String): Decision {
        if (sender in PAYMENT_APPS) return Decision(true, "from a payment app")

        val t = body.lowercase()
        if (CURRENCY.containsMatchIn(t)) return Decision(true, "mentions an amount")
        if (LINK.containsMatchIn(t)) return Decision(true, "contains a link")
        if (MONEY_WORDS.any { t.contains(it) }) return Decision(true, "mentions money")
        if (ACTION_WORDS.any { t.contains(it) }) return Decision(true, "asks you to act")
        if (DLT_HEADER.matches(sender.trim()) && t.length > 40)
            return Decision(true, "commercial sender")

        // Neither about money nor asking anything of you. Not our business.
        return Decision(false, "no money signal")
    }
}

package com.stackunderflow.airgap

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.io.File

/**
 * The real detector. Gemma 3 1B, fully on-device, no network.
 *
 * Measured on iQOO I2501 / SM8850, 5 Sept:
 *   cold load 1579 ms, inference 370 ms, correct SCAM verdict on testset S01.
 *
 * Drop-in replacement for StubDetector - same Detector interface, so nothing
 * else in the app changes. See spike-model-load/RESULTS.md.
 */
class GemmaDetector(private val context: Context) : Detector {

    override val engineName = "Gemma 3 1B (on-device)"

    companion object {
        // Same path on both phones.
        const val MODEL_PATH = "/data/local/tmp/llm/gemma3-1b-int4.task"

        /** True if the model file is actually present, so callers can fall back. */
        fun isAvailable(): Boolean = File(MODEL_PATH).let { it.exists() && it.canRead() }
    }

    /**
     * Built once and reused - creating this per message would cost 1.5 s each time.
     *
     * CAREFUL: maxTokens is the total budget for INPUT + OUTPUT, not a cap on
     * the answer. Setting it below the prompt length crashes the native engine
     * ("input_size(201) was not less than maxTokens(8)"). 512 leaves plenty of
     * room for our ~150 token prompt.
     *
     * Speed comes from the PROMPT, not from this number: the model stops after
     * one word because that is all we ask for. Asking for a written reason
     * instead cost ~12 s per message. The reason text comes from REASONS below.
     */
    private val llm: LlmInference by lazy {
        LlmInference.createFromOptions(
            context,
            LlmInference.LlmInferenceOptions.builder()
                .setModelPath(MODEL_PATH)
                .setMaxTokens(512)
                // Caps sampling at the single most likely token, i.e. greedy.
                // Set on the engine rather than per-session: see ask() below.
                .setMaxTopK(1)
                .build()
        )
    }

    /** Warm the model up at app start so the first real SMS is not slow. */
    fun preload() { llm }

    /**
     * We want GREEDY decoding: the default samples at topK=40, temperature=0.8,
     * which is right for chat and wrong for a classifier - the same SMS could
     * get a different answer on a re-run, and on stage that is unacceptable.
     *
     * DO NOT reintroduce a per-message LlmInferenceSession here. Creating one
     * per call and closing it deadlocked the engine: the process stayed alive
     * holding the model at 762 MB with 0% CPU, and the run never finished.
     * Cost us a test cycle. setMaxTopK(1) on the engine above gets greedy
     * behaviour on the path that is known to work.
     */
    private fun ask(prompt: String): String = llm.generateResponse(prompt).trim()

    override fun check(message: NormalisedMessage): Verdict {
        val answer = try {
            ask(buildPrompt(message))
        } catch (t: Throwable) {
            // Never let the detector crash the app. Fail OPEN (treat as clean)
            // rather than blocking a genuine message the user needs.
            return Verdict.clean()
        }
        return parse(answer, message)
    }

    /**
     * ONE WORD out. Generation time scales with output length, so this is the
     * single biggest lever on speed: a written reason cost ~12 s per message,
     * one word costs ~0.4 s.
     */
    private fun buildPrompt(m: NormalisedMessage): String = buildString {
        append("Classify Indian bank SMS as SCAM or CLEAN.\n\n")
        append("SCAM = it wants an ACTION from you: enter a PIN or approve a request ")
        append("to RECEIVE money, open a link to fix KYC, scan a QR to get cash, ")
        append("call a personal 10-digit number, or return money \"sent by mistake\".\n")
        append("CLEAN = it only TELLS you something: a debit or credit alert, an OTP, ")
        append("a balance, a bill due, an EMI, a statement, an FD maturing. ")
        append("These often mention amounts, account numbers and 1800 numbers. ")
        append("A message that only informs you is CLEAN even if it mentions money.\n\n")
        // Few-shot. A 1B model needs to be shown, not just told.
        append("SMS: Rs.199 debited from HDFC Card xx7723 at NETFLIX. Avl limit Rs.48,801.\n")
        append("Answer: CLEAN\n\n")
        append("SMS: Your KYC has EXPIRED. Account blocked in 24hrs. Update: sbi-kyc-verify.in\n")
        append("Answer: SCAM\n\n")
        append("SMS: INR 12,000 credited to A/c XX8832 via NEFT from RAHUL SHARMA.\n")
        append("Answer: CLEAN\n\n")
        append("SMS: You won Rs.5,000 cashback. Scan the QR to receive the amount.\n")
        append("Answer: SCAM\n\n")
        append("SMS: ").append(m.compact).append('\n')
        if (m.urls.isNotEmpty()) append("(contains link ").append(m.urls.first()).append(")\n")
        if (m.mobileNumbers.isNotEmpty()) append("(contains a personal 10-digit number)\n")
        append("Answer:")
    }

    /**
     * Written by us, not by the model. Three reasons:
     *  - speed: the model only has to emit one token
     *  - quality: a 1B model writes clumsy English; these are plain and calm,
     *    which matters because they are also READ ALOUD to older users
     *  - reliability: no parsing of free text on stage
     */
    private val REASONS = mapOf(
        "kyc_link" to "Your bank will never ask you to update KYC through a link in an SMS. This link is fake.",
        "qr_cashback" to "Scanning a QR code can only SEND money, never receive it. This is a trap.",
        "collect_refund" to "This is a request for YOUR money, not a refund. Approving it will debit your account.",
        "fake_care" to "This is a personal mobile number, not a bank helpline. Real banks use 1800 numbers.",
        "wrong_transfer" to "The classic wrong transfer trick. No money came in. Do not send anything back.",
        "qr_send_only" to "Scanning this code can only SEND your money, never receive it.",
    )

    private fun parse(answer: String, m: NormalisedMessage): Verdict {
        val upper = answer.uppercase()
        val isScam = when {
            upper.contains("SCAM") -> true
            upper.contains("CLEAN") -> false
            else -> false   // unclear - fail open rather than block a real message
        }
        if (!isScam) return Verdict.clean()

        val pattern = guessPattern(m)
        return Verdict(
            isScam = true,
            pattern = pattern,
            confidence = 0.8f,
            reason = REASONS[pattern] ?: "This message looks like a payment scam."
        )
    }

    /**
     * The model gives us scam/clean and a reason; the pattern label comes from
     * structure, which is more reliable than asking a 1B model to pick an enum.
     */
    private fun guessPattern(m: NormalisedMessage): String {
        val t = m.raw.lowercase()
        return when {
            t.contains("kyc") || t.contains("aadhaar") || t.contains("pan card") -> "kyc_link"
            t.contains("qr") || t.contains("scan") -> "qr_cashback"
            t.contains("collect request") || t.contains("approve") || t.contains("upi pin") -> "collect_refund"
            m.mobileNumbers.isNotEmpty() && (t.contains("call") || t.contains("customer care") || t.contains("helpline")) -> "fake_care"
            t.contains("by mistake") || t.contains("wrong") || t.contains("galti") -> "wrong_transfer"
            else -> "kyc_link"
        }
    }

    fun close() { try { llm.close() } catch (_: Throwable) {} }
}

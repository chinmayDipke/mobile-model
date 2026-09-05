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

    /** Built once and reused - creating this per message would cost 1.5 s each time. */
    private val llm: LlmInference by lazy {
        LlmInference.createFromOptions(
            context,
            LlmInference.LlmInferenceOptions.builder()
                .setModelPath(MODEL_PATH)
                .setMaxTokens(256)
                .build()
        )
    }

    /** Warm the model up at app start so the first real SMS is not slow. */
    fun preload() { llm }

    override fun check(message: NormalisedMessage): Verdict {
        val answer = try {
            llm.generateResponse(buildPrompt(message)).trim()
        } catch (t: Throwable) {
            // Never let the detector crash the app. Fail OPEN (treat as clean)
            // rather than blocking a genuine message the user needs.
            return Verdict.clean()
        }
        return parse(answer, message)
    }

    /**
     * Short prompt on purpose. The normaliser already stripped the noise, and
     * short input is what keeps inference near 370 ms.
     */
    private fun buildPrompt(m: NormalisedMessage): String = """
You are a fraud detector for Indian bank and payment SMS.

Reply with EXACTLY two lines and nothing else:
VERDICT: <SCAM or CLEAN>
REASON: <one short sentence a first-time phone user would understand>

Rules:
- Real banks never ask you to approve or enter a UPI PIN to RECEIVE money.
- Real banks do not send shortened or lookalike links to update KYC.
- A personal 10-digit number given as "customer care" is a scam. 1800 numbers are normal.
- A plain transaction alert, OTP, balance or statement message is CLEAN.

Message: "${m.compact}"
${if (m.urls.isNotEmpty()) "Links: ${m.urls.joinToString(", ")}" else ""}
${if (m.mobileNumbers.isNotEmpty()) "Personal numbers in text: ${m.mobileNumbers.joinToString(", ")}" else ""}
""".trim()

    private fun parse(answer: String, m: NormalisedMessage): Verdict {
        val upper = answer.uppercase()
        val isScam = when {
            upper.contains("VERDICT: SCAM") -> true
            upper.contains("VERDICT: CLEAN") -> false
            // model ignored the format - fall back to whichever word it used
            upper.contains("SCAM") -> true
            else -> false
        }
        if (!isScam) return Verdict.clean()

        val reason = answer.lineSequence()
            .firstOrNull { it.trim().uppercase().startsWith("REASON:") }
            ?.substringAfter(":")?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "This message looks like a payment scam."

        return Verdict(
            isScam = true,
            pattern = guessPattern(m),
            confidence = 0.8f,
            reason = reason
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

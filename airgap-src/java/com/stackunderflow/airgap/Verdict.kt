package com.stackunderflow.airgap

/**
 * THE CONTRACT between the app half (Chinmay) and the model half (Shan).
 * Do not change this shape without telling the other person.
 */
data class Verdict(
    val isScam: Boolean,
    val pattern: String,      // kyc_link | qr_cashback | collect_refund | fake_care | wrong_transfer | clean
    val confidence: Float,    // 0.0 .. 1.0
    val reason: String        // shown on the block screen AND read aloud
) {
    companion object {
        fun clean() = Verdict(false, "clean", 1.0f, "Nothing suspicious found.")
    }
}

/**
 * Shan implements this with the on-device model.
 * StubDetector ships now so the app works before the model lands.
 * Must be safe to call OFF the main thread.
 */
interface Detector {
    fun check(message: NormalisedMessage): Verdict
    /** Shown in the app so judges can see which engine actually ran. */
    val engineName: String
}

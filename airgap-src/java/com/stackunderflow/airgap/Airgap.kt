package com.stackunderflow.airgap

import android.content.Context
import android.content.Intent

/**
 * One place that ties the pieces together, so the SMS receiver and the
 * notification listener always behave identically.
 */
object Airgap {

    /** Swap this one line when Shan's model is ready. Nothing else changes. */
    @Volatile
    var detector: Detector = StubDetector()

    fun handleMessage(context: Context, sender: String, body: String): Verdict {
        val normalised = Normaliser.normalise(sender, body)
        val verdict = detector.check(normalised)
        if (verdict.isScam) showBlock(context, verdict, body)
        return verdict
    }

    fun showBlock(context: Context, verdict: Verdict, originalMessage: String) {
        val i = Intent(context, BlockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(BlockActivity.EXTRA_PATTERN, verdict.pattern)
            putExtra(BlockActivity.EXTRA_REASON, verdict.reason)
            putExtra(BlockActivity.EXTRA_MESSAGE, originalMessage)
            putExtra(BlockActivity.EXTRA_CONFIDENCE, verdict.confidence)
            putExtra(BlockActivity.EXTRA_ENGINE, detector.engineName)
        }
        context.startActivity(i)
    }
}

package com.stackunderflow.airgap

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

/**
 * One place that ties the pieces together, so the SMS receiver and the
 * notification listener always behave identically.
 */
object Airgap {

    /**
     * ONE background thread for all detection. Two reasons:
     *  1. Gemma takes ~370 ms. On the main thread that freezes the UI, and in a
     *     BroadcastReceiver it blows the ~10 s limit and Android kills us.
     *  2. LlmInference is NOT thread safe. A single worker serialises access,
     *     so two SMS arriving together cannot corrupt each other.
     */
    private val worker = Executors.newSingleThreadExecutor { r ->
        Thread(r, "airgap-detector").apply { isDaemon = true }
    }
    private val main = Handler(Looper.getMainLooper())

    /** Swap this one line when Shan's model is ready. Nothing else changes. */
    @Volatile
    var detector: Detector = StubDetector()

    /**
     * Call once at app start, OFF the main thread (loading Gemma takes ~1.5 s).
     *
     * Uses the on-device model if its file is present, otherwise stays on the
     * rules stub - so the app always works, including on a phone we have not
     * pushed the model to yet. Never leaves the app with no detector at all.
     */
    fun initDetector(context: Context) {
        detector = try {
            if (GemmaDetector.isAvailable()) {
                GemmaDetector(context.applicationContext).also { it.preload() }
            } else {
                StubDetector()
            }
        } catch (t: Throwable) {
            StubDetector()
        }
    }

    /**
     * USE THIS EVERYWHERE. Runs detection off the main thread, then shows the
     * block screen on the main thread if needed.
     *
     * @param onResult called on the main thread, also for clean messages, so
     *                 the UI can say "nothing happened, and that is correct".
     * @param onFinally called on the main thread after everything - use it to
     *                 release a BroadcastReceiver's goAsync() token.
     */
    fun handleMessageAsync(
        context: Context,
        sender: String,
        body: String,
        onResult: ((Verdict) -> Unit)? = null,
        onFinally: (() -> Unit)? = null,
    ) {
        val app = context.applicationContext
        worker.execute {
            val verdict = try {
                detector.check(Normaliser.normalise(sender, body))
            } catch (t: Throwable) {
                Verdict.clean()   // never let detection crash the demo
            }
            main.post {
                try {
                    if (verdict.isScam) showBlock(app, verdict, body)
                    onResult?.invoke(verdict)
                } finally {
                    onFinally?.invoke()
                }
            }
        }
    }

    /**
     * Blocking version. Only safe to call from a thread you already own -
     * TestSetRunner does. Calling this from the main thread or from
     * BroadcastReceiver.onReceive will freeze the app once Gemma is the engine.
     */
    fun handleMessage(context: Context, sender: String, body: String): Verdict {
        val normalised = Normaliser.normalise(sender, body)
        val verdict = detector.check(normalised)
        if (verdict.isScam) showBlock(context.applicationContext, verdict, body)
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

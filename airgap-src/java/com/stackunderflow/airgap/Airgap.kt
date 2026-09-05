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

    /** Last few message bodies we acted on, so we do not act on them again. */
    private val recent = ArrayDeque<Pair<String, Long>>()
    private const val DUPLICATE_WINDOW_MS = 60_000L

    @Synchronized
    private fun isDuplicate(body: String): Boolean {
        val now = System.currentTimeMillis()
        while (recent.isNotEmpty() && now - recent.first().second > DUPLICATE_WINDOW_MS) {
            recent.removeFirst()
        }
        val key = body.trim()
        if (recent.any { it.first == key }) return true
        recent.addLast(key to now)
        if (recent.size > 20) recent.removeFirst()
        return false
    }

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
    @Volatile
    private var detectorReady = false

    @Synchronized
    fun initDetector(context: Context) {
        if (detectorReady) return
        val rules = StubDetector()
        detector = try {
            val model = if (GemmaDetector.isAvailable()) {
                GemmaDetector(context.applicationContext).also { it.preload() }
            } else null
            HybridDetector(rules, model)
        } catch (t: Throwable) {
            HybridDetector(rules, null)   // model missing or failed to load
        }
        detectorReady = true
    }

    /**
     * MUST be called before any detection, from EVERY entry point.
     *
     * The bug this fixes: initDetector was only called from MainActivity. When a
     * message arrives and the app process is not running, Android starts the
     * process for the notification listener or the SMS receiver - MainActivity
     * never runs, so Gemma was never loaded and we silently fell back to the
     * rules stub. On stage the block screen would have said "Rules (stub)"
     * instead of naming the model, with nothing to indicate anything was wrong.
     */
    private fun ensureDetector(context: Context) {
        if (!detectorReady) initDetector(context)
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

        // GATE 0. Before anything else: is this even our business? A message
        // that neither mentions money nor asks you to do something is dropped
        // here - never normalised, never scored, never stored. Only the counter
        // moves, and the counter holds no text.
        val scope = ScopeFilter.decide(sender, body)
        Stats.recordArrival(app, scope.examine)
        if (!scope.examine) {
            onFinally?.let { main.post(it) }
            return
        }

        // One SMS reaches us up to five times: SmsReceiver catches it, then the
        // notification listener catches the Messages app's notification, which
        // Android re-posts as it updates. Measured on device, 5 hits in 13 s.
        // Without this the block screen would open five times in front of a judge.
        if (isDuplicate(body)) {
            onFinally?.let { main.post(it) }
            return
        }

        worker.execute {
            ensureDetector(app)   // process may have started without MainActivity
            val verdict = try {
                detector.check(Normaliser.normalise(sender, body))
            } catch (t: Throwable) {
                Verdict.clean()   // never let detection crash the demo
            }
            main.post {
                try {
                    if (verdict.isScam) {
                        Stats.recordBlocked(app)
                        History.record(app, verdict, body, sender)
                        showBlock(app, verdict, body)
                    }
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

package com.stackunderflow.airgap

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Runs the 55-message test set WITHOUT touching the UI, and prints the result
 * to logcat. Lets us re-run the suite from the laptop while the phone is busy
 * doing something else (Office Kit mirroring, Remote PC, a demo rehearsal).
 *
 * Trigger:
 *   adb shell am broadcast -a com.stackunderflow.airgap.RUN_TESTS \
 *     -n com.stackunderflow.airgap/.DevTestReceiver
 *
 * Read:
 *   adb logcat -s AIRGAP_TEST
 *
 * Dev tool only. Harmless if it ships, but it is not part of the product.
 */
class DevTestReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION = "com.stackunderflow.airgap.RUN_TESTS"
        private const val TAG = "AIRGAP_TEST"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val app = context.applicationContext

        Thread {
            try {
                Airgap.initDetector(app)
                val d = Airgap.detector
                Log.i(TAG, "START engine=${d.engineName}")

                val t0 = System.currentTimeMillis()
                val r = TestSetRunner.run(app, d)
                val wall = System.currentTimeMillis() - t0

                val total = r.scamTotal + r.genuineTotal
                Log.i(TAG, "ENGINE      ${d.engineName}")
                Log.i(TAG, "SCAMS       ${r.scamCaught}/${r.scamTotal}")
                Log.i(TAG, "CLEAN       ${r.genuinePassed}/${r.genuineTotal}")
                Log.i(TAG, "TOTAL_MS    $wall  (${wall / maxOf(total, 1)} ms per message)")
                Log.i(TAG, "MISSED      ${if (r.missed.isEmpty()) "none" else r.missed.joinToString(", ")}")
                Log.i(TAG, "FALSEALARMS ${if (r.falseAlarms.isEmpty()) "none" else r.falseAlarms.joinToString(", ")}")
                Log.i(TAG, "DONE")
            } catch (t: Throwable) {
                Log.e(TAG, "FAILED ${t::class.java.simpleName}: ${t.message}", t)
                Log.i(TAG, "DONE")
            }
        }.start()
    }
}

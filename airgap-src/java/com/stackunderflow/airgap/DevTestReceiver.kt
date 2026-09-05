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
        // Hand off to the foreground service. Doing the work here would let
        // Android kill the process the moment onReceive returns.
        Log.i(TAG, "handing off to DetectionService")
        DetectionService.runTests(context.applicationContext)
    }
}

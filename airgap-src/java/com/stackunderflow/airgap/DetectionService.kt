package com.stackunderflow.airgap

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log

/**
 * Runs detection inside a FOREGROUND service.
 *
 * WHY THIS EXISTS - this was a real bug, not ceremony:
 * A BroadcastReceiver's process has no foreground component once onReceive
 * returns, so Android is free to kill it. Our process is holding a 529 MB
 * model in memory, which makes it the first thing the system reclaims.
 * Measured: the 55-message run was killed mid-way, twice, and the block
 * screen would have silently never appeared for a real SMS.
 *
 * A foreground service tells Android "this work is user-visible, do not kill
 * it". That is exactly true here - the user is about to be shown a warning
 * that stops them losing money.
 */
class DetectionService : Service() {

    companion object {
        const val ACTION_CHECK = "com.stackunderflow.airgap.CHECK"
        const val ACTION_RUN_TESTS = "com.stackunderflow.airgap.RUN_TESTS_SVC"
        const val EXTRA_SENDER = "sender"
        const val EXTRA_BODY = "body"

        private const val CHANNEL = "airgap_working"
        private const val NOTIF_ID = 4201
        private const val TAG = "AIRGAP_TEST"

        fun check(context: Context, sender: String, body: String) {
            val i = Intent(context, DetectionService::class.java).apply {
                action = ACTION_CHECK
                putExtra(EXTRA_SENDER, sender)
                putExtra(EXTRA_BODY, body)
            }
            context.startForegroundService(i)
        }

        fun runTests(context: Context) {
            val i = Intent(context, DetectionService::class.java).apply { action = ACTION_RUN_TESTS }
            context.startForegroundService(i)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundCompat()

        when (intent?.action) {
            ACTION_CHECK -> {
                val sender = intent.getStringExtra(EXTRA_SENDER) ?: "unknown"
                val body = intent.getStringExtra(EXTRA_BODY) ?: ""
                Airgap.handleMessageAsync(
                    this, sender, body,
                    onResult = { v -> Log.i("Airgap", "checked scam=${v.isScam} pattern=${v.pattern}") },
                    onFinally = { stopSelf(startId) }
                )
            }

            ACTION_RUN_TESTS -> Thread {
                try {
                    Airgap.initDetector(applicationContext)
                    val d = Airgap.detector
                    Log.i(TAG, "START engine=${d.engineName}")
                    val t0 = System.currentTimeMillis()
                    val r = TestSetRunner.run(applicationContext, d)
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
                } finally {
                    stopSelf(startId)
                }
            }.start()

            else -> stopSelf(startId)
        }

        return START_NOT_STICKY
    }

    private fun startForegroundCompat() {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Airgap checking", NotificationManager.IMPORTANCE_LOW)
                    .apply { description = "Shown while Airgap checks a message on this phone." }
            )
        }
        val n: Notification = Notification.Builder(this, CHANNEL)
            .setContentTitle("Airgap")
            .setContentText("Checking a message on this phone")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }
}

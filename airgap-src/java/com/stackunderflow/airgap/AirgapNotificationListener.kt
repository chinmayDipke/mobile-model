package com.stackunderflow.airgap

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * Catches UPI collect requests, which arrive as a notification from the
 * payment app rather than as an SMS.
 */
class AirgapNotificationListener : NotificationListenerService() {

    /** Only payment and messaging apps, not every notification on the phone. */
    private val watched = setOf(
        "com.google.android.apps.nbu.paisa.user", // Google Pay
        "net.one97.paytm",
        "com.phonepe.app",
        "in.org.npci.upiapp",                     // BHIM
        "com.google.android.apps.messaging",
        "com.android.messaging"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in watched) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return

        val body = (title + " " + text).trim()
        Log.i("Airgap", "notification from " + sbn.packageName)
        Airgap.handleMessage(applicationContext, sbn.packageName, body)
    }
}

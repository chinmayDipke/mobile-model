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
        "com.android.messaging",
        // Carriers filter phishing SMS sent from personal numbers, so a lot of
        // real UPI fraud now arrives over WhatsApp instead - and it is the only
        // path we can demo when the operator blocks our own test SMS.
        "com.whatsapp",
        "com.whatsapp.w4b",
        "org.telegram.messenger",
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in watched) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return

        val body = (title + " " + text).trim()
        Log.i("Airgap", "notification from " + sbn.packageName)
        // MUST be the async form. onNotificationPosted is on this service's main
        // thread; the blocking version would freeze it for ~400 ms with Gemma,
        // and Android kills a notification listener that stops responding.
        Airgap.handleMessageAsync(applicationContext, sbn.packageName, body)
    }
}

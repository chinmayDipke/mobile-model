package com.stackunderflow.airgap

import android.app.Notification
import androidx.core.app.NotificationCompat
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

    /**
     * Pull the message text out of a notification, whichever field the app used.
     *
     * WHY THIS IS NOT JUST EXTRA_TEXT:
     * WhatsApp builds a different notification depending on the chat. From a
     * saved contact with one unread message you get the body in EXTRA_TEXT.
     * From an unknown number, or with several unread, it switches to
     * MessagingStyle or InboxStyle and EXTRA_TEXT becomes "2 new messages" -
     * the scam text is in EXTRA_MESSAGES or EXTRA_TEXT_LINES instead.
     *
     * Found the hard way: the same scam message alerted from one sender and did
     * nothing from another. In the demo the sender is not the phone's owner, so
     * this is exactly the path a judge would see fail.
     */
    private fun extractText(n: Notification): String {
        val e = n.extras
        val parts = mutableListOf<String>()

        fun add(cs: CharSequence?) {
            val s = cs?.toString()?.trim().orEmpty()
            if (s.isNotBlank() && parts.none { it == s }) parts.add(s)
        }

        add(e.getCharSequence(Notification.EXTRA_TITLE))
        add(e.getCharSequence(Notification.EXTRA_TEXT))
        add(e.getCharSequence(Notification.EXTRA_BIG_TEXT))
        add(e.getCharSequence(Notification.EXTRA_SUB_TEXT))
        add(e.getCharSequence(Notification.EXTRA_SUMMARY_TEXT))

        // InboxStyle - one line per unread message
        e.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.forEach { add(it) }

        // MessagingStyle - what WhatsApp actually uses for chats
        try {
            val style = NotificationCompat.MessagingStyle
                .extractMessagingStyleFromNotification(n)
            style?.messages?.forEach { add(it.text) }
        } catch (_: Throwable) {
            // older/unusual notification shapes - the fields above still cover us
        }

        return parts.joinToString(" ").trim()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in watched) return

        // A group SUMMARY carries no message text, only "3 new messages".
        // The real one arrives as its own notification right after it.
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val body = extractText(sbn.notification)
        if (body.isBlank()) return

        Log.i("Airgap", "notification from " + sbn.packageName + " len=" + body.length)
        // MUST be the async form. onNotificationPosted is on this service's main
        // thread; the blocking version would freeze it for ~400 ms with Gemma,
        // and Android kills a notification listener that stops responding.
        Airgap.handleMessageAsync(
            applicationContext, sbn.packageName, body,
            // Logged so we can tell a CLEAN verdict apart from a de-duplicated
            // repeat during rehearsal - both look identical without this.
            onResult = { v -> Log.i("Airgap", "verdict scam=" + v.isScam + " pattern=" + v.pattern) }
        )
    }
}

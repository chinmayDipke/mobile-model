package com.stackunderflow.airgap

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

/** Catches an SMS the moment it lands. */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val sender = messages[0].displayOriginatingAddress ?: "unknown"
        val body = messages.joinToString("") { it.displayMessageBody ?: "" }

        // Hand off to a foreground service. onReceive is on the main thread and
        // its process can be killed as soon as we return - with a 529MB model
        // loaded, ours is first in line. goAsync() alone is not enough.
        Log.i("Airgap", "sms from=" + sender)
        DetectionService.check(context.applicationContext, sender, body)
    }
}

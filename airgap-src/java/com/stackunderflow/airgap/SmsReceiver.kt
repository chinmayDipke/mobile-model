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

        val verdict = Airgap.handleMessage(context, sender, body)
        Log.i("Airgap", "sms from=" + sender + " scam=" + verdict.isScam + " pattern=" + verdict.pattern)
    }
}

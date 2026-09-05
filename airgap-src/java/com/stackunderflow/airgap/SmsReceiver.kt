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

        // onReceive runs on the MAIN thread and Android kills a receiver that
        // blocks for ~10 s. Gemma takes ~370 ms, and the very first call also
        // loads the model. goAsync() keeps the receiver alive while we work
        // off-thread - without it, a real incoming SMS would ANR the app.
        val pending = goAsync()
        Airgap.handleMessageAsync(
            context, sender, body,
            onResult = { v ->
                Log.i("Airgap", "sms from=" + sender + " scam=" + v.isScam + " pattern=" + v.pattern)
            },
            onFinally = { pending.finish() }
        )
    }
}

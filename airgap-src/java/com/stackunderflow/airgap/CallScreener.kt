package com.stackunderflow.airgap

import android.content.Intent
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log

/**
 * Warns when a number we have already seen inside a blocked scam rings you.
 *
 * A fake-customer-care SMS gives a number. We stop the message. Then the person
 * calls back, or the scammer calls them, and the money is lost on the call -
 * about a number we already knew was bad.
 *
 * WE NEVER BLOCK OR SILENCE A CALL. Two reasons, and the first is enough:
 * getting this wrong means someone misses a call from a hospital. The second is
 * that a warning teaches, and silently swallowing a call does not.
 *
 * SAFETY: respondToCall is called FIRST and unconditionally, before we look at
 * anything. A call screening service that fails to respond leaves the phone
 * ringing into nothing - so nothing we do can ever reach the point of not
 * responding. Everything after it is wrapped and can only fail quietly.
 *
 * Only runs at all if the user granted the call-screening role. Without it
 * Android never calls this class and the rest of the app is unchanged.
 */
class CallScreener : CallScreeningService() {

    override fun onScreenCall(details: Call.Details) {
        // Always let the call through, first thing, no matter what follows.
        try {
            respondToCall(details, CallResponse.Builder().build())
        } catch (t: Throwable) {
            Log.e("Airgap", "respondToCall failed", t)
            return
        }

        try {
            if (details.callDirection != Call.Details.DIRECTION_INCOMING) {
                Log.i("Airgap", "call screened: not incoming, ignoring")
                return
            }
            val number = details.handle?.schemeSpecificPart
            val known = ScamNumbers.isKnown(this, number)
            // Log every screened call. Without this "the service never ran" and
            // "it ran and did not know the number" look identical, which cost
            // us a test cycle.
            Log.i("Airgap", "call screened: known=" + known +
                    " stored=" + ScamNumbers.count(this) +
                    " last4=" + (number?.takeLast(4) ?: "?"))
            if (number == null || !known) return

            startActivity(Intent(this, BlockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(BlockActivity.EXTRA_PATTERN, "scam_caller")
                putExtra(
                    BlockActivity.EXTRA_REASON,
                    "This number sent you a scam message earlier. Whoever answers will " +
                    "ask you for an OTP or tell you to approve a payment. Your bank " +
                    "will never call from a personal number."
                )
                putExtra(BlockActivity.EXTRA_MESSAGE, "Incoming call from $number")
                putExtra(BlockActivity.EXTRA_CONFIDENCE, 1.0f)
                putExtra(BlockActivity.EXTRA_ENGINE, Airgap.detector.engineName)
            })
        } catch (t: Throwable) {
            // The call is already allowed through. A failure here costs a
            // warning, never the call.
            Log.e("Airgap", "call screen check failed", t)
        }
    }
}

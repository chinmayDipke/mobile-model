package com.stackunderflow.airgap

import android.net.Uri

/**
 * Reads a scanned QR code and decides what to tell the user.
 *
 * THE WHOLE IDEA, and it is worth saying out loud in the pitch:
 *
 *   A UPI QR code can only ever START A PAYMENT FROM YOU.
 *   There is no such thing as a QR that pays money INTO your account.
 *
 * So "scan this QR to receive your cashback / refund / prize" is a scam by
 * construction - not by keyword, not by probability. It cannot be anything
 * else. That is why this check needs no model and is instant, and why we can
 * state it to the user as a fact rather than a suspicion.
 *
 * We do NOT call every UPI QR a scam - paying a shopkeeper is normal. We say
 * plainly which direction the money moves, and let the user notice that it is
 * the opposite of what they were promised.
 */
object UpiQr {

    data class Scan(
        val isUpiPayment: Boolean,
        val payeeVpa: String?,
        val payeeName: String?,
        val amount: String?,
        val url: String?,
        val raw: String,
    )

    fun parse(raw: String): Scan {
        val trimmed = raw.trim()

        // upi://pay?pa=someone@bank&pn=Name&am=500&cu=INR
        if (trimmed.startsWith("upi:", ignoreCase = true)) {
            val u = runCatching { Uri.parse(trimmed) }.getOrNull()
            return Scan(
                isUpiPayment = true,
                payeeVpa = u?.getQueryParameter("pa"),
                payeeName = u?.getQueryParameter("pn"),
                amount = u?.getQueryParameter("am"),
                url = null,
                raw = trimmed,
            )
        }

        val isUrl = trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)
        return Scan(false, null, null, null, if (isUrl) trimmed else null, trimmed)
    }

    /** null means nothing worth interrupting the user for. */
    fun judge(s: Scan): Verdict? {
        if (s.isUpiPayment) {
            val who = s.payeeName?.takeIf { it.isNotBlank() }
                ?: s.payeeVpa?.takeIf { it.isNotBlank() }
                ?: "someone"
            val howMuch = s.amount?.takeIf { it.isNotBlank() }?.let { "Rs.$it" }

            val reason = buildString {
                append("This QR code will SEND ")
                append(howMuch ?: "money")
                append(" from your account to ").append(who).append(". ")
                append("A QR code can never put money INTO your account. ")
                append("If someone told you to scan this to get a refund or cashback, it is a scam.")
            }
            return Verdict(
                isScam = true,
                pattern = "qr_send_only",
                confidence = 1.0f,   // structural, not a guess
                reason = reason,
            )
        }

        // A QR that just opens a link. Hand the URL to the normal detector -
        // a fake KYC page reached by camera is the same scam as one reached by SMS.
        s.url?.let { url ->
            val v = Airgap.detector.check(Normaliser.normalise("QR code", url))
            if (v.isScam) return v
            return null
        }

        return null
    }
}

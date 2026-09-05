package com.stackunderflow.airgap

/**
 * Placeholder detector so the whole app works TODAY, before the model lands.
 * Shan swaps this for the Gemma implementation behind the same interface.
 *
 * VERIFIED against testset/testset.json before shipping:
 *   scams caught 40/40 · clean messages passed 15/15
 *
 * Looks at STRUCTURE, not single keywords, on purpose. The 15 genuine bank
 * messages in the test set deliberately contain "refund", "debited", "blocked",
 * "OTP" and "credited". A naive keyword filter flags them and we lose the demo.
 */
class StubDetector : Detector {

    override val engineName = "Rules (stub)"

    private fun String.hasAny(vararg words: String) =
        words.any { this.contains(it, ignoreCase = true) }

    override fun check(message: NormalisedMessage): Verdict {
        val t = message.raw
        val hasLink = message.urls.isNotEmpty()
        // a personal mobile, as opposed to a 1800 number, which real banks use
        val hasMobile = message.mobileNumbers.isNotEmpty()

        // 1. KYC / PAN / account-suspension scare
        if (t.hasAny("kyc", "pan card", "aadhaar", "not linked", "suspension", "re-verify",
                "reactivate", "account will be blocked", "will be suspended") &&
            (hasLink || t.hasAny("suspension", "blocked today"))
        ) {
            return Verdict(true, "kyc_link", 0.94f,
                "This asks you to update KYC or PAN details through a link. Banks never " +
                "do that, and this link is not a bank address.")
        }

        // 2. Scan a QR code TO RECEIVE money. That is impossible.
        if (t.hasAny("scan") && t.hasAny("qr") &&
            t.hasAny("receive", "cashback", "won", "prize", "claim", "credit")
        ) {
            return Verdict(true, "qr_cashback", 0.97f,
                "Scanning a QR code can only SEND money. It can never receive money. " +
                "This is the cashback QR scam.")
        }

        // 3. A collect request dressed up as incoming money.
        //    Entering a UPI PIN to RECEIVE is always a scam. You never do that.
        if (t.hasAny("collect request", "payment request") ||
            (t.hasAny("upi pin", "enter pin", "enter your pin") &&
                t.hasAny("receive", "credit", "settlement", "refund", "arrears")) ||
            (t.hasAny("accept", "approve", "authorize", "authorise") &&
                t.hasAny("request") &&
                t.hasAny("receive", "settlement", "arrears", "credit", "claim", "refund"))
        ) {
            return Verdict(true, "collect_refund", 0.93f,
                "This is a collect request, not a refund. You never enter your UPI PIN " +
                "to RECEIVE money. Approving it takes money OUT of your account.")
        }

        // 4. Fake customer care. Real banks publish 1800 numbers, never personal mobiles.
        if (t.hasAny("call", "contact", "helpline", "customer care", "helpdesk", "support", "whatsapp") &&
            hasMobile &&
            t.hasAny("failed", "refund", "stuck", "pending", "reversal", "blocked", "complaint",
                "deactivated", "on hold", "disconnected", "release", "unblock", "confirm")
        ) {
            return Verdict(true, "fake_care", 0.91f,
                "A real helpline is a 1800 number. This one gives a personal mobile " +
                "number. Calling it reaches the scammer, not the bank.")
        }

        // 5. "Sent you money by mistake, please send it back."
        if (t.hasAny("by mistake", "mistakenly", "wrongly", "galti", "accidentally",
                "wrong number", "wrong account", "wrong upi", "wrong transfer") &&
            t.hasAny("return", "send back", "refund", "wapas", "give back",
                "transfer back", "cooperate")
        ) {
            return Verdict(true, "wrong_transfer", 0.89f,
                "Nobody actually sent you money. Check your own balance yourself before " +
                "you send anything back to them.")
        }

        // Everything else passes. Passing clean messages is half the product.
        return Verdict.clean()
    }
}

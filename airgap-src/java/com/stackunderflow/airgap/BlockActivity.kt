package com.stackunderflow.airgap

import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

/**
 * The full screen interrupt. This is the whole point of the product:
 * a notification gets swiped away, this does not.
 *
 * Reads the reason ALOUD, which is the voice half of the creative-phone-use
 * score and is genuinely better for first-time and older users.
 */
class BlockActivity : AppCompatActivity() {

    private var tts: TextToSpeech? = null

    companion object {
        const val EXTRA_PATTERN = "pattern"
        const val EXTRA_REASON = "reason"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_CONFIDENCE = "confidence"
        const val EXTRA_ENGINE = "engine"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_block)

        val pattern = intent.getStringExtra(EXTRA_PATTERN) ?: "unknown"
        val reason = intent.getStringExtra(EXTRA_REASON) ?: ""
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: ""
        val confidence = intent.getFloatExtra(EXTRA_CONFIDENCE, 0f)
        val engine = intent.getStringExtra(EXTRA_ENGINE) ?: ""

        findViewById<TextView>(R.id.patternLabel).text = prettyPattern(pattern)
        findViewById<TextView>(R.id.reasonText).text = reason
        findViewById<TextView>(R.id.messageText).text = "\u201C" + message + "\u201D"
        findViewById<TextView>(R.id.metaText).text =
            String.format(Locale.UK, "%s  \u00B7  %.0f%% confident  \u00B7  decided on this phone",
                engine, confidence * 100)

        findViewById<Button>(R.id.closeButton).setOnClickListener { finish() }
        findViewById<Button>(R.id.ignoreButton).setOnClickListener { finish() }

        speak(reason)
    }

    private fun prettyPattern(p: String) = when (p) {
        "kyc_link" -> "FAKE KYC LINK"
        "qr_cashback" -> "CASHBACK QR SCAM"
        "collect_refund" -> "REFUND THAT TAKES MONEY"
        "fake_care" -> "FAKE CUSTOMER CARE"
        "wrong_transfer" -> "WRONG TRANSFER TRICK"
        else -> "SCAM BLOCKED"
    }

    private fun speak(text: String) {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.forLanguageTag("en-IN")
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "airgap-warning")
            }
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

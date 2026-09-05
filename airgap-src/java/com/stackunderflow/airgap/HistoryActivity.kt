package com.stackunderflow.airgap

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * "What has Airgap caught?"
 *
 * Built in code rather than XML because every row is the same shape and a
 * RecyclerView adapter for a fifty-item list is more machinery than the job
 * needs.
 */
class HistoryActivity : AppCompatActivity() {

    private val time = SimpleDateFormat("d MMM, HH:mm", Locale.UK)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val entries = History.all(this)

        findViewById<TextView>(R.id.historyCount).text =
            if (entries.isEmpty()) "Nothing blocked yet"
            else "${entries.size} " + if (entries.size == 1) "scam blocked" else "scams blocked"

        findViewById<TextView>(R.id.historySub).text =
            if (entries.isEmpty())
                "When Airgap stops a scam it will be listed here. Nothing is listed until then, which is also the point."
            else
                "On this phone. This list has never left it."

        val list = findViewById<LinearLayout>(R.id.historyList)
        list.removeAllViews()
        entries.forEach { list.addView(row(it)) }

        findViewById<Button>(R.id.historyClear).setOnClickListener {
            History.clear(this)
            render()
        }
    }

    private fun row(e: History.Entry): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
        }

        card.addView(line(prettyPattern(e.pattern), 15f, "#F0B31C", bold = true))
        card.addView(line(e.reason, 14f, "#D6D8DE", topMargin = dp(6)))
        card.addView(line("“" + e.message + "”", 13f, "#7E818A", topMargin = dp(10)))
        card.addView(
            line(
                time.format(Date(e.at)) + "  ·  " + prettySource(e.source) + "  ·  " + e.engine,
                12f, "#5F626B", topMargin = dp(10)
            )
        )
        return card
    }

    private fun line(
        text: String, size: Float, colour: String,
        bold: Boolean = false, topMargin: Int = 0,
    ) = TextView(this).apply {
        this.text = text
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        setTextColor(Color.parseColor(colour))
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setLineSpacing(0f, 1.3f)
        gravity = Gravity.START
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { this.topMargin = topMargin }
    }

    private fun prettyPattern(p: String) = when (p) {
        "kyc_link" -> "FAKE KYC LINK"
        "qr_cashback" -> "CASHBACK QR SCAM"
        "collect_refund" -> "REFUND THAT TAKES MONEY"
        "fake_care" -> "FAKE CUSTOMER CARE"
        "wrong_transfer" -> "WRONG TRANSFER TRICK"
        "qr_send_only" -> "QR THAT ONLY SENDS MONEY"
        else -> "SCAM BLOCKED"
    }

    private fun prettySource(s: String) = when {
        s.isBlank() -> "message"
        s.startsWith("com.whatsapp") -> "WhatsApp"
        s.startsWith("org.telegram") -> "Telegram"
        s.contains("messaging") -> "SMS"
        s == "shared" -> "shared to Airgap"
        s == "qr" -> "camera"
        else -> s
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}

package com.stackunderflow.airgap

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * "Share to Airgap" - check a message from ANY app.
 *
 * WHY THIS MATTERS more than it looks:
 * The notification listener only sees apps we named, and Android will never
 * let us see them all. Someone forwards a scam over Telegram, or it arrives in
 * email, or in a game's chat - we are blind to it.
 *
 * Sharing is the one route the platform gives every app for free. Long-press
 * any text anywhere, Share, Airgap. Same detector, same warning, no new
 * permission asked of the user.
 *
 * It is also the honest answer to "does this only work for WhatsApp?" - no,
 * that is just the fast path.
 */
class CheckActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check)
        check()
    }

    /**
     * Share something while this screen is already open and Android reuses the
     * instance instead of building a new one - onCreate never runs and the
     * second message is silently never checked. Found while testing two shares
     * back to back.
     */
    override fun onNewIntent(newIntent: Intent) {
        super.onNewIntent(newIntent)
        setIntent(newIntent)
        check()
    }

    private fun check() {
        findViewById<TextView>(R.id.checkTitle).text = "Checking"
        findViewById<TextView>(R.id.checkQuote).text = ""
        findViewById<TextView>(R.id.checkEngine).text = ""

        val shared = readSharedText()
        if (shared.isNullOrBlank()) {
            finishWith("Nothing to check.")
            return
        }

        findViewById<TextView>(R.id.checkStatus).text = "Checking on this phone..."

        Airgap.handleMessageAsync(
            this, "shared", shared,
            deduplicate = false,   // the user asked; always answer
            onResult = { verdict ->
                if (verdict.isScam) {
                    // handleMessageAsync already raised the block screen.
                    finish()
                } else {
                    showClean(shared)
                }
            }
        )
    }

    private fun readSharedText(): String? = when (intent?.action) {
        Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
        Intent.ACTION_PROCESS_TEXT ->
            intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
        else -> null
    }

    /**
     * The user asked a direct question, so silence is the wrong answer here -
     * unlike the passive path, where saying nothing IS the answer.
     */
    private fun showClean(text: String) {
        findViewById<TextView>(R.id.checkTitle).text = "Nothing suspicious"
        findViewById<TextView>(R.id.checkStatus).text =
            "This message does not ask you to send money, enter a PIN, or open a link " +
            "pretending to be your bank.\n\nChecked on this phone. Nothing was uploaded."
        findViewById<TextView>(R.id.checkQuote).text = "“" + text.take(200) + "”"
        findViewById<TextView>(R.id.checkEngine).text = Airgap.detector.engineName
    }

    private fun finishWith(msg: String) {
        findViewById<TextView>(R.id.checkStatus).text = msg
    }
}

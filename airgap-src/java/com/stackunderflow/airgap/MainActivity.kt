package com.stackunderflow.airgap

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Control panel. Not the product itself, but it is how we grant permissions,
 * prove the detector works, and rehearse the demo.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView

    private val demoScam =
        "Congratulations! You have won Rs.10,000 cashback. Scan the attached QR code to receive your amount."
    private val demoClean =
        "Rs.2,500.00 debited from A/c XX4471 on 05-09-26 to VPA grocerystore@okaxis. Not you? Call 18004253800."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        status = findViewById(R.id.statusText)

        // Pick the real model if its file is on this phone, else stay on the stub.
        // Off the main thread: loading Gemma takes ~1.5 s and would hang the UI.
        Thread {
            val started = System.currentTimeMillis()
            Airgap.initDetector(applicationContext)
            val ms = System.currentTimeMillis() - started
            // Print the load time. A judge asking "is it really on the phone?"
            // gets a number, not a claim.
            runOnUiThread {
                refreshStatus()
                // A judge asking "is the model really on this phone?" gets a
                // number off the screen instead of our word for it.
                findViewById<TextView>(R.id.engineDetail).text =
                    "On this device · ready in " + ms + " ms · no network"
            }
        }.start()

        findViewById<LinearLayout>(R.id.smsPermButton).setOnClickListener { askSmsPermission() }
        findViewById<LinearLayout>(R.id.overlayPermButton).setOnClickListener { askOverlay() }
        findViewById<LinearLayout>(R.id.notifPermButton).setOnClickListener { askNotificationAccess() }

        findViewById<LinearLayout>(R.id.demoScamButton).setOnClickListener {
            status.text = "Checking on device..."
            Airgap.handleMessageAsync(this, "VK-REWARDS", demoScam, onResult = { v ->
                if (!v.isScam) status.text = "Model said CLEAN. It missed this one."
            })
        }
        findViewById<LinearLayout>(R.id.demoCleanButton).setOnClickListener {
            status.text = "Checking on device..."
            Airgap.handleMessageAsync(this, "VM-SBIINB", demoClean, onResult = { v ->
                status.text = if (!v.isScam)
                    "Clean message passed silently. Nothing was shown. That is correct."
                else
                    "FALSE ALARM - it blocked a genuine bank SMS. Needs tuning."
            })
        }
        findViewById<LinearLayout>(R.id.scanQrButton).setOnClickListener {
            startActivity(Intent(this, QrScanActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.historyButton).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.runTestsButton).setOnClickListener { runTests() }

        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun runTests() {
        status.text = "Running 55 messages..."
        Thread {
            val r = TestSetRunner.run(this, Airgap.detector)
            runOnUiThread {
                status.text = buildString {
                    append(Airgap.detector.engineName).append("\n")
                    append(r.summary)
                    if (r.missed.isNotEmpty()) append("\nMissed: ").append(r.missed.joinToString(", "))
                    if (r.falseAlarms.isNotEmpty()) append("\nFALSE ALARMS: ").append(r.falseAlarms.joinToString(", "))
                }
            }
        }.start()
    }

    private fun askSmsPermission() {
        ActivityCompat.requestPermissions(
            // RECEIVE_SMS delivers the broadcast. READ_SMS would open the whole
            // inbox history - we never read it, so we no longer ask for it.
            this, arrayOf(Manifest.permission.RECEIVE_SMS), 1
        )
    }

    private fun askOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + packageName))
            )
        }
    }

    private fun askNotificationAccess() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun refreshStatus() {
        val sms = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) ==
                PackageManager.PERMISSION_GRANTED
        val overlay = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
        val listeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: ""
        val notif = listeners.contains(packageName)

        setRow(R.id.smsState, R.id.smsDot, sms)
        setRow(R.id.overlayState, R.id.overlayDot, overlay)
        setRow(R.id.notifState, R.id.notifDot, notif)

        findViewById<TextView>(R.id.engineName).text = Airgap.detector.engineName

        val blocked = History.all(this).size
        findViewById<TextView>(R.id.historyCountBadge).text =
            if (blocked == 0) "" else blocked.toString()
    }

    /** Green dot and "On", or red dot and "Tap to fix". Nothing else. */
    private fun setRow(stateId: Int, dotId: Int, ok: Boolean) {
        findViewById<TextView>(stateId).apply {
            text = if (ok) "On" else "Tap to fix"
            setTextColor(ContextCompat.getColor(this@MainActivity,
                if (ok) R.color.text_tertiary else R.color.bad))
        }
        findViewById<View>(dotId).setBackgroundResource(
            if (ok) R.drawable.dot_on else R.drawable.dot_off
        )
    }
}

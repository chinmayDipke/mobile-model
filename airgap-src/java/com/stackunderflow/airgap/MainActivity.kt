package com.stackunderflow.airgap

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
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

        findViewById<Button>(R.id.smsPermButton).setOnClickListener { askSmsPermission() }
        findViewById<Button>(R.id.overlayPermButton).setOnClickListener { askOverlay() }
        findViewById<Button>(R.id.notifPermButton).setOnClickListener { askNotificationAccess() }

        findViewById<Button>(R.id.demoScamButton).setOnClickListener {
            Airgap.handleMessage(this, "VK-REWARDS", demoScam)
        }
        findViewById<Button>(R.id.demoCleanButton).setOnClickListener {
            val v = Airgap.handleMessage(this, "VM-SBIINB", demoClean)
            if (!v.isScam) {
                status.text = "Clean message passed silently. Nothing was shown. That is correct."
            }
        }
        findViewById<Button>(R.id.runTestsButton).setOnClickListener { runTests() }

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
            this, arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS), 1
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

        status.text = buildString {
            append("Engine: ").append(Airgap.detector.engineName).append("\n")
            append(if (sms) "OK      " else "MISSING ").append("SMS permission\n")
            append(if (overlay) "OK      " else "MISSING ").append("Draw over other apps\n")
            append(if (notif) "OK      " else "MISSING ").append("Notification access")
        }
    }
}

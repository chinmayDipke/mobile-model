package com.stackunderflow.airgap

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Point the camera at a QR code. Everything runs on the phone - ML Kit's
 * barcode model is bundled in the APK, so this works in aeroplane mode too.
 */
class QrScanActivity : AppCompatActivity() {

    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private val scanner = BarcodeScanning.getClient()

    /** One warning per visit. Without it the same code fires every frame. */
    private val handled = AtomicBoolean(false)

    private lateinit var hint: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scan)
        hint = findViewById(R.id.qrHint)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 7)
        } else {
            startCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 7 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            hint.text = "Camera permission is needed to check a QR code."
        }
    }

    override fun onResume() {
        super.onResume()
        // Coming back from the block screen - let the user scan another one.
        handled.set(false)
        hint.text = "Point the camera at a QR code"
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = future.get()
            val previewView = findViewById<PreviewView>(R.id.previewView)

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(analysisExecutor, ::analyse) }

            try {
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            } catch (t: Throwable) {
                Log.e("Airgap", "camera bind failed", t)
                hint.text = "Could not start the camera."
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun analyse(proxy: androidx.camera.core.ImageProxy) {
        val media = proxy.image
        if (media == null || handled.get()) { proxy.close(); return }

        val image = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { codes -> codes.firstOrNull()?.let(::onCode) }
            .addOnCompleteListener { proxy.close() }
    }

    private fun onCode(code: Barcode) {
        val raw = code.rawValue ?: return
        if (!handled.compareAndSet(false, true)) return   // first one wins

        val scan = UpiQr.parse(raw)
        Log.i("Airgap", "qr scanned upi=${scan.isUpiPayment} len=${raw.length}")

        val verdict = UpiQr.judge(scan)
        if (verdict == null) {
            runOnUiThread { hint.text = "This QR looks harmless. Nothing to warn about." }
            handled.set(false)
            return
        }

        History.record(this, verdict, raw.take(160), "qr")

        startActivity(Intent(this, BlockActivity::class.java).apply {
            putExtra(BlockActivity.EXTRA_PATTERN, verdict.pattern)
            putExtra(BlockActivity.EXTRA_REASON, verdict.reason)
            putExtra(BlockActivity.EXTRA_MESSAGE, raw.take(120))
            putExtra(BlockActivity.EXTRA_CONFIDENCE, verdict.confidence)
            putExtra(BlockActivity.EXTRA_ENGINE, Airgap.detector.engineName)
        })
    }

    override fun onDestroy() {
        analysisExecutor.shutdown()
        scanner.close()
        super.onDestroy()
    }
}

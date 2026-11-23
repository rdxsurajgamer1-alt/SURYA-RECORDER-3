package com.suryarecorder

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast

class CaptureActivity : Activity() {
    companion object {
        private const val REQUEST_CAPTURE = 901
        private const val REQUEST_MIC = 902
    }

    private var captureRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestMicrophoneIfNeeded()
    }

    private fun requestMicrophoneIfNeeded() {
        val audioMode = intent.getStringExtra("audio") ?: "MIC"
        val needsMic = audioMode == "MIC" || audioMode == "BOTH"

        if (needsMic && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_MIC)
        } else {
            launchScreenCapture()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_MIC) {
            // Continue even if microphone permission is denied: screen video can still be recorded
            // without audio. The service will use the selected mode only when permission is granted.
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchScreenCapture()
            } else {
                Toast.makeText(this, "Microphone अनुमति नहीं मिली; वीडियो बिना आवाज़ रिकॉर्ड होगा।", Toast.LENGTH_LONG).show()
                intent.putExtra("audio", "NONE")
                launchScreenCapture()
            }
        }
    }

    private fun launchScreenCapture() {
        if (captureRequested) return
        captureRequested = true
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        @Suppress("DEPRECATION")
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CAPTURE)
    }

    @Deprecated("Uses the platform screen-capture consent activity")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CAPTURE) {
            if (resultCode == RESULT_OK && data != null) {
                val serviceIntent = Intent(this, RecordingService::class.java).apply {
                    action = RecordingService.ACTION_START
                    putExtra(RecordingService.EXTRA_RESULT_CODE, resultCode)
                    putExtra(RecordingService.EXTRA_DATA, data)
                    for (key in listOf("ratio", "res", "fps", "audio", "cw", "ch")) {
                        putExtra(key, intent.getStringExtra(key))
                    }
                }
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Recording Service शुरू नहीं हुई: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Screen recording की अनुमति नहीं दी गई।", Toast.LENGTH_LONG).show()
            }
            finish()
        }
    }
}

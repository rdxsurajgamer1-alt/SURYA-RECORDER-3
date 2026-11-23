package com.suryarecorder

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import java.io.File

class RecordingService : Service() {
    companion object {
        const val ACTION_START = "com.suryarecorder.START"
        const val ACTION_STOP = "com.suryarecorder.STOP"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "projection_data"
        const val CHANNEL = "surya_recording"
        private const val TAG = "SURYA_RECORDER"
    }

    private var recorder: MediaRecorder? = null
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var projectionCallback: MediaProjection.Callback? = null
    private var recordingStarted = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopRecording()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                createNotificationChannel()
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            41,
                            buildNotification(),
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                        )
                    } else {
                        startForeground(41, buildNotification())
                    }
                    startRecording(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Unable to start recording service", e)
                    showError(e)
                    stopRecording()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startRecording(intent: Intent) {
        try {
            val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            @Suppress("DEPRECATION")
            val permissionData: Intent? = if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
            } else {
                intent.getParcelableExtra(EXTRA_DATA)
            }
            val data = permissionData ?: throw IllegalStateException("Screen capture permission data missing")
            if (resultCode != Activity.RESULT_OK) {
                throw IllegalStateException("Screen capture permission was not granted")
            }

            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = manager.getMediaProjection(resultCode, data)
                ?: throw IllegalStateException("MediaProjection could not be created")

            projectionCallback = object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.i(TAG, "MediaProjection stopped by system/user")
                    stopRecording()
                }
            }
            projection!!.registerCallback(projectionCallback!!, Handler(Looper.getMainLooper()))

            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)

            val requestedHeight = (intent.getStringExtra("res")?.filter { it.isDigit() }?.toIntOrNull() ?: 1080)
                .coerceIn(480, 2160)
            val ratio = intent.getStringExtra("ratio") ?: "9:16"
            var ratioW = metrics.widthPixels.toFloat()
            var ratioH = metrics.heightPixels.toFloat()
            when (ratio) {
                "9:16" -> { ratioW = 9f; ratioH = 16f }
                "16:9" -> { ratioW = 16f; ratioH = 9f }
                "1:1" -> { ratioW = 1f; ratioH = 1f }
                "4:3" -> { ratioW = 4f; ratioH = 3f }
                "Custom" -> {
                    ratioW = intent.getStringExtra("cw")?.toFloatOrNull()?.coerceAtLeast(1f) ?: 1080f
                    ratioH = intent.getStringExtra("ch")?.toFloatOrNull()?.coerceAtLeast(1f) ?: 1920f
                }
            }

            val landscape = metrics.widthPixels > metrics.heightPixels
            val rawWidth = if (landscape) requestedHeight else requestedHeight * ratioW / ratioH
            val rawHeight = if (landscape) requestedHeight * ratioH / ratioW else requestedHeight
            fun align16(value: Int): Int = (((value.coerceAtLeast(16) + 15) / 16) * 16)
            val width = align16(rawWidth.toInt())
            val height = align16(rawHeight.toInt())

            val fps = (intent.getStringExtra("fps")?.toIntOrNull() ?: 30).coerceIn(15, 60)
            val audioMode = intent.getStringExtra("audio") ?: "MIC"
            if ((audioMode == "INTERNAL" || audioMode == "BOTH")) {
                Toast.makeText(this, "Internal Audio अभी इस build में लागू नहीं है; वीडियो बिना Internal Audio रिकॉर्ड होगा।", Toast.LENGTH_LONG).show()
            }

            val outputDir = File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "SURYA_RECORDER")
            if (!outputDir.exists() && !outputDir.mkdirs()) {
                throw IllegalStateException("Recording folder could not be created")
            }
            val outputFile = File(outputDir, "SURYA_${System.currentTimeMillis()}.mp4")

            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder = mediaRecorder

            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            mediaRecorder.setVideoSize(width, height)
            mediaRecorder.setVideoFrameRate(fps)
            val bitRate = when {
                requestedHeight >= 2160 -> 16_000_000
                requestedHeight >= 1440 -> 10_000_000
                requestedHeight >= 1080 -> 6_000_000
                else -> 4_000_000
            }
            mediaRecorder.setVideoEncodingBitRate(bitRate)

            // No Audio is a valid video-only configuration. Microphone is added only when granted.
            if (audioMode == "MIC" || audioMode == "BOTH") {
                val micGranted = checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
                if (micGranted) {
                    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                    mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    mediaRecorder.setAudioEncodingBitRate(128_000)
                    mediaRecorder.setAudioSamplingRate(44_100)
                } else {
                    Toast.makeText(this, "Microphone अनुमति नहीं है; वीडियो बिना आवाज़ रिकॉर्ड होगा।", Toast.LENGTH_LONG).show()
                }
            }

            mediaRecorder.setOutputFile(outputFile.absolutePath)
            mediaRecorder.prepare()

            virtualDisplay = projection!!.createVirtualDisplay(
                "SURYA_RECORDER",
                width,
                height,
                metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder.surface,
                null,
                null
            ) ?: throw IllegalStateException("VirtualDisplay could not be created")

            mediaRecorder.start()
            recordingStarted = true
            Toast.makeText(this, "Recording शुरू हो गई", Toast.LENGTH_SHORT).show()
            Log.i(TAG, "Recording started: ${outputFile.absolutePath}, ${width}x${height}, ${fps}fps")
        } catch (e: Exception) {
            Log.e(TAG, "Recording start failed", e)
            throw e
        }
    }

    private fun showError(error: Exception) {
        val message = "${error.javaClass.simpleName}: ${error.message ?: "No detail"}"
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, "Recording शुरू नहीं हुई: $message", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopRecording() {
        try {
            if (recordingStarted) recorder?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Recorder stop failed", e)
        }
        recordingStarted = false
        try { recorder?.reset() } catch (_: Exception) {}
        try { recorder?.release() } catch (_: Exception) {}
        recorder = null

        try { virtualDisplay?.release() } catch (_: Exception) {}
        virtualDisplay = null
        try {
            projectionCallback?.let { projection?.unregisterCallback(it) }
        } catch (_: Exception) {}
        projectionCallback = null
        try { projection?.stop() } catch (_: Exception) {}
        projection = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "SURYA Recording", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, RecordingService::class.java).setAction(ACTION_STOP)
        val pendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL)
            .setContentTitle("SURYA RECORDER")
            .setContentText("Recording चालू है")
            .setSmallIcon(android.R.drawable.presence_video_online)
            .addAction(Notification.Action.Builder(null, "Stop", pendingIntent).build())
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        try { recorder?.release() } catch (_: Exception) {}
        try { virtualDisplay?.release() } catch (_: Exception) {}
        try { projection?.stop() } catch (_: Exception) {}
        super.onDestroy()
    }
}

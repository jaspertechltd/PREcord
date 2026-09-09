package com.example.precord.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.VolumeProvider
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Binder
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.precord.audio.AudioConfig
import com.example.precord.audio.AudioEncoder
import com.example.precord.audio.AudioEnhancer
import com.example.precord.audio.RingBuffer
import com.example.precord.audio.SoundDetector
import com.example.precord.data.CaptureMetadataStore
import com.example.precord.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

data class CapturedFile(
    val filePath: String,
    val fileName: String,
    val timestamp: Long,
    val durationMs: Long,
    val fileSizeBytes: Long
)

class AudioCaptureService : Service() {

    private val binder = LocalBinder()
    private var config = AudioConfig()
    private lateinit var ringBuffer: RingBuffer
    private lateinit var prefs: PreferencesManager
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private val isRecording = AtomicBoolean(false)
    private var audioManager: AudioManager? = null
    private var mediaSession: MediaSession? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var shakeDetector: ShakeDetector? = null

    // Volume key combo tracking for background capture
    private val keySequence = mutableListOf<String>()
    private var lastKeyTime = 0L
    private val comboTimeoutMs = 2000L

    private val _capturedFiles = MutableStateFlow<List<CapturedFile>>(emptyList())
    val capturedFiles: StateFlow<List<CapturedFile>> = _capturedFiles.asStateFlow()

    private val _amplitudes = MutableStateFlow<FloatArray>(FloatArray(0))
    val amplitudes: StateFlow<FloatArray> = _amplitudes.asStateFlow()

    private val _useExternalMic = MutableStateFlow(false)
    val useExternalMic: StateFlow<Boolean> = _useExternalMic.asStateFlow()

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val CHANNEL_ID = "precord_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_CAPTURE = "com.example.precord.ACTION_CAPTURE"
        const val ACTION_UPDATE_CONFIG = "com.example.precord.ACTION_UPDATE_CONFIG"
    }

    inner class LocalBinder : Binder() {
        fun getService(): AudioCaptureService = this@AudioCaptureService
        fun captureBuffer(): String? = this@AudioCaptureService.captureBuffer()
        fun getRingBuffer(): RingBuffer = ringBuffer
        fun isRecording(): Boolean = isRecording.get()
        fun getAmplitudes(numSamples: Int): FloatArray = ringBuffer.getAmplitudes(numSamples)
        val capturedFilesFlow: StateFlow<List<CapturedFile>> get() = capturedFiles
        val amplitudesFlow: StateFlow<FloatArray> get() = amplitudes
        val useExternalMicFlow: StateFlow<Boolean> get() = useExternalMic

        fun setUseExternalMic(use: Boolean) {
            this@AudioCaptureService.setExternalMic(use)
        }

        fun restartWithConfig() {
            this@AudioCaptureService.restartRecording()
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesManager(this)
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        config = AudioConfig(
            sampleRate = prefs.sampleRate,
            channels = prefs.channels,
            bufferDurationSeconds = prefs.bufferDurationSeconds
        )
        ringBuffer = RingBuffer(config.totalBufferBytes)
        _useExternalMic.value = prefs.useExternalMic
        createNotificationChannel()
        setupMediaSession()
        setupShakeDetector()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CAPTURE -> captureBuffer()
            ACTION_UPDATE_CONFIG -> {
                restartRecording()
            }
            else -> {
                startForegroundService()
                startRecording()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
        disableBluetoothSco()
        mediaSession?.release()
        mediaSession = null
        shakeDetector?.stop()
        shakeDetector = null
        serviceScope.cancel()
    }

    private fun setupShakeDetector() {
        if (prefs.isPro && prefs.shakeEnabled) {
            shakeDetector = ShakeDetector(
                context = this,
                sensitivityG = prefs.shakeSensitivity
            ) {
                // Shake detected! Trigger capture
                val result = captureBuffer()
                mainHandler.post {
                    if (result != null) {
                        Toast.makeText(this, "\u2713 Shake captured!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            shakeDetector?.start()
        }
    }

    private fun setupMediaSession() {
        if (prefs.buttonComboEnabled) {
            mediaSession = MediaSession(this, "PrecordMediaSession").apply {
                // Set a minimal playback state so the system considers us an active media app
                val stateBuilder = PlaybackState.Builder()
                    .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE)
                    .setState(PlaybackState.STATE_PLAYING, 0, 1f)
                setPlaybackState(stateBuilder.build())

                // VolumeProvider intercepts volume key presses even with screen off
                setPlaybackToRemote(object : VolumeProvider(
                    VOLUME_CONTROL_RELATIVE, 100, 50
                ) {
                    override fun onAdjustVolume(direction: Int) {
                        if (!prefs.buttonComboEnabled) return
                        val now = System.currentTimeMillis()
                        if (now - lastKeyTime > comboTimeoutMs && keySequence.isNotEmpty()) {
                            keySequence.clear()
                        }
                        val keyName = when (direction) {
                            AudioManager.ADJUST_LOWER -> "DOWN"
                            AudioManager.ADJUST_RAISE -> "UP"
                            else -> return
                        }
                        keySequence.add(keyName)
                        lastKeyTime = now

                        val targetCombo = prefs.buttonComboSequence.split(",").map { it.trim() }
                        if (keySequence.size >= targetCombo.size) {
                            val recentKeys = keySequence.takeLast(targetCombo.size)
                            if (recentKeys == targetCombo) {
                                keySequence.clear()
                                val result = captureBuffer()
                                mainHandler.post {
                                    if (result != null) {
                                        Toast.makeText(this@AudioCaptureService, "✓ Audio captured!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(this@AudioCaptureService, "Buffer empty", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                        if (keySequence.size > 20) keySequence.removeAt(0)
                    }
                })

                isActive = true
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Precord Audio Capture",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Continuously buffering audio"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundService() {
        val captureIntent = Intent(this, AudioCaptureService::class.java).apply {
            action = ACTION_CAPTURE
        }
        val capturePendingIntent = PendingIntent.getService(
            this, 0, captureIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Precord Active")
            .setContentText("Buffering audio • ${formatDuration(prefs.bufferDurationSeconds)} buffer")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .addAction(
                android.R.drawable.ic_menu_save,
                "Capture",
                capturePendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startRecording() {
        if (isRecording.get()) return

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        // Configure audio source based on external mic preference
        val audioSource = if (_useExternalMic.value) {
            enableBluetoothSco()
            MediaRecorder.AudioSource.MIC // DEFAULT will route to external/bluetooth when available
        } else {
            disableBluetoothSco()
            MediaRecorder.AudioSource.MIC
        }

        val channelConfig = if (config.channels == 2) AudioFormat.CHANNEL_IN_STEREO else AudioFormat.CHANNEL_IN_MONO

        val minBufferSize = AudioRecord.getMinBufferSize(
            config.sampleRate,
            channelConfig,
            config.audioFormat
        )
        val bufferSize = if (minBufferSize > 0) minBufferSize * 4 else config.bytesPerSecond / 10

        audioRecord = AudioRecord(
            audioSource,
            config.sampleRate,
            channelConfig,
            config.audioFormat,
            bufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            return
        }

        isRecording.set(true)
        audioRecord?.startRecording()

        recordingThread = Thread {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
            val audioBuffer = ByteArray(bufferSize)

            while (isRecording.get()) {
                val readResult = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0
                if (readResult > 0) {
                    ringBuffer.write(audioBuffer, 0, readResult)
                    _amplitudes.value = ringBuffer.getAmplitudes(100)
                }
            }
        }.apply { start() }
    }

    private fun stopRecording() {
        isRecording.set(false)
        recordingThread?.join(1000)
        audioRecord?.apply {
            if (state == AudioRecord.STATE_INITIALIZED) {
                stop()
            }
            release()
        }
        audioRecord = null
    }

    fun restartRecording() {
        stopRecording()
        config = AudioConfig(
            sampleRate = prefs.sampleRate,
            channels = prefs.channels,
            bufferDurationSeconds = prefs.bufferDurationSeconds
        )
        ringBuffer = RingBuffer(config.totalBufferBytes)
        startRecording()
    }

    fun setExternalMic(use: Boolean) {
        prefs.useExternalMic = use
        _useExternalMic.value = use
        restartRecording()
    }

    private fun enableBluetoothSco() {
        try {
            audioManager?.let { am ->
                @Suppress("DEPRECATION")
                if (am.isBluetoothScoAvailableOffCall) {
                    @Suppress("DEPRECATION")
                    am.startBluetoothSco()
                    @Suppress("DEPRECATION")
                    am.isBluetoothScoOn = true
                }
            }
        } catch (_: Exception) { }
    }

    private fun disableBluetoothSco() {
        try {
            audioManager?.let { am ->
                @Suppress("DEPRECATION")
                am.isBluetoothScoOn = false
                @Suppress("DEPRECATION")
                am.stopBluetoothSco()
            }
        } catch (_: Exception) { }
    }

    fun captureBuffer(): String? {
        val pcmData = ringBuffer.snapshot()
        if (pcmData.isEmpty()) return null

        val timestamp = System.currentTimeMillis()
        val format = AudioEncoder.Format.fromString(prefs.fileFormat)
        val fileName = "Precord_${timestamp}.${format.extension}"

        val durationMs = (pcmData.size / config.bytesPerSecond.toDouble() * 1000).toLong()

        return saveFile(pcmData, fileName, timestamp, durationMs, format)
    }

    private fun saveFile(
        pcmData: ByteArray,
        fileName: String,
        timestamp: Long,
        durationMs: Long,
        format: AudioEncoder.Format
    ): String? {
        return try {
            val musicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Precord")
            if (!musicDir.exists()) {
                musicDir.mkdirs()
            }

            val outputFile = File(musicDir, fileName)

            val success = AudioEncoder.encodeToFormat(
                pcmData = pcmData,
                sampleRate = config.sampleRate,
                channels = config.channels,
                bitsPerSample = config.bitsPerSample,
                format = format,
                outputFile = outputFile
            )

            if (success && outputFile.exists()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        val mimeType = when (format) {
                            AudioEncoder.Format.WAV -> "audio/wav"
                            AudioEncoder.Format.MP3 -> "audio/mpeg"
                            AudioEncoder.Format.AIFF -> "audio/aiff"
                            AudioEncoder.Format.OGG -> "audio/ogg"
                            AudioEncoder.Format.FLAC -> "audio/flac"
                        }
                        val values = ContentValues().apply {
                            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                            put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
                            put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/Precord")
                            put(MediaStore.Audio.Media.IS_PENDING, 1)
                        }

                        val resolver = contentResolver
                        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)

                        uri?.let {
                            resolver.openOutputStream(it)?.use { os ->
                                if (outputFile.exists()) {
                                    outputFile.inputStream().use { input ->
                                        input.copyTo(os)
                                    }
                                }
                            }
                            values.clear()
                            values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                            resolver.update(uri, values, null, null)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val capturedFile = CapturedFile(
                    filePath = outputFile.absolutePath,
                    fileName = fileName,
                    timestamp = timestamp,
                    durationMs = durationMs,
                    fileSizeBytes = outputFile.length()
                )

                _capturedFiles.value = _capturedFiles.value + capturedFile

                // Show toast "HH:MM:SS capture saved!"
                val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(applicationContext, "$timeStr capture saved!", Toast.LENGTH_SHORT).show()
                }

                // Pro features: auto-enhance, sound detection bookmarks, cloud upload
                if (prefs.isPro) {
                    serviceScope.launch {
                        // Auto-enhance if enabled
                        if (prefs.autoEnhance && (format == AudioEncoder.Format.WAV || format == AudioEncoder.Format.AIFF)) {
                            val enhanceSuccess = AudioEnhancer.enhanceFile(outputFile, config.sampleRate, config.channels, config.bitsPerSample)
                            if (enhanceSuccess) CaptureMetadataStore.setEnhanced(outputFile.absolutePath)
                        }

                        // Sound detection: auto-generate bookmarks
                        if (prefs.soundDetectionEnabled) {
                            val events = SoundDetector.detectTransitions(
                                pcmData = pcmData,
                                sampleRate = config.sampleRate,
                                channels = config.channels,
                                silenceThresholdDb = prefs.silenceThresholdDb
                            )
                            if (events.isNotEmpty()) {
                                CaptureMetadataStore.setBookmarks(
                                    outputFile.absolutePath,
                                    events.map { CaptureMetadataStore.BookmarkEntry(it.offsetMs, it.label) }
                                )
                            }
                        }

                        // Cloud upload if configured
                        if (prefs.cloudUploadEnabled) {
                            CloudUploadManager.uploadFile(this@AudioCaptureService, outputFile)
                        }
                    }
                }

                return outputFile.absolutePath
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun formatDuration(seconds: Int): String {
        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
            else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
        }
    }
}

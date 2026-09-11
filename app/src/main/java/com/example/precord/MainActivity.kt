package com.example.precord

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.precord.data.PreferencesManager
import com.example.precord.service.AudioCaptureService
import com.example.precord.theme.PrecordTheme

class MainActivity : ComponentActivity() {

    private var prefs: PreferencesManager? = null
    private var audioService: AudioCaptureService.LocalBinder? = null
    private var isBound = false

    // Button combo tracking
    private val keySequence = mutableListOf<String>()
    private var lastKeyTime = 0L
    private val comboTimeoutMs = 2000L

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            audioService = service as AudioCaptureService.LocalBinder
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            audioService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = PreferencesManager(this)

        // Only auto-start if permissions are already granted AND auto-start is enabled
        // This prevents the crash on first launch before permissions are granted
        if (prefs?.autoStart == true && prefs?.hasCompletedOnboarding == true &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        ) {
            val intent = Intent(this, AudioCaptureService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }

        // Only bind to the service if onboarding is done (service may not exist yet)
        if (prefs?.hasCompletedOnboarding == true) {
            try {
                Intent(this, AudioCaptureService::class.java).also { intent ->
                    bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
                }
            } catch (_: Exception) { }
        }

        enableEdgeToEdge()
        setContent {
            PrecordTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation()
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (prefs?.buttonComboEnabled != true) return super.onKeyDown(keyCode, event)

        val now = System.currentTimeMillis()

        if (now - lastKeyTime > comboTimeoutMs && keySequence.isNotEmpty()) {
            keySequence.clear()
        }

        val keyName = when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> "DOWN"
            KeyEvent.KEYCODE_VOLUME_UP -> "UP"
            else -> return super.onKeyDown(keyCode, event)
        }

        keySequence.add(keyName)
        lastKeyTime = now

        val targetCombo = prefs?.buttonComboSequence?.split(",")?.map { it.trim() } ?: return true

        if (keySequence.size >= targetCombo.size) {
            val recentKeys = keySequence.takeLast(targetCombo.size)
            if (recentKeys == targetCombo) {
                keySequence.clear()
                triggerComboCapture()
            }
        }

        if (keySequence.size > 20) {
            keySequence.removeAt(0)
        }

        return true
    }

    private fun triggerComboCapture() {
        if (isBound && audioService != null) {
            val result = audioService?.captureBuffer()
            if (result != null) {
                Toast.makeText(this, "✓ Audio captured!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Buffer empty — nothing to capture", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Service not running", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            try { unbindService(serviceConnection) } catch (_: Exception) { }
            isBound = false
        }
    }
}

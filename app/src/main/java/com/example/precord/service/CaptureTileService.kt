package com.example.precord.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.example.precord.data.PreferencesManager

/**
 * Quick Settings Tile for instant capture from the notification shade.
 * Tap the tile to capture the current audio buffer.
 * Long press opens the app.
 */
class CaptureTileService : TileService() {

    private var audioService: AudioCaptureService.LocalBinder? = null
    private var isBound = false

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

    override fun onStartListening() {
        super.onStartListening()
        val prefs = PreferencesManager(this)

        qsTile?.apply {
            label = "Precord"
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                subtitle = if (prefs.isPro) "Tap to Capture" else "Pro Only"
            }
            state = if (prefs.isPro) Tile.STATE_INACTIVE else Tile.STATE_UNAVAILABLE
            updateTile()
        }

        if (prefs.isPro) {
            try {
                Intent(this, AudioCaptureService::class.java).also { intent ->
                    bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
                }
            } catch (_: Exception) { }
        }
    }

    override fun onClick() {
        super.onClick()
        val prefs = PreferencesManager(this)

        if (!prefs.isPro) {
            Toast.makeText(this, "Quick Tile is a Pro feature", Toast.LENGTH_SHORT).show()
            return
        }

        if (isBound && audioService != null) {
            val result = audioService?.captureBuffer()
            if (result != null) {
                Toast.makeText(this, "✓ Audio captured!", Toast.LENGTH_SHORT).show()
                // Flash the tile briefly
                qsTile?.apply {
                    state = Tile.STATE_ACTIVE
                    updateTile()
                }
                // Reset tile state after a moment
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    qsTile?.apply {
                        state = Tile.STATE_INACTIVE
                        updateTile()
                    }
                }, 1000)
            } else {
                Toast.makeText(this, "Buffer empty — start recording first", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Precord service not running", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        if (isBound) {
            try { unbindService(serviceConnection) } catch (_: Exception) { }
            isBound = false
        }
    }
}

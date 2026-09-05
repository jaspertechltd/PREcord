package com.example.precord.ui.main

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.precord.data.PreferencesManager
import com.example.precord.service.AudioCaptureService
import com.example.precord.service.CapturedFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val isBuffering: Boolean = false,
    val amplitudes: FloatArray = FloatArray(0),
    val bufferElapsedSeconds: Int = 0,
    val bufferMaxSeconds: Int = 60,
    val captures: List<CapturedFile> = emptyList(),
    val isPermissionGranted: Boolean = false,
    val showPermissionRationale: Boolean = true,
    val useExternalMic: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    val prefs = PreferencesManager(application)
    private var audioService: AudioCaptureService.LocalBinder? = null
    private var isBound = false
    private var startTimeMs = 0L

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            audioService = service as AudioCaptureService.LocalBinder
            isBound = true
            startTimeMs = System.currentTimeMillis()
            _uiState.update { it.copy(bufferMaxSeconds = prefs.bufferDurationSeconds) }
            observeService()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            audioService = null
        }
    }

    init {
        _uiState.update {
            it.copy(
                useExternalMic = prefs.useExternalMic,
                bufferMaxSeconds = prefs.bufferDurationSeconds
            )
        }
        Intent(application, AudioCaptureService::class.java).also { intent ->
            application.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun observeService() {
        audioService?.let { service ->
            viewModelScope.launch {
                service.amplitudesFlow.collect { amps ->
                    _uiState.update { it.copy(amplitudes = amps) }
                }
            }
            viewModelScope.launch {
                service.capturedFilesFlow.collect { files ->
                    _uiState.update { it.copy(captures = files) }
                }
            }
            viewModelScope.launch {
                service.useExternalMicFlow.collect { use ->
                    _uiState.update { it.copy(useExternalMic = use) }
                }
            }
            viewModelScope.launch {
                while (true) {
                    val recording = service.isRecording()
                    val elapsed = if (recording && startTimeMs > 0) {
                        ((System.currentTimeMillis() - startTimeMs) / 1000).toInt()
                            .coerceAtMost(prefs.bufferDurationSeconds)
                    } else 0
                    _uiState.update {
                        it.copy(
                            isBuffering = recording,
                            bufferElapsedSeconds = elapsed,
                            bufferMaxSeconds = prefs.bufferDurationSeconds
                        )
                    }
                    delay(500)
                }
            }
        }
    }

    fun startBuffering() {
        val intent = Intent(getApplication(), AudioCaptureService::class.java)
        getApplication<Application>().startForegroundService(intent)
        startTimeMs = System.currentTimeMillis()
        _uiState.update { it.copy(isBuffering = true) }
    }

    fun stopBuffering() {
        val intent = Intent(getApplication(), AudioCaptureService::class.java)
        getApplication<Application>().stopService(intent)
        _uiState.update { it.copy(isBuffering = false) }
    }

    fun capture() {
        audioService?.captureBuffer()
    }

    fun toggleExternalMic() {
        val newValue = !_uiState.value.useExternalMic
        audioService?.setUseExternalMic(newValue)
        _uiState.update { it.copy(useExternalMic = newValue) }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update {
            it.copy(
                isPermissionGranted = granted,
                showPermissionRationale = false
            )
        }
        if (granted) {
            startBuffering()
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            getApplication<Application>().unbindService(serviceConnection)
            isBound = false
        }
    }
}

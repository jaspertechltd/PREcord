package com.example.precord.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("precord_prefs", Context.MODE_PRIVATE)

    var bufferDurationSeconds: Int
        get() = prefs.getInt("buffer_duration_seconds", 60)
        set(value) = prefs.edit().putInt("buffer_duration_seconds", value.coerceIn(10, 14400)).apply()

    var sampleRate: Int
        get() = prefs.getInt("sample_rate", 44100)
        set(value) = prefs.edit().putInt("sample_rate", value).apply()

    var channels: Int
        get() = prefs.getInt("channels", 1)
        set(value) = prefs.edit().putInt("channels", value).apply()

    var autoStart: Boolean
        get() = prefs.getBoolean("auto_start", false)
        set(value) = prefs.edit().putBoolean("auto_start", value).apply()

    var fileFormat: String
        get() = prefs.getString("file_format", "WAV") ?: "WAV"
        set(value) = prefs.edit().putString("file_format", value).apply()

    var isStereo: Boolean
        get() = prefs.getBoolean("is_stereo", false)
        set(value) {
            prefs.edit().putBoolean("is_stereo", value).apply()
            channels = if (value) 2 else 1
        }

    var useExternalMic: Boolean
        get() = prefs.getBoolean("use_external_mic", false)
        set(value) = prefs.edit().putBoolean("use_external_mic", value).apply()

    var buttonComboSequence: String
        get() = prefs.getString("button_combo", "DOWN,UP,DOWN") ?: "DOWN,UP,DOWN"
        set(value) = prefs.edit().putString("button_combo", value).apply()

    var buttonComboEnabled: Boolean
        get() = prefs.getBoolean("button_combo_enabled", true)
        set(value) = prefs.edit().putBoolean("button_combo_enabled", value).apply()

    var hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean("has_completed_onboarding", false)
        set(value) = prefs.edit().putBoolean("has_completed_onboarding", value).apply()

    var isPro: Boolean
        get() = prefs.getBoolean("is_pro", false)
        set(value) = prefs.edit().putBoolean("is_pro", value).apply()

    // ═══════════════════════════════════════
    // NEW: Shake-to-Capture
    // ═══════════════════════════════════════
    var shakeEnabled: Boolean
        get() = prefs.getBoolean("shake_enabled", false)
        set(value) = prefs.edit().putBoolean("shake_enabled", value).apply()

    var shakeSensitivity: Float
        get() = prefs.getFloat("shake_sensitivity", 2.5f) // G-force threshold
        set(value) = prefs.edit().putFloat("shake_sensitivity", value).apply()

    // ═══════════════════════════════════════
    // NEW: Smart Sound Detection
    // ═══════════════════════════════════════
    var soundDetectionEnabled: Boolean
        get() = prefs.getBoolean("sound_detection_enabled", false)
        set(value) = prefs.edit().putBoolean("sound_detection_enabled", value).apply()

    var silenceThresholdDb: Float
        get() = prefs.getFloat("silence_threshold_db", -40f)
        set(value) = prefs.edit().putFloat("silence_threshold_db", value).apply()

    // ═══════════════════════════════════════
    // NEW: Audio Enhancement
    // ═══════════════════════════════════════
    var autoEnhance: Boolean
        get() = prefs.getBoolean("auto_enhance", false)
        set(value) = prefs.edit().putBoolean("auto_enhance", value).apply()

    // ═══════════════════════════════════════
    // NEW: Cloud Upload
    // ═══════════════════════════════════════
    var cloudUploadEnabled: Boolean
        get() = prefs.getBoolean("cloud_upload_enabled", false)
        set(value) = prefs.edit().putBoolean("cloud_upload_enabled", value).apply()

    var cloudUploadUri: String?
        get() = prefs.getString("cloud_upload_uri", null)
        set(value) = prefs.edit().putString("cloud_upload_uri", value).apply()

    // ═══════════════════════════════════════
    // NEW: Auto-Transcribe
    // ═══════════════════════════════════════
    var autoTranscribe: Boolean
        get() = prefs.getBoolean("auto_transcribe", false)
        set(value) = prefs.edit().putBoolean("auto_transcribe", value).apply()
}

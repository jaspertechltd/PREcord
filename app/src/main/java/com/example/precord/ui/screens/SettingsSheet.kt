package com.example.precord.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.precord.audio.AudioEncoder
import com.example.precord.data.PreferencesManager
import kotlin.math.ln
import kotlin.math.exp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val focusManager = LocalFocusManager.current

    var bufferDuration by remember { mutableIntStateOf(prefs.bufferDurationSeconds) }
    var bufferDurationText by remember { mutableStateOf(formatDurationForInput(prefs.bufferDurationSeconds)) }
    var autoStart by remember { mutableStateOf(prefs.autoStart) }
    var sampleRate by remember { mutableIntStateOf(prefs.sampleRate) }
    var fileFormat by remember { mutableStateOf(prefs.fileFormat) }
    var isStereo by remember { mutableStateOf(prefs.isStereo) }
    var useExternalMic by remember { mutableStateOf(prefs.useExternalMic) }
    var buttonComboEnabled by remember { mutableStateOf(prefs.buttonComboEnabled) }
    var buttonCombo by remember { mutableStateOf(prefs.buttonComboSequence) }
    var isPro by remember { mutableStateOf(prefs.isPro) }

    // Pro feature states
    var shakeEnabled by remember { mutableStateOf(prefs.shakeEnabled) }
    var shakeSensitivity by remember { mutableFloatStateOf(prefs.shakeSensitivity) }
    var soundDetectionEnabled by remember { mutableStateOf(prefs.soundDetectionEnabled) }
    var silenceThreshold by remember { mutableFloatStateOf(prefs.silenceThresholdDb) }
    var autoEnhance by remember { mutableStateOf(prefs.autoEnhance) }
    var autoTranscribe by remember { mutableStateOf(prefs.autoTranscribe) }
    var cloudUploadEnabled by remember { mutableStateOf(prefs.cloudUploadEnabled) }
    var cloudUploadUri by remember { mutableStateOf(prefs.cloudUploadUri) }

    val channels = if (isStereo) 2 else 1

    // Logarithmic slider mapping for 10s - 14400s (4 hours)
    val minLog = ln(10f)
    val maxLog = ln(14400f)

    fun durationToSlider(dur: Int): Float {
        return ((ln(dur.toFloat()) - minLog) / (maxLog - minLog)).coerceIn(0f, 1f)
    }
    fun sliderToDuration(pos: Float): Int {
        return exp(minLog + pos * (maxLog - minLog)).roundToInt().coerceIn(10, 14400)
    }

    var sliderPosition by remember { mutableFloatStateOf(durationToSlider(bufferDuration)) }

    // File size estimation functions
    fun estimatePcmBytesPerSecond(): Long {
        return (sampleRate * channels * 2).toLong() // 16-bit = 2 bytes per sample
    }

    fun estimateFileSizeBytes(format: AudioEncoder.Format, durationSecs: Int): Long {
        val pcmPerSecond = estimatePcmBytesPerSecond()
        val totalPcm = pcmPerSecond * durationSecs
        return when (format) {
            AudioEncoder.Format.WAV -> totalPcm + 44 // WAV header
            AudioEncoder.Format.AIFF -> totalPcm + 72 // AIFF-C header
            AudioEncoder.Format.FLAC -> (totalPcm * 0.55).toLong() // ~55% of PCM (typical)
            AudioEncoder.Format.MP3 -> {
                // ~192kbps = 24KB/s for high quality
                val kbps = 192L
                (kbps * 1000 / 8) * durationSecs
            }
            AudioEncoder.Format.OGG -> {
                // ~160kbps Vorbis = ~20KB/s
                val kbps = 160L
                (kbps * 1000 / 8) * durationSecs
            }
        }
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes} B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024L * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }

    fun formatPerSecond(format: AudioEncoder.Format): String {
        val bytesPerSec = when (format) {
            AudioEncoder.Format.WAV, AudioEncoder.Format.AIFF -> estimatePcmBytesPerSecond()
            AudioEncoder.Format.FLAC -> (estimatePcmBytesPerSecond() * 0.55).toLong()
            AudioEncoder.Format.MP3 -> 192L * 1000 / 8
            AudioEncoder.Format.OGG -> 160L * 1000 / 8
        }
        return formatFileSize(bytesPerSec) + "/s"
    }

    // Half-screen modal
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetMaxWidth = BottomSheetDefaults.SheetMaxWidth
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // ═══════════════════════════════════════
            // BUFFER DURATION (Slider + Text Input)
            // ═══════════════════════════════════════
            SectionLabel("Buffer Duration")
            Text(
                text = formatDurationDisplay(bufferDuration),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Slider(
                    value = sliderPosition,
                    onValueChange = {
                        sliderPosition = it
                        bufferDuration = sliderToDuration(it)
                        bufferDurationText = formatDurationForInput(bufferDuration)
                        prefs.bufferDurationSeconds = bufferDuration
                    },
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.width(12.dp))

                OutlinedTextField(
                    value = bufferDurationText,
                    onValueChange = { input ->
                        bufferDurationText = input
                        val parsed = parseDurationInput(input)
                        if (parsed != null && parsed in 10..14400) {
                            bufferDuration = parsed
                            sliderPosition = durationToSlider(parsed)
                            prefs.bufferDurationSeconds = parsed
                        }
                    },
                    modifier = Modifier.width(90.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        textAlign = TextAlign.Center
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("10s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("4 hours", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Quick presets
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(30 to "30s", 60 to "1m", 300 to "5m", 900 to "15m", 3600 to "1h").forEach { (secs, label) ->
                    FilterChip(
                        selected = bufferDuration == secs,
                        onClick = {
                            bufferDuration = secs
                            sliderPosition = durationToSlider(secs)
                            bufferDurationText = formatDurationForInput(secs)
                            prefs.bufferDurationSeconds = secs
                        },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            SettingsDivider()

            // ═══════════════════════════════════════
            // FILE FORMAT WITH DYNAMIC FILE SIZES
            // ═══════════════════════════════════════
            SectionLabel("File Format")
            Text(
                "Estimated sizes for ${formatDurationDisplay(bufferDuration)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val formats = AudioEncoder.Format.entries
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                formats.forEach { fmt ->
                    val totalSize = estimateFileSizeBytes(fmt, bufferDuration)
                    val perSecond = formatPerSecond(fmt)
                    val isSelected = fileFormat == fmt.name

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                fileFormat = fmt.name
                                prefs.fileFormat = fmt.name
                            }
                            .then(
                                if (isSelected)
                                    Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                else Modifier
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Radio button
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                fileFormat = fmt.name
                                prefs.fileFormat = fmt.name
                            },
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Format name + description
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = fmt.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = fmt.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Dynamic file size estimates
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formatFileSize(totalSize),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = perSecond,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            SettingsDivider()

            // ═══════════════════════════════════════
            // AUDIO QUALITY
            // ═══════════════════════════════════════
            SectionLabel("Audio Quality")
            val qualities = listOf(
                22050 to "Standard (22kHz)",
                44100 to "High (44.1kHz)",
                48000 to "Studio (48kHz)"
            )
            qualities.forEach { (rate, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            sampleRate = rate
                            prefs.sampleRate = rate
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = sampleRate == rate,
                        onClick = {
                            sampleRate = rate
                            prefs.sampleRate = rate
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(label)
                }
            }

            SettingsDivider()

            // ═══════════════════════════════════════
            // STEREO / MONO TOGGLE
            // ═══════════════════════════════════════
            SettingsToggleRow(
                label = "Stereo Recording",
                description = if (isStereo) "Recording in stereo (2 channels)" else "Recording in mono (1 channel)",
                checked = isStereo,
                onCheckedChange = {
                    isStereo = it
                    prefs.isStereo = it
                }
            )

            SettingsDivider()

            // ═══════════════════════════════════════
            // EXTERNAL / BLUETOOTH MIC
            // ═══════════════════════════════════════
            SettingsToggleRow(
                label = "Use External/Bluetooth Mic",
                description = "Route audio to connected external or Bluetooth microphone when available",
                checked = useExternalMic,
                onCheckedChange = {
                    useExternalMic = it
                    prefs.useExternalMic = it
                }
            )

            SettingsDivider()

            // ═══════════════════════════════════════
            // BUTTON COMBO CAPTURE
            // ═══════════════════════════════════════
            SectionLabel("Hardware Button Capture")

            SettingsToggleRow(
                label = "Volume Button Combo",
                description = "Trigger capture with a volume key sequence",
                checked = buttonComboEnabled,
                onCheckedChange = {
                    buttonComboEnabled = it
                    prefs.buttonComboEnabled = it
                }
            )

            if (buttonComboEnabled) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Current combo:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    buttonCombo.split(",").forEach { key ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (key.trim() == "DOWN") "Vol ▼" else "Vol ▲",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Text(
                    "Quick presets:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ComboPresetChip("▼▲▼", "DOWN,UP,DOWN", buttonCombo) {
                        buttonCombo = it
                        prefs.buttonComboSequence = it
                    }
                    ComboPresetChip("▲▼▲", "UP,DOWN,UP", buttonCombo) {
                        buttonCombo = it
                        prefs.buttonComboSequence = it
                    }
                    ComboPresetChip("▼▼▼", "DOWN,DOWN,DOWN", buttonCombo) {
                        buttonCombo = it
                        prefs.buttonComboSequence = it
                    }
                    ComboPresetChip("▲▲▲", "UP,UP,UP", buttonCombo) {
                        buttonCombo = it
                        prefs.buttonComboSequence = it
                    }
                }
            }

            SettingsDivider()

            // ═══════════════════════════════════════
            // AUTO-START
            // ═══════════════════════════════════════
            SettingsToggleRow(
                label = "Auto-start on Launch",
                description = "Begin buffering automatically when app opens",
                checked = autoStart,
                onCheckedChange = {
                    autoStart = it
                    prefs.autoStart = it
                }
            )

            SettingsDivider()

            // ═══════════════════════════════════════
            // PRO FEATURES SECTION HEADER
            // ═══════════════════════════════════════
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    "PRO FEATURES",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                if (!isPro) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "UPGRADE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // ═══════════════════════════════════════
            // SHAKE TO CAPTURE (PRO)
            // ═══════════════════════════════════════
            SectionLabel("Shake to Capture")
            SettingsToggleRow(
                label = "Shake to Capture",
                description = if (isPro) "Shake your phone to trigger a capture" else "Pro feature — upgrade to unlock",
                checked = shakeEnabled,
                onCheckedChange = {
                    if (isPro) {
                        shakeEnabled = it
                        prefs.shakeEnabled = it
                    }
                }
            )

            if (shakeEnabled && isPro) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Sensitivity", style = MaterialTheme.typography.labelMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Gentle", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = shakeSensitivity,
                        onValueChange = {
                            shakeSensitivity = it
                            prefs.shakeSensitivity = it
                        },
                        valueRange = 1.5f..5f,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    Text("Firm", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            SettingsDivider()

            // ═══════════════════════════════════════
            // SMART SOUND DETECTION (PRO)
            // ═══════════════════════════════════════
            SectionLabel("Smart Sound Detection")
            SettingsToggleRow(
                label = "Auto-Bookmark Sounds",
                description = if (isPro) "Detect silence→sound transitions and bookmark them" else "Pro feature — upgrade to unlock",
                checked = soundDetectionEnabled,
                onCheckedChange = {
                    if (isPro) {
                        soundDetectionEnabled = it
                        prefs.soundDetectionEnabled = it
                    }
                }
            )

            if (soundDetectionEnabled && isPro) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Silence Threshold: ${silenceThreshold.toInt()} dB", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = silenceThreshold,
                    onValueChange = {
                        silenceThreshold = it
                        prefs.silenceThresholdDb = it
                    },
                    valueRange = -60f..-20f,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("More sensitive", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Less sensitive", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            SettingsDivider()

            // ═══════════════════════════════════════
            // AUTO-ENHANCE (PRO)
            // ═══════════════════════════════════════
            SectionLabel("Audio Enhancement")
            SettingsToggleRow(
                label = "Auto-Enhance Captures",
                description = if (isPro) "Normalize volume & remove rumble on capture (WAV/AIFF only)" else "Pro feature — upgrade to unlock",
                checked = autoEnhance,
                onCheckedChange = {
                    if (isPro) {
                        autoEnhance = it
                        prefs.autoEnhance = it
                    }
                }
            )

            SettingsDivider()

            // ═══════════════════════════════════════
            // AUTO-TRANSCRIBE (PRO)
            // ═══════════════════════════════════════
            SectionLabel("Transcription")
            SettingsToggleRow(
                label = "Auto-Transcribe",
                description = if (isPro) "Attempt speech recognition on new captures" else "Pro feature — upgrade to unlock",
                checked = autoTranscribe,
                onCheckedChange = {
                    if (isPro) {
                        autoTranscribe = it
                        prefs.autoTranscribe = it
                    }
                }
            )

            SettingsDivider()

            // ═══════════════════════════════════════
            // CLOUD BACKUP (PRO)
            // ═══════════════════════════════════════
            SectionLabel("Cloud Backup")
            SettingsToggleRow(
                label = "Auto-Upload Captures",
                description = if (isPro) {
                    if (cloudUploadUri != null) "Uploading to configured folder" else "Pick a backup folder to enable"
                } else "Pro feature — upgrade to unlock",
                checked = cloudUploadEnabled,
                onCheckedChange = {
                    if (isPro) {
                        cloudUploadEnabled = it
                        prefs.cloudUploadEnabled = it
                    }
                }
            )

            if (isPro && cloudUploadEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT_TREE)
                        // This will be handled by the Activity's result launcher
                        // For now, show a toast with instructions
                        android.widget.Toast.makeText(
                            context,
                            "Use the system file picker to select a backup folder (Google Drive, Dropbox, etc.)",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (cloudUploadUri != null) "Change Backup Folder" else "Select Backup Folder")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Save Location", style = MaterialTheme.typography.labelLarge)
            Text(
                "Music/Precord/",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Precord v1.1",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    )
}

@Composable
private fun SettingsToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null
        )
    }
}

@Composable
private fun ComboPresetChip(
    label: String,
    value: String,
    currentValue: String,
    onSelect: (String) -> Unit
) {
    FilterChip(
        selected = currentValue == value,
        onClick = { onSelect(value) },
        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
    )
}

private fun formatDurationDisplay(seconds: Int): String {
    return when {
        seconds < 60 -> "${seconds} seconds"
        seconds < 3600 -> {
            val m = seconds / 60
            val s = seconds % 60
            if (s > 0) "${m}m ${s}s" else "${m} minute${if (m > 1) "s" else ""}"
        }
        else -> {
            val h = seconds / 3600
            val m = (seconds % 3600) / 60
            if (m > 0) "${h}h ${m}m" else "${h} hour${if (h > 1) "s" else ""}"
        }
    }
}

private fun formatDurationForInput(seconds: Int): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> {
            val m = seconds / 60
            val s = seconds % 60
            if (s > 0) "${m}m${s}s" else "${m}m"
        }
        else -> {
            val h = seconds / 3600
            val m = (seconds % 3600) / 60
            if (m > 0) "${h}h${m}m" else "${h}h"
        }
    }
}

private fun parseDurationInput(input: String): Int? {
    val trimmed = input.trim().lowercase()
    if (trimmed.isEmpty()) return null

    trimmed.toIntOrNull()?.let { return it }

    if (trimmed.endsWith("s") && !trimmed.contains("m") && !trimmed.contains("h")) {
        trimmed.dropLast(1).trim().toIntOrNull()?.let { return it }
    }

    var total = 0
    val hourMatch = Regex("(\\d+)h").find(trimmed)
    val minMatch = Regex("(\\d+)m").find(trimmed)
    val secMatch = Regex("(\\d+)s").find(trimmed)

    hourMatch?.groupValues?.get(1)?.toIntOrNull()?.let { total += it * 3600 }
    minMatch?.groupValues?.get(1)?.toIntOrNull()?.let { total += it * 60 }
    secMatch?.groupValues?.get(1)?.toIntOrNull()?.let { total += it }

    if (total > 0) return total

    val parts = trimmed.split(":")
    if (parts.size == 2) {
        val m = parts[0].toIntOrNull() ?: return null
        val s = parts[1].toIntOrNull() ?: return null
        return m * 60 + s
    }
    if (parts.size == 3) {
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val s = parts[2].toIntOrNull() ?: return null
        return h * 3600 + m * 60 + s
    }

    return null
}

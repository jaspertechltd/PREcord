package com.example.precord.ui.screens

import android.media.MediaPlayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.precord.data.CaptureMetadataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
    filePath: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val file = remember { File(filePath) }
    val fileName = file.name

    // Load metadata for bookmarks
    val metadata = remember { CaptureMetadataStore.load(filePath) }

    // Generate waveform data from file
    val waveformData = remember { generateWaveform(filePath, 200) }

    // MediaPlayer state
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf(0) }

    // A-B Loop state
    var loopEnabled by remember { mutableStateOf(false) }
    var loopStartFraction by remember { mutableFloatStateOf(0f) }
    var loopEndFraction by remember { mutableFloatStateOf(1f) }
    var draggingMarker by remember { mutableStateOf<String?>(null) } // "A", "B", or null

    // Initialize MediaPlayer
    LaunchedEffect(filePath) {
        mediaPlayer = try {
            MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                durationMs = duration
            }
        } catch (_: Exception) { null }
    }

    // Position tracking
    LaunchedEffect(isPlaying) {
        while (isPlaying && isActive) {
            mediaPlayer?.let { mp ->
                currentPositionMs = mp.currentPosition

                // A-B Loop logic
                if (loopEnabled) {
                    val loopEndMs = (loopEndFraction * durationMs).toInt()
                    if (currentPositionMs >= loopEndMs) {
                        val loopStartMs = (loopStartFraction * durationMs).toInt()
                        mp.seekTo(loopStartMs)
                    }
                }
            }
            delay(50)
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(fileName, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = {
                        mediaPlayer?.release()
                        mediaPlayer = null
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Waveform with A-B markers
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(12.dp)
                    )
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val secondaryColor = MaterialTheme.colorScheme.secondary
                val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
                val loopRegionColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                val bookmarkColor = MaterialTheme.colorScheme.tertiary

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .pointerInput(loopEnabled, durationMs) {
                            if (!loopEnabled) {
                                detectTapGestures { offset ->
                                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                    val seekMs = (fraction * durationMs).toInt()
                                    mediaPlayer?.seekTo(seekMs)
                                    currentPositionMs = seekMs
                                }
                            } else {
                                detectTapGestures { offset ->
                                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                    // Determine which marker is closer
                                    val distA = abs(fraction - loopStartFraction)
                                    val distB = abs(fraction - loopEndFraction)
                                    if (distA < distB) {
                                        loopStartFraction = min(fraction, loopEndFraction - 0.01f)
                                    } else {
                                        loopEndFraction = max(fraction, loopStartFraction + 0.01f)
                                    }
                                }
                            }
                        }
                        .pointerInput(loopEnabled) {
                            if (loopEnabled) {
                                detectHorizontalDragGestures(
                                    onDragStart = { offset ->
                                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                        val distA = abs(fraction - loopStartFraction)
                                        val distB = abs(fraction - loopEndFraction)
                                        draggingMarker = if (distA < distB) "A" else "B"
                                    },
                                    onDragEnd = { draggingMarker = null },
                                    onDragCancel = { draggingMarker = null }
                                ) { _, dragAmount ->
                                    val delta = dragAmount / size.width
                                    when (draggingMarker) {
                                        "A" -> loopStartFraction = (loopStartFraction + delta).coerceIn(0f, loopEndFraction - 0.01f)
                                        "B" -> loopEndFraction = (loopEndFraction + delta).coerceIn(loopStartFraction + 0.01f, 1f)
                                    }
                                }
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    // Draw loop region highlight
                    if (loopEnabled) {
                        drawRect(
                            color = loopRegionColor,
                            topLeft = Offset(loopStartFraction * w, 0f),
                            size = Size((loopEndFraction - loopStartFraction) * w, h)
                        )
                    }

                    // Draw waveform
                    if (waveformData.isNotEmpty()) {
                        val barWidth = w / waveformData.size
                        val midY = h / 2
                        waveformData.forEachIndexed { index, amplitude ->
                            val x = index * barWidth
                            val barH = amplitude * midY * 0.9f
                            val fraction = index.toFloat() / waveformData.size

                            val color = when {
                                loopEnabled && fraction in loopStartFraction..loopEndFraction -> primaryColor
                                durationMs > 0 && fraction <= currentPositionMs.toFloat() / durationMs -> primaryColor.copy(alpha = 0.7f)
                                else -> onSurfaceVariant.copy(alpha = 0.4f)
                            }

                            drawRect(
                                color = color,
                                topLeft = Offset(x, midY - barH),
                                size = Size(max(barWidth - 1f, 1f), barH * 2)
                            )
                        }
                    }

                    // Draw bookmarks
                    metadata.bookmarks.forEach { bookmark ->
                        if (durationMs > 0) {
                            val bFraction = bookmark.offsetMs.toFloat() / durationMs
                            val bx = bFraction * w
                            drawLine(
                                color = bookmarkColor,
                                start = Offset(bx, 0f),
                                end = Offset(bx, h),
                                strokeWidth = 2f
                            )
                            // Small triangle at top
                            val triPath = Path().apply {
                                moveTo(bx, 0f)
                                lineTo(bx - 4f, 8f)
                                lineTo(bx + 4f, 8f)
                                close()
                            }
                            drawPath(triPath, bookmarkColor)
                        }
                    }

                    // Draw playhead
                    if (durationMs > 0) {
                        val playheadX = (currentPositionMs.toFloat() / durationMs) * w
                        drawLine(
                            color = secondaryColor,
                            start = Offset(playheadX, 0f),
                            end = Offset(playheadX, h),
                            strokeWidth = 3f
                        )
                    }

                    // Draw A-B markers
                    if (loopEnabled) {
                        // Marker A
                        val ax = loopStartFraction * w
                        drawLine(Color(0xFF4CAF50), Offset(ax, 0f), Offset(ax, h), 4f)
                        drawCircle(Color(0xFF4CAF50), 8f, Offset(ax, 12f))

                        // Marker B
                        val bx2 = loopEndFraction * w
                        drawLine(Color(0xFFF44336), Offset(bx2, 0f), Offset(bx2, h), 4f)
                        drawCircle(Color(0xFFF44336), 8f, Offset(bx2, 12f))
                    }
                }

                // A-B labels
                if (loopEnabled) {
                    Text(
                        "A",
                        color = Color(0xFF4CAF50),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 4.dp, top = 4.dp)
                    )
                    Text(
                        "B",
                        color = Color(0xFFF44336),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 4.dp, top = 4.dp)
                    )
                }
            }

            // Timestamps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    formatTimeMs(currentPositionMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (loopEnabled) {
                    Text(
                        "Loop: ${formatTimeMs((loopStartFraction * durationMs).toInt())} → ${formatTimeMs((loopEndFraction * durationMs).toInt())}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    formatTimeMs(durationMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Seek slider
            Slider(
                value = if (durationMs > 0) currentPositionMs.toFloat() / durationMs else 0f,
                onValueChange = { fraction ->
                    val seekMs = (fraction * durationMs).toInt()
                    mediaPlayer?.seekTo(seekMs)
                    currentPositionMs = seekMs
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Playback controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // A-B Loop toggle
                IconButton(onClick = {
                    loopEnabled = !loopEnabled
                    if (loopEnabled) {
                        // Set default loop markers to 25%-75%
                        loopStartFraction = 0.25f
                        loopEndFraction = 0.75f
                    }
                }) {
                    Icon(
                        imageVector = if (loopEnabled) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = if (loopEnabled) "Disable Loop" else "Enable A-B Loop",
                        tint = if (loopEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Play/Pause
                FilledIconButton(
                    onClick = {
                        mediaPlayer?.let { mp ->
                            if (isPlaying) {
                                mp.pause()
                                isPlaying = false
                            } else {
                                if (loopEnabled) {
                                    val pos = mp.currentPosition
                                    val loopStartMs = (loopStartFraction * durationMs).toInt()
                                    val loopEndMs = (loopEndFraction * durationMs).toInt()
                                    if (pos < loopStartMs || pos >= loopEndMs) {
                                        mp.seekTo(loopStartMs)
                                    }
                                }
                                mp.start()
                                isPlaying = true
                            }
                        }
                    },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Stop
                IconButton(onClick = {
                    mediaPlayer?.let { mp ->
                        mp.pause()
                        mp.seekTo(0)
                        isPlaying = false
                        currentPositionMs = 0
                    }
                }) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Loop instructions
            if (loopEnabled) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Drag the green (A) and red (B) markers on the waveform to set loop boundaries. Tap the waveform to snap the nearest marker.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Bookmarks list
            if (metadata.bookmarks.isNotEmpty()) {
                Text(
                    "Bookmarks",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                metadata.bookmarks.forEach { bookmark ->
                    Surface(
                        onClick = {
                            mediaPlayer?.seekTo(bookmark.offsetMs.toInt())
                            currentPositionMs = bookmark.offsetMs.toInt()
                        },
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                formatTimeMs(bookmark.offsetMs.toInt()),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                bookmark.label,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

private fun formatTimeMs(ms: Int): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

/**
 * Generate waveform amplitudes from an audio file.
 * Returns normalized float values 0..1.
 */
private fun generateWaveform(filePath: String, numBars: Int): FloatArray {
    return try {
        val file = File(filePath)
        val bytes = file.readBytes()

        // Find PCM data region
        val pcmOffset = when (file.extension.lowercase()) {
            "wav" -> findWavDataOffset(bytes)
            "aiff" -> 72
            else -> return FloatArray(numBars) { 0.2f } // Compressed formats: show flat waveform
        }

        if (pcmOffset < 0 || pcmOffset >= bytes.size) return FloatArray(numBars) { 0.2f }

        val pcmData = bytes.copyOfRange(pcmOffset, bytes.size)
        val totalSamples = pcmData.size / 2
        if (totalSamples < numBars) return FloatArray(numBars) { 0.2f }

        val samplesPerBar = totalSamples / numBars
        val result = FloatArray(numBars)

        for (i in 0 until numBars) {
            var maxAmp = 0
            val startSample = i * samplesPerBar
            for (j in 0 until samplesPerBar) {
                val idx = (startSample + j) * 2
                if (idx + 1 < pcmData.size) {
                    val low = pcmData[idx].toInt() and 0xFF
                    val high = pcmData[idx + 1].toInt()
                    val sample = abs((high shl 8) or low)
                    maxAmp = max(maxAmp, sample)
                }
            }
            result[i] = maxAmp / 32768f
        }

        result
    } catch (_: Exception) {
        FloatArray(numBars) { 0.2f }
    }
}

private fun findWavDataOffset(bytes: ByteArray): Int {
    for (i in 0 until bytes.size - 4) {
        if (bytes[i] == 'd'.code.toByte() &&
            bytes[i + 1] == 'a'.code.toByte() &&
            bytes[i + 2] == 't'.code.toByte() &&
            bytes[i + 3] == 'a'.code.toByte()
        ) {
            return i + 8
        }
    }
    return 44
}

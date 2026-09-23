package com.example.precord.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.precord.theme.WaveformActiveColor
import com.example.precord.theme.WaveformColor
import kotlin.math.abs

/**
 * Static waveform visualizer - draws amplitude bars.
 * Used as a building block and for non-animated contexts.
 */
@Composable
fun WaveformVisualizer(
    amplitudes: FloatArray,
    modifier: Modifier = Modifier,
    color: Color = WaveformColor,
    barWidth: Dp = 3.dp,
    barSpacing: Dp = 1.dp,
    minBarHeight: Dp = 2.dp
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val barWidthPx = barWidth.toPx()
        val spacingPx = barSpacing.toPx()
        val minHeightPx = minBarHeight.toPx()
        val totalBarWidth = barWidthPx + spacingPx

        val maxBars = (width / totalBarWidth).toInt()
        val startIndex = if (amplitudes.size > maxBars) amplitudes.size - maxBars else 0

        var startX = width - barWidthPx

        for (i in (amplitudes.size - 1) downTo startIndex) {
            val amp = amplitudes[i]
            val normalizedAmp = abs(amp).coerceIn(0f, 1f)
            val barHeightPx = (normalizedAmp * height).coerceAtLeast(minHeightPx)

            val startY = (height - barHeightPx) / 2f

            drawRoundRect(
                color = color,
                topLeft = Offset(startX, startY),
                size = Size(barWidthPx, barHeightPx),
                cornerRadius = CornerRadius(barWidthPx / 2f, barWidthPx / 2f)
            )

            startX -= totalBarWidth
            if (startX < 0) break
        }
    }
}

/**
 * Animated waveform visualizer with scrolling treadmill effect.
 * Shows the last ~10 seconds of audio scrolling right-to-left.
 * New amplitudes appear on the right and travel to the left edge.
 */
@Composable
fun AnimatedWaveformVisualizer(
    amplitudes: FloatArray,
    modifier: Modifier = Modifier,
    isBuffering: Boolean = false
) {
    // Keep a rolling buffer of amplitude history for smooth scrolling
    val amplitudeHistory = remember { mutableStateListOf<Float>() }
    val maxHistorySize = 500 // enough for ~10 seconds at typical update rates
    
    // Append new amplitudes when they change
    LaunchedEffect(amplitudes) {
        if (amplitudes.isNotEmpty()) {
            // Add new samples to history
            for (amp in amplitudes) {
                amplitudeHistory.add(amp)
            }
            // Trim to max size
            while (amplitudeHistory.size > maxHistorySize) {
                amplitudeHistory.removeAt(0)
            }
        }
    }

    // Smooth sub-pixel scroll offset for treadmill effect
    val scrollOffset = remember { Animatable(0f) }
    
    LaunchedEffect(amplitudes) {
        if (amplitudes.isNotEmpty() && isBuffering) {
            // Reset and animate a small scroll for each new batch
            scrollOffset.snapTo(4f) // pixels to scroll per update
            scrollOffset.animateTo(
                0f,
                animationSpec = tween(
                    durationMillis = 80,
                    easing = LinearEasing
                )
            )
        }
    }

    // Glow effect when buffering
    val infiniteTransition = rememberInfiniteTransition(label = "waveformGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val barColor = if (isBuffering) {
        WaveformActiveColor.copy(alpha = glowAlpha)
    } else {
        WaveformColor
    }

    val currentHistory = amplitudeHistory.toFloatArray()
    val currentOffset = scrollOffset.value

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val barWidthPx = 3.dp.toPx()
        val spacingPx = 1.dp.toPx()
        val minHeightPx = 2.dp.toPx()
        val totalBarWidth = barWidthPx + spacingPx

        val maxBars = (width / totalBarWidth).toInt()
        
        if (currentHistory.isEmpty()) {
            // Draw minimal idle bars
            var x = width - barWidthPx
            for (i in 0 until maxBars) {
                val y = (height - minHeightPx) / 2f
                drawRoundRect(
                    color = WaveformColor.copy(alpha = 0.3f),
                    topLeft = Offset(x, y),
                    size = Size(barWidthPx, minHeightPx),
                    cornerRadius = CornerRadius(barWidthPx / 2f, barWidthPx / 2f)
                )
                x -= totalBarWidth
                if (x < 0) break
            }
            return@Canvas
        }

        // Draw from right to left, newest data on the right
        val startIndex = if (currentHistory.size > maxBars) currentHistory.size - maxBars else 0
        var drawX = width - barWidthPx + currentOffset // sub-pixel scroll offset

        for (i in (currentHistory.size - 1) downTo startIndex) {
            val amp = currentHistory[i]
            val normalizedAmp = abs(amp).coerceIn(0f, 1f)
            val barHeightPx = (normalizedAmp * height * 0.9f).coerceAtLeast(minHeightPx)
            val y = (height - barHeightPx) / 2f

            // Fade bars near the left edge for smooth disappearance
            val fadeZone = width * 0.1f
            val alpha = if (drawX < fadeZone) (drawX / fadeZone).coerceIn(0f, 1f) else 1f

            drawRoundRect(
                color = barColor.copy(alpha = barColor.alpha * alpha),
                topLeft = Offset(drawX, y),
                size = Size(barWidthPx, barHeightPx),
                cornerRadius = CornerRadius(barWidthPx / 2f, barWidthPx / 2f)
            )

            drawX -= totalBarWidth
            if (drawX < -barWidthPx) break
        }
    }
}

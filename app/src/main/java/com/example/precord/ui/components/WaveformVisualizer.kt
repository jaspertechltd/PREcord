package com.example.precord.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
        // ⚡ Bolt Optimization:
        // Avoid allocating a new FloatArray via sliceArray on every draw frame.
        // Also avoid allocating an IntProgression via .reversed()
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

@Composable
fun AnimatedWaveformVisualizer(
    amplitudes: FloatArray,
    modifier: Modifier = Modifier,
    isBuffering: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveformGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val color = if (isBuffering) {
        WaveformActiveColor.copy(alpha = glowAlpha)
    } else {
        WaveformColor
    }

    WaveformVisualizer(
        amplitudes = amplitudes,
        modifier = modifier,
        color = color
    )
}

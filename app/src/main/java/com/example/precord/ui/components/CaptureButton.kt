package com.example.precord.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.example.precord.theme.PrecordPurple
import com.example.precord.theme.PrecordSurface
import com.example.precord.theme.PrecordYellow
import kotlinx.coroutines.launch

@Composable
fun CaptureButton(
    onClick: () -> Unit,
    isBuffering: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    // Pulse animation state on capture click (Yellow -> Purple over 1 second)
    val buttonColor = remember { Animatable(PrecordPurple) }
    val capturePulseScale = remember { Animatable(1f) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "buttonScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(100.dp)
                .scale(scale * capturePulseScale.value)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = {
                        if (enabled) {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            
                            // Trigger yellow -> purple pulse animation over 1s
                            scope.launch {
                                launch {
                                    buttonColor.snapTo(PrecordYellow)
                                    buttonColor.animateTo(PrecordPurple, animationSpec = tween(1000, easing = FastOutSlowInEasing))
                                }
                                launch {
                                    capturePulseScale.animateTo(1.25f, animationSpec = tween(300))
                                    capturePulseScale.animateTo(1.0f, animationSpec = tween(700))
                                }
                            }
                            
                            onClick()
                        }
                    }
                )
        ) {
            if (isBuffering) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = if (buttonColor.value == PrecordYellow) PrecordYellow else PrecordYellow.copy(alpha = pulseAlpha),
                        radius = (size.width / 2) * pulseScale,
                        style = Stroke(width = 4.dp.toPx())
                    )
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(if (enabled) buttonColor.value else PrecordPurple.copy(alpha = 0.5f))
            ) {
                Canvas(modifier = Modifier.size(24.dp)) {
                    drawRoundRect(
                        color = PrecordSurface,
                        size = Size(size.width, size.height),
                        cornerRadius = CornerRadius(6.dp.toPx())
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "CAPTURE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.state.VoiceStatus
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanLight
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisNavyCardBorder
import com.example.ui.theme.JarvisSchoolRed
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactorVisualizer(
    status: VoiceStatus,
    amplitude: Float,
    isSchoolMode: Boolean,
    onClick: () -> Unit,
    animationsEnabled: Boolean = true,
    accentColor: Color = JarvisCyan,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arc_reactor_animation")

    // Rotation animation
    val animRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Pulse animation
    val animPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Secondary reverse rotation
    val animReverseRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reverse_rotation"
    )

    val rotation = if (animationsEnabled) animRotation else 0f
    val pulse = if (animationsEnabled) animPulse else 1f
    val reverseRotation = if (animationsEnabled) animReverseRotation else 0f

    val coreColor = when {
        isSchoolMode -> JarvisSchoolRed
        status == VoiceStatus.PROCESSING -> JarvisAmber
        status == VoiceStatus.LISTENING -> accentColor
        status == VoiceStatus.WAITING_FOR_WAKE_WORD -> accentColor
        status == VoiceStatus.SPEAKING -> JarvisGreen
        else -> JarvisNavyCardBorder
    }

    val glowAlpha = when {
        isSchoolMode -> 0.4f
        status == VoiceStatus.LISTENING -> 0.85f + (amplitude * 0.15f)
        status == VoiceStatus.WAITING_FOR_WAKE_WORD -> 0.55f * pulse
        status == VoiceStatus.PROCESSING -> 0.7f
        status == VoiceStatus.SPEAKING -> 0.8f
        else -> 0.15f
    }

    Box(
        modifier = modifier
            .size(190.dp)
            .testTag("arc_reactor_visualizer")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 95.dp),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(180.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2f) - 10f
            val dynamicScale = if (status == VoiceStatus.LISTENING) {
                1f + (amplitude * 0.22f)
            } else if (status == VoiceStatus.WAITING_FOR_WAKE_WORD) {
                pulse
            } else {
                1f
            }

            // 1. Outer Glow Aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreColor.copy(alpha = glowAlpha * 0.5f),
                        coreColor.copy(alpha = glowAlpha * 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.15f * dynamicScale
                ),
                radius = baseRadius * 1.15f * dynamicScale,
                center = center
            )

            // 2. Outer Segmented Ring
            val outerRadius = baseRadius * 0.92f
            drawCircle(
                color = coreColor.copy(alpha = if (isSchoolMode) 0.3f else 0.4f),
                radius = outerRadius,
                center = center,
                style = Stroke(
                    width = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f), rotation)
                )
            )

            // 3. Middle Ring with Counter-Rotation
            val middleRadius = baseRadius * 0.74f
            drawCircle(
                color = coreColor.copy(alpha = if (isSchoolMode) 0.25f else 0.5f),
                radius = middleRadius,
                center = center,
                style = Stroke(
                    width = 3.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 14f), reverseRotation)
                )
            )

            // 4. Arc Reactor Power Rays (12 spokes)
            val spokeCount = 12
            val innerRingRadius = baseRadius * 0.52f
            for (i in 0 until spokeCount) {
                val angleRad = Math.toRadians((i * (360.0 / spokeCount) + rotation)).toFloat()
                val startX = center.x + cos(angleRad) * (innerRingRadius + 4f)
                val startY = center.y + sin(angleRad) * (innerRingRadius + 4f)
                val endX = center.x + cos(angleRad) * (middleRadius - 4f)
                val endY = center.y + sin(angleRad) * (middleRadius - 4f)

                drawLine(
                    color = coreColor.copy(alpha = if (isSchoolMode) 0.2f else 0.6f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2.5f
                )
            }

            // 5. Inner Core Ring
            drawCircle(
                color = coreColor.copy(alpha = 0.8f),
                radius = innerRingRadius,
                center = center,
                style = Stroke(width = 3f)
            )

            // 6. Central Arc Reactor Core
            val coreRadius = baseRadius * 0.38f * (if (status == VoiceStatus.LISTENING) (1f + amplitude * 0.15f) else 1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (status == VoiceStatus.DISABLED) 0.3f else 0.95f),
                        coreColor.copy(alpha = if (status == VoiceStatus.DISABLED) 0.2f else 0.9f),
                        coreColor.copy(alpha = if (status == VoiceStatus.DISABLED) 0.1f else 0.4f)
                    ),
                    center = center,
                    radius = coreRadius
                ),
                radius = coreRadius,
                center = center
            )
        }
    }
}

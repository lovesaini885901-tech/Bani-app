package com.example.ui.components

import androidx.compose.animation.core.Animatable
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
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CoralAlarm
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.CyanSecondary
import com.example.ui.theme.EmeraldActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VoiceOrb(
    isServiceActive: Boolean,
    isListening: Boolean,
    isAlarmRinging: Boolean,
    audioLevel: Float,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_rotation")

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // Smooth reactive audio level
    val animatedAudioLevel = remember { Animatable(0f) }
    LaunchedEffect(audioLevel) {
        animatedAudioLevel.animateTo(
            targetValue = audioLevel,
            animationSpec = tween(durationMillis = 100, easing = LinearEasing)
        )
    }

    val primaryColor = when {
        isAlarmRinging -> CoralAlarm
        isListening -> CyanPrimary
        isServiceActive -> EmeraldActive
        else -> Color(0xFF64748B)
    }

    val secondaryColor = when {
        isAlarmRinging -> AmberAccent
        isListening -> CyanSecondary
        isServiceActive -> Color(0xFF0284C7)
        else -> Color(0xFF334155)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.minDimension / 2f * 0.72f
            val audioBoost = animatedAudioLevel.value * 28f
            val dynamicRadius = (baseRadius * breathingScale) + audioBoost

            // 1. Ambient outer glow circles
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = if (isListening || isAlarmRinging) 0.35f else 0.12f),
                        secondaryColor.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = dynamicRadius * 1.45f
                ),
                radius = dynamicRadius * 1.45f,
                center = center
            )

            // 2. Outer revolving arc ring
            val sweep = if (isListening) 90f else 60f
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(primaryColor, secondaryColor, Color.Transparent),
                    center = center
                ),
                startAngle = rotationAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(center.x - dynamicRadius * 1.15f, center.y - dynamicRadius * 1.15f),
                size = androidx.compose.ui.geometry.Size(dynamicRadius * 2.3f, dynamicRadius * 2.3f),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(secondaryColor, primaryColor, Color.Transparent),
                    center = center
                ),
                startAngle = rotationAngle + 180f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(center.x - dynamicRadius * 1.15f, center.y - dynamicRadius * 1.15f),
                size = androidx.compose.ui.geometry.Size(dynamicRadius * 2.3f, dynamicRadius * 2.3f),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // 3. Central Core Sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.95f),
                        secondaryColor.copy(alpha = 0.8f),
                        Color(0xFF091426)
                    ),
                    center = Offset(center.x - dynamicRadius * 0.25f, center.y - dynamicRadius * 0.25f),
                    radius = dynamicRadius
                ),
                radius = dynamicRadius * 0.75f,
                center = center
            )

            // 4. Acoustic waveform spikes around core when listening
            val barCount = 24
            for (i in 0 until barCount) {
                val angleRad = (i * (360.0 / barCount) + rotationAngle * 0.5) * (PI / 180.0)
                val barHeight = if (isListening || isAlarmRinging) {
                    val wave = sin(i * 1.2 + rotationAngle * 0.1).toFloat()
                    (8.dp.toPx() + (wave * 6.dp.toPx()) + (animatedAudioLevel.value * 24.dp.toPx())).coerceAtLeast(3f)
                } else {
                    4.dp.toPx()
                }

                val startR = dynamicRadius * 0.82f
                val endR = startR + barHeight

                val startX = (center.x + cos(angleRad) * startR).toFloat()
                val startY = (center.y + sin(angleRad) * startR).toFloat()
                val endX = (center.x + cos(angleRad) * endR).toFloat()
                val endY = (center.y + sin(angleRad) * endR).toFloat()

                drawLine(
                    color = if (i % 2 == 0) primaryColor else secondaryColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 5. Inner Core Specular Highlight
            drawCircle(
                color = Color.White.copy(alpha = if (isListening) 0.8f else 0.4f),
                radius = dynamicRadius * 0.16f,
                center = Offset(center.x - dynamicRadius * 0.2f, center.y - dynamicRadius * 0.2f)
            )
        }
    }
}

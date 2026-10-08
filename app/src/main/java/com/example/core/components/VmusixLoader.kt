package com.example.core.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Modern minimalist circular loader.
 * Requirements strictly met:
 * - Pure animation only
 * - No text
 * - No logo
 * - No percentage
 * - Modern 60fps ring animation
 * - Colors dynamically follow the app theme
 */
@Composable
fun VmusixLoader(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    strokeWidth: Dp = 3.5.dp
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val infiniteTransition = rememberInfiniteTransition(label = "vmusix_loader_transition")

    // Rotation 0 -> 360 deg
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vmusix_loader_rotation"
    )

    // Pulse sweep angle 45 -> 280 deg
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 40f,
        targetValue = 270f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vmusix_loader_sweep"
    )

    // Pulsing inner ring scale
    val innerScale by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vmusix_loader_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .testTag("vmusix_loader"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val canvasWidth = this.size.width
            val canvasHeight = this.size.height
            val centerOffset = Offset(canvasWidth / 2f, canvasHeight / 2f)

            // Background subtle track
            drawCircle(
                color = primaryColor.copy(alpha = 0.12f),
                radius = (canvasWidth - strokePx) / 2f,
                style = Stroke(width = strokePx * 0.7f)
            )

            // Outer primary animated spinning arc
            val gradient = Brush.sweepGradient(
                colors = listOf(
                    secondaryColor.copy(alpha = 0.2f),
                    primaryColor,
                    secondaryColor
                ),
                center = centerOffset
            )

            drawArc(
                brush = gradient,
                startAngle = rotation,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(strokePx / 2f, strokePx / 2f),
                size = Size(canvasWidth - strokePx, canvasHeight - strokePx),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Inner counter-rotating subtle orbit ring
            val innerSize = canvasWidth * innerScale
            val innerOffset = (canvasWidth - innerSize) / 2f
            drawArc(
                color = secondaryColor.copy(alpha = 0.85f),
                startAngle = -rotation * 1.5f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(innerOffset, innerOffset),
                size = Size(innerSize, innerSize),
                style = Stroke(width = strokePx * 0.65f, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun VmusixLoaderFullScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("vmusix_loader_fullscreen"),
        contentAlignment = Alignment.Center
    ) {
        VmusixLoader(size = 56.dp, strokeWidth = 4.dp)
    }
}

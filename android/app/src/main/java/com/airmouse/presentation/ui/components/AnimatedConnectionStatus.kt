// app/src/main/java/com/airmouse/ui/components/AnimatedConnectionStatus.kt
package com.airmouse.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedConnectionStatus(
    isConnected: Boolean,
    signalStrength: Int = 100,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val ripple by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        // Ripple effect
        if (isConnected) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2 * (0.5f + ripple * 0.5f)
                drawCircle(
                    color = Color(0xFF4CAF50).copy(alpha = 0.3f * (1 - ripple)),
                    radius = radius,
                    center = center
                )
            }
        }

        // Pulse ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            val ringRadius = size.minDimension / 2 * (0.8f + pulse * 0.1f)
            drawCircle(
                color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336),
                radius = ringRadius,
                center = center,
                style = Stroke(width = 3f)
            )
        }

        // Center icon
        Icon(
            imageVector = if (isConnected)
                androidx.compose.material.icons.Icons.Filled.Wifi
            else
                androidx.compose.material.icons.Icons.Filled.WifiOff,
            contentDescription = "Connection Status",
            modifier = Modifier.size(40.dp),
            tint = if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336)
        )

        // Signal strength bars
        if (isConnected && signalStrength > 0) {
            Row(
                modifier = Modifier.align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                repeat(4) { index ->
                    val height = 4.dp * (index + 1)
                    val alpha = if (index * 25 <= signalStrength) 1f else 0.3f
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(height)
                            .background(
                                Color(0xFF4CAF50).copy(alpha = alpha),
                                RoundedCornerShape(1.5.dp)
                            )
                    )
                }
            }
        }
    }
}
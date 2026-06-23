package com.airmouse.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedConnectionStatus(
    isConnected: Boolean,
    signalStrength: Int = 100,
    ping: Int = 0,
    modifier: Modifier = Modifier,
    showDetails: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "connection")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val ripple by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple"
    )

    val rotate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )

    Box(
        modifier = modifier.size(if (showDetails) 100.dp else 80.dp),
        contentAlignment = Alignment.Center
    ) {
        
        if (!isConnected) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val scanAngle = rotate % 360f
                drawArc(
                    color = Color(0xFF00BCD4).copy(alpha = 0.15f),
                    startAngle = scanAngle - 30f,
                    sweepAngle = 60f,
                    useCenter = true,
                    topLeft = Offset(0f, 0f),
                    size = size
                )
            }
        }

        
        if (isConnected) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2 * (0.5f + ripple * 0.5f)
                drawCircle(
                    color = Color(0xFF4CAF50).copy(alpha = 0.25f * (1 - ripple)),
                    radius = radius,
                    center = center
                )
                drawCircle(
                    color = Color(0xFF4CAF50).copy(alpha = 0.15f * (1 - ripple * 0.5f)),
                    radius = radius * 0.8f,
                    center = center
                )
            }
        }

        
        Canvas(modifier = Modifier.fillMaxSize()) {
            val ringRadius = size.minDimension / 2 * (0.85f + pulse * 0.05f)
            val ringColor = when {
                isConnected -> Color(0xFF4CAF50)
                ping > 0 && ping < 200 -> Color(0xFFFFC107)
                ping > 0 -> Color(0xFFF44336)
                else -> Color(0xFF96A0AE)
            }
            drawCircle(
                color = ringColor,
                radius = ringRadius,
                center = center,
                style = Stroke(width = if (isConnected) 3f else 2f)
            )
            
            
            drawCircle(
                color = ringColor.copy(alpha = 0.5f),
                radius = ringRadius * 0.7f,
                center = center,
                style = Stroke(width = 1.5f)
            )
        }

        
        Icon(
            imageVector = when {
                isConnected -> Icons.Filled.Wifi
                ping > 0 -> Icons.Filled.SignalCellularAlt
                else -> Icons.Filled.WifiOff
            },
            contentDescription = "Connection Status",
            modifier = Modifier.size(if (isConnected) 40.dp else 36.dp),
            tint = when {
                isConnected -> Color(0xFF4CAF50)
                ping in 1..99 -> Color(0xFFFFC107)
                ping > 0 -> Color(0xFFF44336)
                else -> Color(0xFF96A0AE)
            }
        )

        
        if (isConnected && signalStrength > 0 && showDetails) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                repeat(4) { index ->
                    val height = 6.dp * (index + 1)
                    val alpha = if (index * 25 <= signalStrength) 1f else 0.3f
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(height)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Color(0xFF4CAF50).copy(alpha = alpha)
                            )
                    )
                }
            }
        }

        
        if (showDetails && !isConnected && ping > 0) {
            Text(
                text = "${ping}ms",
                fontSize = 10.sp,
                color = when {
                    ping < 100 -> Color(0xFFFFC107)
                    else -> Color(0xFFF44336)
                },
                modifier = Modifier.align(Alignment.BottomCenter)
                    .padding(bottom = -20.dp)
            )
        }
    }
}

@Composable
fun ConnectionQualityIndicator(
    quality: ConnectionQuality,
    modifier: Modifier = Modifier
) {
    val color = when (quality.signalStrength) {
        ConnectionQuality.SignalStrength.EXCELLENT -> Color(0xFF4CAF50)
        ConnectionQuality.SignalStrength.GOOD -> Color(0xFF8BC34A)
        ConnectionQuality.SignalStrength.FAIR -> Color(0xFFFFC107)
        ConnectionQuality.SignalStrength.POOR -> Color(0xFFF44336)
        ConnectionQuality.SignalStrength.UNKNOWN -> Color(0xFF96A0AE)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        
        AnimatedConnectionStatus(
            isConnected = true,
            signalStrength = quality.level(),
            ping = quality.ping,
            modifier = Modifier.size(48.dp),
            showDetails = false
        )
        
        Column {
            Text(
                text = quality.description(),
                style = MaterialTheme.typography.bodySmall,
                color = color,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            )
            Text(
                text = "Latency: ${quality.ping}ms • Jitter: ${quality.jitter}ms",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (quality.packetLoss > 0) {
                Text(
                    text = "Packet Loss: ${(quality.packetLoss * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFF44336)
                )
            }
        }
    }
}

data class ConnectionQuality(
    val ping: Int,
    val jitter: Int,
    val packetLoss: Float,
    val signalStrength: SignalStrength
) {
    enum class SignalStrength { EXCELLENT, GOOD, FAIR, POOR, UNKNOWN }
    
    fun level(): Int = when (signalStrength) {
        SignalStrength.EXCELLENT -> 100
        SignalStrength.GOOD -> 75
        SignalStrength.FAIR -> 50
        SignalStrength.POOR -> 25
        SignalStrength.UNKNOWN -> 0
    }
    
    fun description(): String = when (signalStrength) {
        SignalStrength.EXCELLENT -> "Excellent Connection"
        SignalStrength.GOOD -> "Good Connection"
        SignalStrength.FAIR -> "Fair Connection"
        SignalStrength.POOR -> "Poor Connection"
        SignalStrength.UNKNOWN -> "Unknown"
    }
}

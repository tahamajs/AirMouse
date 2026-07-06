package com.airmouse.presentation.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

@Composable
fun SensorVisualizer(
    roll: Float,
    pitch: Float,
    yaw: Float,
    modifier: Modifier = Modifier,
    showLabels: Boolean = true,
    size: VisualizerSize = VisualizerSize.MEDIUM,
    isCalibrated: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hud_pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    val animatedRoll = roll
    
    val animatedPitch = pitch
    
    val animatedYaw = yaw

    val visualizerSize = when (size) {
        VisualizerSize.SMALL -> 120.dp
        VisualizerSize.MEDIUM -> 200.dp
        VisualizerSize.LARGE -> 280.dp
    }

    Box(
        modifier = modifier
            .size(visualizerSize)
            .padding(16.dp)
    ) {
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = this.size.width
            val canvasHeight = this.size.height
            val centerX = canvasWidth / 2
            val centerY = canvasHeight / 2
            val phoneWidth = canvasWidth * 0.7f
            val phoneHeight = canvasHeight * 0.8f
            
            
            // 3D parallax offset for phone frame perspective
            val px = (animatedRoll / 45f) * 10f
            val py = (animatedPitch / 45f) * 10f

            // Background shadow (shifted oppositely)
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.35f),
                topLeft = Offset(centerX - phoneWidth/2 + 6 + px, centerY - phoneHeight/2 + 6 + py),
                size = Size(phoneWidth, phoneHeight),
                cornerRadius = CornerRadius(24f)
            )
            
            rotate(degrees = animatedYaw) {
                // Phone frame border
                drawRoundRect(
                    color = Color(0xFF1E222B),
                    topLeft = Offset(centerX - phoneWidth/2 - px, centerY - phoneHeight/2 - py),
                    size = Size(phoneWidth, phoneHeight),
                    cornerRadius = CornerRadius(24f)
                )
                
                // Cyan bezel highlight outline
                drawRoundRect(
                    color = Color(0xFF00E5FF).copy(alpha = 0.45f),
                    topLeft = Offset(centerX - phoneWidth/2 - px, centerY - phoneHeight/2 - py),
                    size = Size(phoneWidth, phoneHeight),
                    cornerRadius = CornerRadius(24f),
                    style = Stroke(width = 2.5f)
                )
                
                // Screen body
                drawRoundRect(
                    color = Color(0xFF0A0C10),
                    topLeft = Offset(centerX - phoneWidth/2 + 8 - px, centerY - phoneHeight/2 + 8 - py),
                    size = Size(phoneWidth - 16, phoneHeight - 16),
                    cornerRadius = CornerRadius(18f)
                )

                val screenRadius = (phoneWidth - 16) / 2f
                val activeCenterX = centerX - px
                val activeCenterY = centerY - py

                // Concentric Target Rings
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = 0.08f),
                    radius = screenRadius * 0.4f,
                    center = Offset(activeCenterX, activeCenterY),
                    style = Stroke(width = 1f)
                )
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = 0.06f),
                    radius = screenRadius * 0.75f,
                    center = Offset(activeCenterX, activeCenterY),
                    style = Stroke(width = 1f)
                )
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = 0.04f),
                    radius = screenRadius,
                    center = Offset(activeCenterX, activeCenterY),
                    style = Stroke(width = 1.5f)
                )

                // Coordinate Grid Axes
                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = Offset(activeCenterX, activeCenterY - screenRadius),
                    end = Offset(activeCenterX, activeCenterY + screenRadius),
                    strokeWidth = 1f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = Offset(activeCenterX - screenRadius, activeCenterY),
                    end = Offset(activeCenterX + screenRadius, activeCenterY),
                    strokeWidth = 1f
                )

                // Chevron Directional Intensity Calculation
                val maxRange = 25f
                val intensityUp = ((-animatedPitch) / maxRange).coerceIn(0f, 1f)
                val intensityDown = ((animatedPitch) / maxRange).coerceIn(0f, 1f)
                val intensityLeft = ((-animatedRoll) / maxRange).coerceIn(0f, 1f)
                val intensityRight = ((animatedRoll) / maxRange).coerceIn(0f, 1f)

                val chevronSize = 12f

                // UP Chevron
                val upY = activeCenterY - screenRadius * 0.75f
                val upColor = if (intensityUp > 0.05f) Color(0xFF00FFCC).copy(alpha = 0.2f + intensityUp * 0.8f) else Color.White.copy(alpha = 0.12f)
                val upWidth = if (intensityUp > 0.05f) 3.5f else 1.5f
                val upPath = Path().apply {
                    moveTo(activeCenterX - chevronSize, upY + chevronSize * 0.5f)
                    lineTo(activeCenterX, upY - chevronSize * 0.5f)
                    lineTo(activeCenterX + chevronSize, upY + chevronSize * 0.5f)
                }
                drawPath(path = upPath, color = upColor, style = Stroke(width = upWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                if (intensityUp > 0.1f) {
                    drawPath(path = upPath, color = Color(0xFF00FFCC).copy(alpha = intensityUp * 0.3f), style = Stroke(width = upWidth + 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }

                // DOWN Chevron
                val downY = activeCenterY + screenRadius * 0.75f
                val downColor = if (intensityDown > 0.05f) Color(0xFF00FFCC).copy(alpha = 0.2f + intensityDown * 0.8f) else Color.White.copy(alpha = 0.12f)
                val downWidth = if (intensityDown > 0.05f) 3.5f else 1.5f
                val downPath = Path().apply {
                    moveTo(activeCenterX - chevronSize, downY - chevronSize * 0.5f)
                    lineTo(activeCenterX, downY + chevronSize * 0.5f)
                    lineTo(activeCenterX + chevronSize, downY - chevronSize * 0.5f)
                }
                drawPath(path = downPath, color = downColor, style = Stroke(width = downWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                if (intensityDown > 0.1f) {
                    drawPath(path = downPath, color = Color(0xFF00FFCC).copy(alpha = intensityDown * 0.3f), style = Stroke(width = downWidth + 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }

                // LEFT Chevron
                val leftX = activeCenterX - screenRadius * 0.75f
                val leftColor = if (intensityLeft > 0.05f) Color(0xFF00FFCC).copy(alpha = 0.2f + intensityLeft * 0.8f) else Color.White.copy(alpha = 0.12f)
                val leftWidth = if (intensityLeft > 0.05f) 3.5f else 1.5f
                val leftPath = Path().apply {
                    moveTo(leftX + chevronSize * 0.5f, activeCenterY - chevronSize)
                    lineTo(leftX - chevronSize * 0.5f, activeCenterY)
                    lineTo(leftX + chevronSize * 0.5f, activeCenterY + chevronSize)
                }
                drawPath(path = leftPath, color = leftColor, style = Stroke(width = leftWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                if (intensityLeft > 0.1f) {
                    drawPath(path = leftPath, color = Color(0xFF00FFCC).copy(alpha = intensityLeft * 0.3f), style = Stroke(width = leftWidth + 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }

                // RIGHT Chevron
                val rightX = activeCenterX + screenRadius * 0.75f
                val rightColor = if (intensityRight > 0.05f) Color(0xFF00FFCC).copy(alpha = 0.2f + intensityRight * 0.8f) else Color.White.copy(alpha = 0.12f)
                val rightWidth = if (intensityRight > 0.05f) 3.5f else 1.5f
                val rightPath = Path().apply {
                    moveTo(rightX - chevronSize * 0.5f, activeCenterY - chevronSize)
                    lineTo(rightX + chevronSize * 0.5f, activeCenterY)
                    lineTo(rightX - chevronSize * 0.5f, activeCenterY + chevronSize)
                }
                drawPath(path = rightPath, color = rightColor, style = Stroke(width = rightWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                if (intensityRight > 0.1f) {
                    drawPath(path = rightPath, color = Color(0xFF00FFCC).copy(alpha = intensityRight * 0.3f), style = Stroke(width = rightWidth + 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }

                // Neon Tracker Bubble Position
                val dotX = (activeCenterX + (animatedRoll / 45f) * screenRadius).coerceIn(activeCenterX - screenRadius + 14f, activeCenterX + screenRadius - 14f)
                val dotY = (activeCenterY + (animatedPitch / 45f) * screenRadius).coerceIn(activeCenterY - screenRadius + 14f, activeCenterY + screenRadius - 14f)

                // Ripple Pulse effect
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = pulseAlpha),
                    radius = pulseRadius * (screenRadius / 50f),
                    center = Offset(dotX, dotY),
                    style = Stroke(width = 1.5f)
                )

                // Neon Radial Glow
                val glowBrush = Brush.radialGradient(
                    colors = listOf(Color(0xFF00FFCC), Color(0xFF00E5FF).copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(dotX, dotY),
                    radius = 24.dp.toPx()
                )
                drawCircle(
                    brush = glowBrush,
                    radius = 24.dp.toPx(),
                    center = Offset(dotX, dotY)
                )

                // Inner Glow Core
                drawCircle(
                    color = Color(0xFF00FFCC),
                    radius = 8.dp.toPx(),
                    center = Offset(dotX, dotY)
                )

                // Core White Point
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = Offset(dotX, dotY)
                )
            }
        }

        
        if (showLabels) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SensorValueCard("Roll", animatedRoll, Color(0xFF00BCD4))
                    SensorValueCard("Pitch", animatedPitch, Color(0xFF4CAF50))
                    SensorValueCard("Yaw", animatedYaw, Color(0xFFFF9800))
                }
                if (!isCalibrated) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.28f))
                    ) {
                        Text(
                            "Not calibrated yet",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Color(0xFFFBBF24),
                            fontSize = 10.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SensorValueCard(label: String, value: Float, color: Color) {
    Card(
        modifier = Modifier
            .width(80.dp)
            .padding(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "${"%.1f".format(value)}°",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun GyroscopeVisualizer(
    x: Float,
    y: Float,
    z: Float,
    modifier: Modifier = Modifier
) {
    val maxRange = 30f
    
    Column(modifier = modifier.fillMaxWidth()) {
        GyroAxisIndicator("X", x, maxRange, Color(0xFFF44336))
        Spacer(modifier = Modifier.height(8.dp))
        GyroAxisIndicator("Y", y, maxRange, Color(0xFF4CAF50))
        Spacer(modifier = Modifier.height(8.dp))
        GyroAxisIndicator("Z", z, maxRange, Color(0xFF2196F3))
    }
}

@Composable
fun GyroAxisIndicator(label: String, value: Float, maxRange: Float, color: Color) {
    val normalizedValue = (value / maxRange).coerceIn(-1f, 1f)
    val progress = (normalizedValue + 1f) / 2f
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = color
            )
            Text(
                text = "${"%.2f".format(value)} rad/s",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
        
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("-$maxRange", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("0", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("+$maxRange", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

enum class VisualizerSize {
    SMALL, MEDIUM, LARGE
}

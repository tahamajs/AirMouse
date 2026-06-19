package com.airmouse.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
    size: VisualizerSize = VisualizerSize.MEDIUM
) {
    val animatedRoll by animateFloatAsState(
        targetValue = roll,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "roll"
    )
    
    val animatedPitch by animateFloatAsState(
        targetValue = pitch,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pitch"
    )
    
    val animatedYaw by animateFloatAsState(
        targetValue = yaw,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "yaw"
    )

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
        // 3D Phone representation
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = this.size.width
            val canvasHeight = this.size.height
            val centerX = canvasWidth / 2
            val centerY = canvasHeight / 2
            val phoneWidth = canvasWidth * 0.7f
            val phoneHeight = canvasHeight * 0.8f
            
            // Phone shadow
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.3f),
                topLeft = Offset(centerX - phoneWidth/2 + 4, centerY - phoneHeight/2 + 4),
                size = Size(phoneWidth, phoneHeight),
                cornerRadius = CornerRadius(20f)
            )
            
            // Apply 3D rotation
            rotate(degrees = animatedYaw) {
                // Phone body
                drawRoundRect(
                    color = Color(0xFF1A1D24),
                    topLeft = Offset(centerX - phoneWidth/2, centerY - phoneHeight/2),
                    size = Size(phoneWidth, phoneHeight),
                    cornerRadius = CornerRadius(20f)
                )
                
                // Border glow
                drawRoundRect(
                    color = Color(0xFF00BCD4).copy(alpha = 0.3f),
                    topLeft = Offset(centerX - phoneWidth/2, centerY - phoneHeight/2),
                    size = Size(phoneWidth, phoneHeight),
                    cornerRadius = CornerRadius(20f),
                    style = Stroke(width = 2f)
                )
                
                // Screen
                drawRoundRect(
                    color = Color(0xFF0F1115),
                    topLeft = Offset(centerX - phoneWidth/2 + 8, centerY - phoneHeight/2 + 8),
                    size = Size(phoneWidth - 16, phoneHeight - 50),
                    cornerRadius = CornerRadius(12f)
                )
                
                // Crosshair
                drawLine(
                    color = Color.White.copy(alpha = 0.2f),
                    start = Offset(centerX - 30f, centerY),
                    end = Offset(centerX + 30f, centerY),
                    strokeWidth = 1f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.2f),
                    start = Offset(centerX, centerY - 30f),
                    end = Offset(centerX, centerY + 30f),
                    strokeWidth = 1f
                )
                
                // Orientation indicator dot
                val dotX = centerX + (animatedRoll / 45f) * (phoneWidth / 3)
                val dotY = centerY + (animatedPitch / 45f) * (phoneHeight / 3)
                
                drawCircle(
                    color = Color(0xFF4CAF50),
                    radius = 8f,
                    center = Offset(
                        dotX.coerceIn(centerX - phoneWidth/3, centerX + phoneWidth/3),
                        dotY.coerceIn(centerY - phoneHeight/3, centerY + phoneHeight/3)
                    )
                )
                
                // Dot glow
                drawCircle(
                    color = Color(0xFF4CAF50).copy(alpha = 0.3f),
                    radius = 14f,
                    center = Offset(
                        dotX.coerceIn(centerX - phoneWidth/3, centerX + phoneWidth/3),
                        dotY.coerceIn(centerY - phoneHeight/3, centerY + phoneHeight/3)
                    )
                )
            }
        }

        // Labels
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
        
        // Indicator arrow
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

// app/src/main/java/com/airmouse/ui/components/SensorVisualizer.kt
package com.airmouse.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.*

@Composable
fun SensorVisualizer(
    roll: Float,
    pitch: Float,
    yaw: Float,
    modifier: Modifier = Modifier
) {
    val animateRotation by animateFloatAsState(
        targetValue = yaw,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier
            .size(200.dp)
            .padding(16.dp)
    ) {
        // 3D Phone representation
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val width = size.width * 0.7f
            val height = size.height * 0.8f

            // Phone shadow
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.3f),
                topLeft = Offset(centerX - width/2 + 4, centerY - height/2 + 4),
                size = androidx.compose.ui.geometry.Size(width, height),
                cornerRadius = CornerRadius(20f)
            )

            rotate(degrees = animateRotation) {
                // Phone body
                drawRoundRect(
                    color = Color(0xFF1A1D24),
                    topLeft = Offset(centerX - width/2, centerY - height/2),
                    size = androidx.compose.ui.geometry.Size(width, height),
                    cornerRadius = CornerRadius(20f)
                )

                // Screen (showing orientation)
                drawRoundRect(
                    color = Color(0xFF0F1115),
                    topLeft = Offset(centerX - width/2 + 8, centerY - height/2 + 8),
                    size = androidx.compose.ui.geometry.Size(width - 16, height - 50),
                    cornerRadius = CornerRadius(12f)
                )

                // Orientation indicator dot
                val dotX = centerX + (roll / 45f) * (width / 3)
                val dotY = centerY + (pitch / 45f) * (height / 3)

                drawCircle(
                    color = Color(0xFF4CAF50),
                    radius = 8f,
                    center = Offset(dotX.coerceIn(centerX - width/3, centerX + width/3), dotY.coerceIn(centerY - height/3, centerY + height/3))
                )
            }
        }

        // Labels
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SensorValue("Roll", roll)
                SensorValue("Pitch", pitch)
                SensorValue("Yaw", yaw)
            }
        }
    }
}

@Composable
fun SensorValue(label: String, value: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "${"%.1f".format(value)}°",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
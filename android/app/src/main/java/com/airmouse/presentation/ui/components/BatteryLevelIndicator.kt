package com.airmouse.presentation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.airmouse.presentation.ui.battery.getBatteryColor

@Composable
fun BatteryLevelIndicator(
    level: Int,
    isCharging: Boolean,
    size: Int = 80
) {
    val batteryColor = getBatteryColor(level)

    Canvas(
        modifier = Modifier.size(size.dp)
    ) {
        val strokeWidth = size * 0.1f
        val radius = size / 2f - strokeWidth / 2

        
        drawCircle(
            color = Color.White.copy(alpha = 0.1f),
            radius = radius,
            center = Offset(size / 2f, size / 2f),
            style = Stroke(width = strokeWidth)
        )

        
        val sweepAngle = 360f * (level / 100f)
        drawArc(
            color = batteryColor,
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        
        if (isCharging) {
            drawCircle(
                color = Color(0xFFFFC107),
                radius = radius * 0.3f,
                center = Offset(size / 2f, size / 2f)
            )
        }
    }
}
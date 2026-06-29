package com.airmouse.presentation.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import kotlin.math.*

@Composable
fun LineChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    animated: Boolean = true
) {
    val animatedData = if (animated) {
        data.mapIndexed { index, value ->
            animateFloatAsState(
                targetValue = value,
                animationSpec = tween(1000, delayMillis = (index * 50L).toInt()),
                label = "chart_$index"
            ).value
        }
    } else {
        data
    }
    
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        val width = size.width
        val height = size.height
        val stepX = width / (data.size - 1).coerceAtLeast(1)
        val maxValue = animatedData.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        
        
        val gridColor = Color.White.copy(alpha = 0.05f)
        for (i in 0..4) {
            val y = height * i / 4
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }
        
        
        val fillPath = Path().apply {
            moveTo(0f, height)
            animatedData.forEachIndexed { index, value ->
                val x = index * stepX
                val y = height - (value / maxValue) * height
                lineTo(x, y)
            }
            lineTo(width, height)
            close()
        }
        
        drawPath(
            path = fillPath,
            color = color.copy(alpha = 0.1f),
            style = Stroke(width = 0f)
        )
        
        
        val linePath = Path().apply {
            animatedData.forEachIndexed { index, value ->
                val x = index * stepX
                val y = height - (value / maxValue) * height
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        
        drawPath(
            path = linePath,
            color = color,
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )
        
        
        animatedData.forEachIndexed { index, value ->
            val x = index * stepX
            val y = height - (value / maxValue) * height
            drawCircle(
                color = color,
                radius = 4f,
                center = Offset(x, y)
            )
            drawCircle(
                color = color.copy(alpha = 0.3f),
                radius = 6f,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun DonutChart(
    percentage: Float,
    modifier: Modifier = Modifier,
    size: Int = 100,
    color: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "donut"
    )
    
    Box(
        modifier = modifier.size(size.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size / 6f
            
            
            drawArc(
                color = backgroundColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                size = androidx.compose.ui.geometry.Size(size.toFloat(), size.toFloat()),
                style = Stroke(width = strokeWidth)
            )
            
            
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animatedPercentage,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                size = androidx.compose.ui.geometry.Size(size.toFloat(), size.toFloat()),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        
        Text(
            text = "${(percentage * 100).toInt()}%",
            fontSize = (size / 4).sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = color
        )
    }
}

package com.airmouse.presentation.ui.sensor

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.*

@Composable
fun SensorCubeView(
    roll: Float,
    pitch: Float,
    yaw: Float,
    modifier: Modifier = Modifier,
    showAxes: Boolean = true,
    showGrid: Boolean = true,
    showTextures: Boolean = true,
    cubeColor: Color = Color(0xFFFF5722),
    backgroundColor: Color = Color(0xFF1A1A2E)
) {
    val animatedRoll by animateFloatAsState(
        targetValue = roll,
        animationSpec = tween(durationMillis = 50),
        label = "roll"
    )
    val animatedPitch by animateFloatAsState(
        targetValue = pitch,
        animationSpec = tween(durationMillis = 50),
        label = "pitch"
    )
    val animatedYaw by animateFloatAsState(
        targetValue = yaw,
        animationSpec = tween(durationMillis = 50),
        label = "yaw"
    )

    Canvas(modifier = modifier) {
        
        drawRect(color = backgroundColor)

        val cx = size.width / 2
        val cy = size.height / 2
        val scale = min(size.width, size.height) / 3.2f

        
        fun project(x: Float, y: Float, z: Float): Offset {
            var xx = x
            var yy = y
            var zz = z

            
            val cosX = cos(animatedRoll)
            val sinX = sin(animatedRoll)
            val y1 = yy * cosX - zz * sinX
            val z1 = yy * sinX + zz * cosX
            yy = y1
            zz = z1

            
            val cosY = cos(animatedPitch)
            val sinY = sin(animatedPitch)
            val x1 = xx * cosY + zz * sinY
            val z2 = -xx * sinY + zz * cosY
            xx = x1
            zz = z2

            
            val cosZ = cos(animatedYaw)
            val sinZ = sin(animatedYaw)
            val x2 = xx * cosZ - yy * sinZ
            val y2 = xx * sinZ + yy * cosZ
            xx = x2
            yy = y2

            
            val factor = 3.5f / (3.5f - zz)
            return Offset(cx + xx * scale * factor, cy + yy * scale * factor)
        }

        
        val vertices = listOf(
            Triple(-1f, -1f, -1f), Triple(1f, -1f, -1f), Triple(1f, -1f, 1f), Triple(-1f, -1f, 1f),
            Triple(-1f, 1f, -1f), Triple(1f, 1f, -1f), Triple(1f, 1f, 1f), Triple(-1f, 1f, 1f)
        )

        
        val faceColors = listOf(
            Color.Red.copy(alpha = 0.3f),   
            Color.Green.copy(alpha = 0.3f),  
            Color.Blue.copy(alpha = 0.3f),   
            Color.Yellow.copy(alpha = 0.3f), 
            Color.Cyan.copy(alpha = 0.3f),   
            Color.Magenta.copy(alpha = 0.3f) 
        )

        val proj = vertices.map { project(it.first, it.second, it.third) }

        
        val faces = listOf(
            listOf(0, 1, 2, 3) to faceColors[0],  
            listOf(4, 5, 6, 7) to faceColors[1],  
            listOf(0, 1, 5, 4) to faceColors[2],  
            listOf(2, 3, 7, 6) to faceColors[3],  
            listOf(1, 2, 6, 5) to faceColors[4],  
            listOf(0, 3, 7, 4) to faceColors[5]   
        )

        if (showTextures) {
            faces.forEach { (indices, color) ->
                val path = Path().apply {
                    moveTo(proj[indices[0]].x, proj[indices[0]].y)
                    lineTo(proj[indices[1]].x, proj[indices[1]].y)
                    lineTo(proj[indices[2]].x, proj[indices[2]].y)
                    lineTo(proj[indices[3]].x, proj[indices[3]].y)
                    close()
                }
                drawPath(path, color)
            }
        }

        
        val edges = listOf(
            0 to 1, 1 to 2, 2 to 3, 3 to 0,
            4 to 5, 5 to 6, 6 to 7, 7 to 4,
            0 to 4, 1 to 5, 2 to 6, 3 to 7
        )

        for ((i, j) in edges) {
            val z1 = vertices[i].third
            val z2 = vertices[j].third
            val avgZ = (z1 + z2) / 2
            val alpha = (1f - (avgZ + 1f) / 2f).coerceIn(0.3f, 1f)
            drawLine(
                color = cubeColor.copy(alpha = alpha),
                start = proj[i],
                end = proj[j],
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }

        
        proj.forEach { point ->
            drawCircle(
                color = cubeColor.copy(alpha = 0.5f),
                radius = 8f,
                center = point
            )
            drawCircle(
                color = Color.White,
                radius = 3f,
                center = point
            )
        }

        
        if (showAxes) {
            drawAxes(cx, cy, scale)
        }

        
        if (showGrid) {
            drawGrid(cx, cy, scale)
        }
    }
}

private fun DrawScope.drawAxes(cx: Float, cy: Float, scale: Float) {
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

    
    drawLine(
        color = Color.Red,
        start = Offset(cx - scale, cy),
        end = Offset(cx + scale, cy),
        strokeWidth = 2f,
        pathEffect = dashEffect
    )
    drawLine(
        color = Color.Red,
        start = Offset(cx + scale, cy),
        end = Offset(cx + scale + 10f, cy),
        strokeWidth = 3f
    )
    drawCircle(
        color = Color.Red,
        radius = 4f,
        center = Offset(cx + scale, cy)
    )

    
    drawLine(
        color = Color.Green,
        start = Offset(cx, cy - scale),
        end = Offset(cx, cy + scale),
        strokeWidth = 2f,
        pathEffect = dashEffect
    )
    drawLine(
        color = Color.Green,
        start = Offset(cx, cy - scale),
        end = Offset(cx, cy - scale - 10f),
        strokeWidth = 3f
    )
    drawCircle(
        color = Color.Green,
        radius = 4f,
        center = Offset(cx, cy - scale)
    )

    
    val zStart = Offset(cx - scale * 0.7f, cy + scale * 0.7f)
    val zEnd = Offset(cx + scale * 0.7f, cy - scale * 0.7f)
    drawLine(
        color = Color.Blue,
        start = zStart,
        end = zEnd,
        strokeWidth = 2f,
        pathEffect = dashEffect
    )
    drawLine(
        color = Color.Blue,
        start = zEnd,
        end = Offset(zEnd.x + 10f, zEnd.y - 10f),
        strokeWidth = 3f
    )
    drawCircle(
        color = Color.Blue,
        radius = 4f,
        center = zEnd
    )
}

private fun DrawScope.drawGrid(cx: Float, cy: Float, scale: Float) {
    val gridColor = Color.White.copy(alpha = 0.08f)
    val spacing = scale / 4
    val count = 9

    
    for (i in -count / 2..count / 2) {
        val offset = i * spacing
        drawLine(
            color = gridColor,
            start = Offset(cx + offset, cy - scale * 2f),
            end = Offset(cx + offset, cy + scale * 2f),
            strokeWidth = 1f
        )
        drawLine(
            color = gridColor,
            start = Offset(cx - scale * 2f, cy + offset),
            end = Offset(cx + scale * 2f, cy + offset),
            strokeWidth = 1f
        )
    }
}
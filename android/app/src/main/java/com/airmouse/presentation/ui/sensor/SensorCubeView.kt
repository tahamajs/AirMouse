package com.airmouse.presentation.ui.sensor

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.*

@Composable
fun SensorCubeView(
    roll: Float,
    pitch: Float,
    yaw: Float,
    modifier: Modifier = Modifier,
    showAxes: Boolean = true,
    showGrid: Boolean = true,
    cubeColor: Color = Color(0xFFFF5722),
    backgroundColor: Color = Color(0xFF1A1A2E)
) {
    val animatedRoll by animateFloatAsState(
        targetValue = roll,
        animationSpec = tween(durationMillis = 50)
    )
    val animatedPitch by animateFloatAsState(
        targetValue = pitch,
        animationSpec = tween(durationMillis = 50)
    )
    val animatedYaw by animateFloatAsState(
        targetValue = yaw,
        animationSpec = tween(durationMillis = 50)
    )

    Canvas(modifier = modifier) {
        drawRect(color = backgroundColor)

        val cx = size.width / 2
        val cy = size.height / 2
        val scale = min(size.width, size.height) / 3.2f

        // Project 3D point to 2D with full rotation
        fun project(x: Float, y: Float, z: Float): Offset {
            var xx = x
            var yy = y
            var zz = z

            // Roll (X rotation)
            val cosX = cos(animatedRoll)
            val sinX = sin(animatedRoll)
            val y1 = yy * cosX - zz * sinX
            val z1 = yy * sinX + zz * cosX
            yy = y1
            zz = z1

            // Pitch (Y rotation)
            val cosY = cos(animatedPitch)
            val sinY = sin(animatedPitch)
            val x1 = xx * cosY + zz * sinY
            val z2 = -xx * sinY + zz * cosY
            xx = x1
            zz = z2

            // Yaw (Z rotation)
            val cosZ = cos(animatedYaw)
            val sinZ = sin(animatedYaw)
            val x2 = xx * cosZ - yy * sinZ
            val y2 = xx * sinZ + yy * cosZ
            xx = x2
            yy = y2

            // Perspective projection
            val factor = 3.5f / (3.5f - zz)
            return Offset(cx + xx * scale * factor, cy + yy * scale * factor)
        }

        // Cube vertices
        val vertices = listOf(
            Triple(-1f, -1f, -1f), Triple(1f, -1f, -1f), Triple(1f, -1f, 1f), Triple(-1f, -1f, 1f),
            Triple(-1f, 1f, -1f), Triple(1f, 1f, -1f), Triple(1f, 1f, 1f), Triple(-1f, 1f, 1f)
        )

        // Edges with colors for each axis
        val edges = listOf(
            // Bottom face (X-Y plane)
            0 to 1, 1 to 2, 2 to 3, 3 to 0,
            // Top face
            4 to 5, 5 to 6, 6 to 7, 7 to 4,
            // Vertical edges
            0 to 4, 1 to 5, 2 to 6, 3 to 7
        )

        val proj = vertices.map { project(it.first, it.second, it.third) }

        // Draw edges with depth-based fading
        val centerZ = (vertices.map { it.third }.average()).toFloat()

        for ((i, j) in edges) {
            val z1 = vertices[i].third
            val z2 = vertices[j].third
            val avgZ = (z1 + z2) / 2
            val alpha = (1f - (avgZ + 1f) / 2f).coerceIn(0.3f, 1f)
            drawLine(cubeColor.copy(alpha = alpha), proj[i], proj[j], strokeWidth = 3f)
        }

        // Draw vertices
        proj.forEach { point ->
            drawCircle(color = cubeColor, radius = 5f, center = point)
        }

        // Draw axes if enabled
        if (showAxes) {
            drawAxes(cx, cy, scale)
        }

        // Draw grid if enabled
        if (showGrid) {
            drawGrid(cx, cy, scale)
        }
    }
}

private fun DrawScope.drawAxes(cx: Float, cy: Float, scale: Float) {
    // X-axis (Red)
    drawLine(
        color = Color.Red,
        start = Offset(cx - scale, cy),
        end = Offset(cx + scale, cy),
        strokeWidth = 2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
    )

    // Y-axis (Green)
    drawLine(
        color = Color.Green,
        start = Offset(cx, cy - scale),
        end = Offset(cx, cy + scale),
        strokeWidth = 2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
    )

    // Z-axis (Blue) - shown as diagonal
    drawLine(
        color = Color.Blue,
        start = Offset(cx - scale * 0.7f, cy + scale * 0.7f),
        end = Offset(cx + scale * 0.7f, cy - scale * 0.7f),
        strokeWidth = 2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
    )

    // Axis labels
    drawCircle(color = Color.Red, radius = 4f, center = Offset(cx + scale, cy))
    drawCircle(color = Color.Green, radius = 4f, center = Offset(cx, cy - scale))
    drawCircle(color = Color.Blue, radius = 4f, center = Offset(cx + scale * 0.7f, cy - scale * 0.7f))
}

private fun DrawScope.drawGrid(cx: Float, cy: Float, scale: Float) {
    val gridColor = Color.White.copy(alpha = 0.1f)
    val spacing = scale / 4

    for (i in -4..4) {
        val offset = i * spacing
        drawLine(gridColor, Offset(cx + offset, cy - scale), Offset(cx + offset, cy + scale), strokeWidth = 1f)
        drawLine(gridColor, Offset(cx - scale, cy + offset), Offset(cx + scale, cy + offset), strokeWidth = 1f)
    }
}package com.airmouse.presentation.ui.sensor

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.*

@Composable
fun SensorCubeView(
    roll: Float,
    pitch: Float,
    yaw: Float,
    modifier: Modifier = Modifier,
    showAxes: Boolean = true,
    showGrid: Boolean = true,
    cubeColor: Color = Color(0xFFFF5722),
    backgroundColor: Color = Color(0xFF1A1A2E)
) {
    val animatedRoll by animateFloatAsState(
        targetValue = roll,
        animationSpec = tween(durationMillis = 50)
    )
    val animatedPitch by animateFloatAsState(
        targetValue = pitch,
        animationSpec = tween(durationMillis = 50)
    )
    val animatedYaw by animateFloatAsState(
        targetValue = yaw,
        animationSpec = tween(durationMillis = 50)
    )

    Canvas(modifier = modifier) {
        drawRect(color = backgroundColor)

        val cx = size.width / 2
        val cy = size.height / 2
        val scale = min(size.width, size.height) / 3.2f

        // Project 3D point to 2D with full rotation
        fun project(x: Float, y: Float, z: Float): Offset {
            var xx = x
            var yy = y
            var zz = z

            // Roll (X rotation)
            val cosX = cos(animatedRoll)
            val sinX = sin(animatedRoll)
            val y1 = yy * cosX - zz * sinX
            val z1 = yy * sinX + zz * cosX
            yy = y1
            zz = z1

            // Pitch (Y rotation)
            val cosY = cos(animatedPitch)
            val sinY = sin(animatedPitch)
            val x1 = xx * cosY + zz * sinY
            val z2 = -xx * sinY + zz * cosY
            xx = x1
            zz = z2

            // Yaw (Z rotation)
            val cosZ = cos(animatedYaw)
            val sinZ = sin(animatedYaw)
            val x2 = xx * cosZ - yy * sinZ
            val y2 = xx * sinZ + yy * cosZ
            xx = x2
            yy = y2

            // Perspective projection
            val factor = 3.5f / (3.5f - zz)
            return Offset(cx + xx * scale * factor, cy + yy * scale * factor)
        }

        // Cube vertices
        val vertices = listOf(
            Triple(-1f, -1f, -1f), Triple(1f, -1f, -1f), Triple(1f, -1f, 1f), Triple(-1f, -1f, 1f),
            Triple(-1f, 1f, -1f), Triple(1f, 1f, -1f), Triple(1f, 1f, 1f), Triple(-1f, 1f, 1f)
        )

        // Edges with colors for each axis
        val edges = listOf(
            // Bottom face (X-Y plane)
            0 to 1, 1 to 2, 2 to 3, 3 to 0,
            // Top face
            4 to 5, 5 to 6, 6 to 7, 7 to 4,
            // Vertical edges
            0 to 4, 1 to 5, 2 to 6, 3 to 7
        )

        val proj = vertices.map { project(it.first, it.second, it.third) }

        // Draw edges with depth-based fading
        val centerZ = (vertices.map { it.third }.average()).toFloat()

        for ((i, j) in edges) {
            val z1 = vertices[i].third
            val z2 = vertices[j].third
            val avgZ = (z1 + z2) / 2
            val alpha = (1f - (avgZ + 1f) / 2f).coerceIn(0.3f, 1f)
            drawLine(cubeColor.copy(alpha = alpha), proj[i], proj[j], strokeWidth = 3f)
        }

        // Draw vertices
        proj.forEach { point ->
            drawCircle(color = cubeColor, radius = 5f, center = point)
        }

        // Draw axes if enabled
        if (showAxes) {
            drawAxes(cx, cy, scale)
        }

        // Draw grid if enabled
        if (showGrid) {
            drawGrid(cx, cy, scale)
        }
    }
}

private fun DrawScope.drawAxes(cx: Float, cy: Float, scale: Float) {
    // X-axis (Red)
    drawLine(
        color = Color.Red,
        start = Offset(cx - scale, cy),
        end = Offset(cx + scale, cy),
        strokeWidth = 2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
    )

    // Y-axis (Green)
    drawLine(
        color = Color.Green,
        start = Offset(cx, cy - scale),
        end = Offset(cx, cy + scale),
        strokeWidth = 2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
    )

    // Z-axis (Blue) - shown as diagonal
    drawLine(
        color = Color.Blue,
        start = Offset(cx - scale * 0.7f, cy + scale * 0.7f),
        end = Offset(cx + scale * 0.7f, cy - scale * 0.7f),
        strokeWidth = 2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
    )

    // Axis labels
    drawCircle(color = Color.Red, radius = 4f, center = Offset(cx + scale, cy))
    drawCircle(color = Color.Green, radius = 4f, center = Offset(cx, cy - scale))
    drawCircle(color = Color.Blue, radius = 4f, center = Offset(cx + scale * 0.7f, cy - scale * 0.7f))
}

private fun DrawScope.drawGrid(cx: Float, cy: Float, scale: Float) {
    val gridColor = Color.White.copy(alpha = 0.1f)
    val spacing = scale / 4

    for (i in -4..4) {
        val offset = i * spacing
        drawLine(gridColor, Offset(cx + offset, cy - scale), Offset(cx + offset, cy + scale), strokeWidth = 1f)
        drawLine(gridColor, Offset(cx - scale, cy + offset), Offset(cx + scale, cy + offset), strokeWidth = 1f)
    }
}
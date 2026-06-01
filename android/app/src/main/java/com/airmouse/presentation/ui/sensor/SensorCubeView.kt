package com.airmouse.presentation.ui.sensor

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.*

@Composable
fun SensorCubeView(roll: Float, pitch: Float, yaw: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        val scale = min(size.width, size.height) / 3.5f

        // Project a 3D point to 2D using rotation matrices (simplified)
        fun project(x: Float, y: Float, z: Float): Offset {
            // Rotate around Y (pitch), then X (roll), then Z (yaw)
            var xx = x
            var yy = y
            var zz = z
            // Y rotation
            val cosY = cos(pitch); val sinY = sin(pitch)
            val x1 = xx * cosY + zz * sinY
            val z1 = -xx * sinY + zz * cosY
            xx = x1; zz = z1
            // X rotation
            val cosX = cos(roll); val sinX = sin(roll)
            val y1 = yy * cosX - zz * sinX
            val z2 = yy * sinX + zz * cosX
            yy = y1; zz = z2
            // Z rotation
            val cosZ = cos(yaw); val sinZ = sin(yaw)
            val x2 = xx * cosZ - yy * sinZ
            val y2 = xx * sinZ + yy * cosZ
            xx = x2; yy = y2
            // Perspective
            val factor = 2f / (4f - zz)
            return Offset(cx + xx * scale * factor, cy + yy * scale * factor)
        }

        val vertices = listOf(
            Triple(-1f, -1f, -1f), Triple(1f, -1f, -1f), Triple(1f, -1f, 1f), Triple(-1f, -1f, 1f),
            Triple(-1f, 1f, -1f), Triple(1f, 1f, -1f), Triple(1f, 1f, 1f), Triple(-1f, 1f, 1f)
        )
        val edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 0, 4 to 5, 5 to 6, 6 to 7, 7 to 4, 0 to 4, 1 to 5, 2 to 6, 3 to 7)

        val proj = vertices.map { project(it.first, it.second, it.third) }
        for ((i, j) in edges) {
            drawLine(Color.White, proj[i], proj[j], strokeWidth = 3f)
        }
    }
}
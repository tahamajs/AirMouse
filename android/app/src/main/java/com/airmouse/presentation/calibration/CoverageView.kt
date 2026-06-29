package com.airmouse.presentation.calibration

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * Figure-8 calibration guide view.
 * Displays a Lissajous figure-8 pattern to guide the user
 * during magnetometer calibration.
 */
@Composable
fun CoverageView(
    modifier: Modifier = Modifier,
    guideColor: Color = Color.White.copy(alpha = 0.3f),
    dotColor: Color = Color.Cyan,
    strokeWidth: Float = 3f
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color(0xFF1A1A2E))
    ) {
        val width = size.width
        val height = size.height
        val cx = width / 2f
        val cy = height / 2f
        val rx = width * 0.35f
        val ry = height * 0.25f

        val path = Path()
        val points = 160
        for (i in 0..points) {
            val t = i.toFloat() / points * 2f * kotlin.math.PI.toFloat()
            val x = cx + rx * sin(t)
            val y = cy + ry * sin(2f * t)
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = guideColor,
            style = Stroke(width = strokeWidth)
        )

        drawCircle(
            color = dotColor,
            radius = 10f,
            center = Offset(cx, cy)
        )
    }
}

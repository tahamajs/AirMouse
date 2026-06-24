package com.airmouse.presentation.ui.sensor
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import kotlin.math.*

/**
 * A stunning, interactive 3D cube visualisation for Air Mouse sensor data.
 * Features holographic glass effect, dynamic glow, and responsive sensor-driven rotation.
 */
@Composable
fun SensorCubeView(
    roll: Float,           // Rotation around X-axis (degrees)
    pitch: Float,          // Rotation around Y-axis (degrees)
    yaw: Float,            // Rotation around Z-axis (degrees)
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true,
    colorPalette: SensorCubePalette = SensorCubePalette.ELECTRIC
) {
    // Smooth animation with spring physics for natural feel
    val animatedRoll by animateFloatAsState(
        targetValue = roll,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ), label = "roll"
    )
    val animatedPitch by animateFloatAsState(
        targetValue = pitch,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ), label = "pitch"
    )
    val animatedYaw by animateFloatAsState(
        targetValue = yaw,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ), label = "yaw"
    )

    // Auto-rotation animation when idle (for visual appeal)
    val autoRotate = rememberInfiniteTransition(label = "autoRotate")
    val autoAngle by autoRotate.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "autoAngle"
    )

    // Dynamic glow pulsing
    val glowPulse = rememberInfiniteTransition(label = "glowPulse")
    val glowIntensity by glowPulse.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "glowIntensity"
    )

    val screenWidth = LocalConfiguration.current.screenWidthDp
    val cubeSize = min(screenWidth * 0.8f, 400f).dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(cubeSize)
        ) {
            val cx = size.width / 2
            val cy = size.height / 2
            val scale = min(size.width, size.height) / 2.8f

            // Draw background glow effect
            drawGlowEffect(cx, cy, scale, glowIntensity, colorPalette)

            // Calculate rotation angles
            val rotX = Math.toRadians(animatedRoll.toDouble()).toFloat()
            val rotY = Math.toRadians(animatedPitch.toDouble()).toFloat()
            val rotZ = Math.toRadians(animatedYaw.toDouble()).toFloat()

            // Auto-rotation adds gentle idle animation
            val autoRad = if (!isInteractive) {
                Math.toRadians(autoAngle.toDouble()).toFloat()
            } else {
                0f
            }

            // 3D projection function with perspective
            fun project(x: Float, y: Float, z: Float): Offset {
                var xx = x
                var yy = y
                var zz = z

                // Roll (X-axis)
                val cosX = cos(rotX)
                val sinX = sin(rotX)
                val y1 = yy * cosX - zz * sinX
                val z1 = yy * sinX + zz * cosX
                yy = y1
                zz = z1

                // Pitch (Y-axis)
                val cosY = cos(rotY)
                val sinY = sin(rotY)
                val x1 = xx * cosY + zz * sinY
                val z2 = -xx * sinY + zz * cosY
                xx = x1
                zz = z2

                // Yaw (Z-axis) + auto-rotation
                val totalZ = rotZ + autoRad
                val cosZ = cos(totalZ)
                val sinZ = sin(totalZ)
                val x2 = xx * cosZ - yy * sinZ
                val y2 = xx * sinZ + yy * cosZ

                // Perspective projection
                val perspectiveDist = 4.5f
                val factor = perspectiveDist / (perspectiveDist - zz)
                return Offset(
                    cx + x2 * scale * factor,
                    cy + y2 * scale * factor
                )
            }

            // Define cube vertices (normalised coordinates)
            val vertices = listOf(
                Triple(-1f, -1f, -1f), Triple(1f, -1f, -1f),
                Triple(1f, -1f, 1f), Triple(-1f, -1f, 1f),
                Triple(-1f, 1f, -1f), Triple(1f, 1f, -1f),
                Triple(1f, 1f, 1f), Triple(-1f, 1f, 1f)
            )

            // Project vertices
            val proj = vertices.map { project(it.first, it.second, it.third) }

            // Define faces with vertex indices
            val faces = listOf(
                listOf(0, 1, 2, 3) to 0,  // Front (Z-)
                listOf(4, 5, 6, 7) to 1,  // Back (Z+)
                listOf(0, 1, 5, 4) to 2,  // Left (X-)
                listOf(2, 3, 7, 6) to 3,  // Right (X+)
                listOf(1, 2, 6, 5) to 4,  // Top (Y+)
                listOf(0, 3, 7, 4) to 5   // Bottom (Y-)
            )

            // Draw faces with glass effect
            val faceColors = colorPalette.faceColors

            faces.forEach { (indices, faceIndex) ->
                val zDepth = vertices.indices
                    .filter { it in indices }
                    .map { vertices[it].third }
                    .average()
                    .toFloat()

                val alpha = (0.5f + (zDepth + 1f) / 4f).coerceIn(0.3f, 0.9f)

                val path = Path().apply {
                    moveTo(proj[indices[0]].x, proj[indices[0]].y)
                    lineTo(proj[indices[1]].x, proj[indices[1]].y)
                    lineTo(proj[indices[2]].x, proj[indices[2]].y)
                    lineTo(proj[indices[3]].x, proj[indices[3]].y)
                    close()
                }

                // Glass-like fill with gradient
                val gradientColors = faceColors[faceIndex].let { base ->
                    listOf(
                        base.copy(alpha = 0.25f * alpha),
                        base.copy(alpha = 0.6f * alpha),
                        base.copy(alpha = 0.3f * alpha)
                    )
                }
                val shader = SweepGradientShader(
                    colors = gradientColors,
                    center = Offset(cx, cy)
                )
                drawPath(path, brush = ShaderBrush(shader))

                // Glass edge highlight (subtle border)
                drawPath(
                    path,
                    color = Color.White.copy(alpha = 0.08f * alpha),
                    style = Stroke(width = 1.5f)
                )
            }

            // Draw glowing edges with depth-based intensity
            val edges = listOf(
                0 to 1, 1 to 2, 2 to 3, 3 to 0,
                4 to 5, 5 to 6, 6 to 7, 7 to 4,
                0 to 4, 1 to 5, 2 to 6, 3 to 7
            )

            edges.forEach { (i, j) ->
                val avgZ = (vertices[i].third + vertices[j].third) / 2f
                val alpha = (0.4f + (avgZ + 1f) / 4f).coerceIn(0.3f, 0.9f)
                val edgeColor = colorPalette.edgeColor.copy(
                    alpha = alpha * (0.7f + 0.3f * glowIntensity)
                )

                drawLine(
                    color = edgeColor,
                    start = proj[i],
                    end = proj[j],
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )

                // Secondary glow line
                drawLine(
                    color = edgeColor.copy(alpha = 0.2f),
                    start = proj[i],
                    end = proj[j],
                    strokeWidth = 8f,
                    cap = StrokeCap.Round
                )
            }

            // Draw vertex nodes with glow
            proj.forEachIndexed { index, point ->
                val zDepth = vertices[index].third
                val alpha = (0.5f + (zDepth + 1f) / 4f).coerceIn(0.4f, 1f)
                val size = 6f + 2f * (1f - (zDepth + 1f) / 2f)

                // Outer glow
                drawCircle(
                    color = colorPalette.vertexGlow.copy(alpha = 0.3f * alpha * glowIntensity),
                    radius = size * 3f,
                    center = point
                )

                // Core
                drawCircle(
                    color = colorPalette.vertexColor.copy(alpha = alpha),
                    radius = size,
                    center = point
                )

                // Inner highlight
                drawCircle(
                    color = Color.White.copy(alpha = 0.4f * alpha),
                    radius = size * 0.4f,
                    center = point
                )
            }

            // Draw subtle 3D grid lines
            drawGridLines(cx, cy, scale, rotX, rotY, rotZ, autoRad, colorPalette)
        }
    }
}

/**
 * Draws a subtle 3D grid on the ground plane for depth perception.
 */
private fun DrawScope.drawGridLines(
    cx: Float,
    cy: Float,
    scale: Float,
    rotX: Float,
    rotY: Float,
    rotZ: Float,
    autoRad: Float,
    palette: SensorCubePalette
) {
    val gridSize = 5
    val spacing = scale * 0.15f
    val gridColor = palette.gridColor.copy(alpha = 0.15f)

    for (i in -gridSize..gridSize) {
        val x = i * spacing
        val y = -scale * 0.7f // Ground plane Z position

        fun projectGridPoint(x: Float, y: Float, z: Float): Offset {
            var xx = x
            var yy = y
            var zz = z

            val cosX = cos(rotX)
            val sinX = sin(rotX)
            val y1 = yy * cosX - zz * sinX
            val z1 = yy * sinX + zz * cosX
            yy = y1
            zz = z1

            val cosY = cos(rotY)
            val sinY = sin(rotY)
            val x1 = xx * cosY + zz * sinY
            val z2 = -xx * sinY + zz * cosY
            xx = x1
            zz = z2

            val totalZ = rotZ + autoRad
            val cosZ = cos(totalZ)
            val sinZ = sin(totalZ)
            val x2 = xx * cosZ - yy * sinZ
            val y2 = xx * sinZ + yy * cosZ

            val factor = 4.5f / (4.5f - zz)
            return Offset(cx + x2 * scale * factor, cy + y2 * scale * factor)
        }

        // Lines parallel to X axis
        val startX = projectGridPoint(-gridSize * spacing, y, 0f)
        val endX = projectGridPoint(gridSize * spacing, y, 0f)
        drawLine(gridColor, startX, endX, strokeWidth = 1f)

        // Lines parallel to Y axis
        val startY = projectGridPoint(0f, y, -gridSize * spacing)
        val endY = projectGridPoint(0f, y, gridSize * spacing)
        drawLine(gridColor, startY, endY, strokeWidth = 1f)
    }
}

/**
 * Draws a glowing background effect behind the cube.
 */
private fun DrawScope.drawGlowEffect(
    cx: Float,
    cy: Float,
    scale: Float,
    intensity: Float,
    palette: SensorCubePalette
) {
    val glowRadius = scale * 2.2f
    val glowColor = palette.glowColor.copy(alpha = 0.08f * intensity)

    drawCircle(
        color = glowColor,
        radius = glowRadius,
        center = Offset(cx, cy)
    )

    drawCircle(
        color = palette.glowColor.copy(alpha = 0.04f * intensity),
        radius = glowRadius * 0.6f,
        center = Offset(cx, cy)
    )

    for (i in 0..12) {
        val angle = i * (2 * PI / 12).toFloat()
        val ringRadius = glowRadius * (0.7f + 0.2f * sin(angle + intensity * 2f))
        val x = cx + ringRadius * cos(angle)
        val y = cy + ringRadius * sin(angle)

        drawCircle(
            color = palette.glowColor.copy(alpha = 0.03f * intensity),
            radius = 20f + 10f * sin(angle * 2f + intensity * 3f),
            center = Offset(x, y)
        )
    }
}

/**
 * Color palettes for the sensor cube.
 */
enum class SensorCubePalette(
    val faceColors: List<Color>,
    val edgeColor: Color,
    val vertexColor: Color,
    val vertexGlow: Color,
    val glowColor: Color,
    val gridColor: Color
) {
    ELECTRIC(
        faceColors = listOf(
            Color(0xFF00D4FF), // Cyan
            Color(0xFF7B2FBE), // Purple
            Color(0xFF00E676), // Green
            Color(0xFFFF6B6B), // Red
            Color(0xFFFFD93D), // Yellow
            Color(0xFFFF6B9D)  // Pink
        ),
        edgeColor = Color(0xFF00D4FF),
        vertexColor = Color(0xFFFFFFFF),
        vertexGlow = Color(0xFF00D4FF),
        glowColor = Color(0xFF00D4FF),
        gridColor = Color(0xFF00D4FF)
    ),
    NEON(
        faceColors = listOf(
            Color(0xFFFF0050), // Hot Pink
            Color(0xFF00F5FF), // Cyan
            Color(0xFFFFD700), // Gold
            Color(0xFF00FF87), // Mint
            Color(0xFF7B2FBE), // Purple
            Color(0xFFFF6B6B)  // Coral
        ),
        edgeColor = Color(0xFFFF0050),
        vertexColor = Color(0xFFFFFFFF),
        vertexGlow = Color(0xFFFF0050),
        glowColor = Color(0xFFFF0050),
        gridColor = Color(0xFFFF0050)
    ),
    OCEAN(
        faceColors = listOf(
            Color(0xFF006994), // Deep Blue
            Color(0xFF00B4D8), // Sky Blue
            Color(0xFF48CAE4), // Light Blue
            Color(0xFF90E0EF), // Pale Blue
            Color(0xFF0077B6), // Medium Blue
            Color(0xFF023E8A)  // Navy
        ),
        edgeColor = Color(0xFF48CAE4),
        vertexColor = Color(0xFFFFFFFF),
        vertexGlow = Color(0xFF00B4D8),
        glowColor = Color(0xFF00B4D8),
        gridColor = Color(0xFF48CAE4)
    ),
    SUNSET(
        faceColors = listOf(
            Color(0xFFFF6B35), // Orange
            Color(0xFFF7931E), // Yellow-Orange
            Color(0xFFFFD700), // Gold
            Color(0xFFFF0050), // Hot Pink
            Color(0xFF7B2FBE), // Purple
            Color(0xFFFF6B6B)  // Coral
        ),
        edgeColor = Color(0xFFFF6B35),
        vertexColor = Color(0xFFFFFFFF),
        vertexGlow = Color(0xFFFF6B35),
        glowColor = Color(0xFFFF6B35),
        gridColor = Color(0xFFFF6B35)
    ),
    MONOCHROME(
        faceColors = listOf(
            Color(0xFF1A1A2E), Color(0xFF16213E),
            Color(0xFF0F3460), Color(0xFF1A1A2E),
            Color(0xFF16213E), Color(0xFF0F3460)
        ),
        edgeColor = Color(0xFF4A9EFF),
        vertexColor = Color(0xFFFFFFFF),
        vertexGlow = Color(0xFF4A9EFF),
        glowColor = Color(0xFF4A9EFF),
        gridColor = Color(0xFF4A9EFF)
    )
}

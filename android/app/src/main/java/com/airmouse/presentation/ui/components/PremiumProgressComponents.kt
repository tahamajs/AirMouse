package com.airmouse.presentation.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.cos
import kotlin.math.sin

/**
 * A premium circular progress indicator featuring a sweep gradient ring and outer glow shadows.
 * Supports both determinate (when [progress] is provided) and indeterminate states.
 */
@Composable
fun SweepGradientGlowRing(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    size: Dp = 64.dp,
    strokeWidth: Dp = 6.dp,
    glowRadius: Dp = 8.dp,
    colors: List<Color> = listOf(
        Color(0xFF6366F1), // Neon Indigo
        Color(0xFFEC4899), // Neon Pink
        Color(0xFF3B82F6), // Neon Blue
        Color(0xFF6366F1)  // Neon Indigo loop
    )
) {
    val infiniteTransition = rememberInfiniteTransition(label = "SweepRingTransition")

    // Rotation animation for indeterminate state
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepRotation"
    )

    // Pulse animation for ambient glow intensity
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowPulse"
    )

    val animatedProgress = progress?.let {
        animateFloatAsState(
            targetValue = it.coerceIn(0f, 1f),
            animationSpec = tween(500, easing = FastOutSlowInEasing),
            label = "DeterminateProgress"
        ).value
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // 1. Frosted glass-like background circle
        Box(
            modifier = Modifier
                .fillMaxSize(0.85f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.02f)
                        )
                    ),
                    shape = CircleShape
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // 2. Ambient neon glow layer (blurred)
        val glowModifier = if (progress == null) {
            Modifier
                .fillMaxSize(0.95f)
                .rotate(rotation)
                .blur(glowRadius)
        } else {
            Modifier
                .fillMaxSize(0.95f)
                .blur(glowRadius)
        }

        Canvas(modifier = glowModifier) {
            val strokePx = strokeWidth.toPx()
            val radius = (size.toPx() - strokePx) / 2f
            
            drawCircle(
                brush = Brush.sweepGradient(colors = colors),
                radius = radius,
                style = Stroke(width = strokePx * 1.5f, cap = StrokeCap.Round),
                alpha = glowPulse * 0.7f
            )
        }

        // 3. Crisp foreground ring
        val ringModifier = if (progress == null) {
            Modifier
                .fillMaxSize(0.9f)
                .rotate(rotation)
        } else {
            Modifier.fillMaxSize(0.9f)
        }

        Canvas(modifier = ringModifier) {
            val strokePx = strokeWidth.toPx()
            val pad = strokePx / 2f
            val arcSize = Size(size.toPx() * 0.9f - strokePx, size.toPx() * 0.9f - strokePx)
            val offset = Offset(
                (size.toPx() - (size.toPx() * 0.9f - strokePx)) / 2f,
                (size.toPx() - (size.toPx() * 0.9f - strokePx)) / 2f
            )

            if (animatedProgress != null) {
                // Determinate track
                drawArc(
                    color = Color.White.copy(alpha = 0.08f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = offset,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )

                // Determinate progress arc
                drawArc(
                    brush = Brush.sweepGradient(colors = colors),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = offset,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            } else {
                // Indeterminate full ring rotation
                drawArc(
                    brush = Brush.sweepGradient(colors = colors),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = offset,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
    }
}

/**
 * A highly premium, sci-fi styled "orbiting planet" loading animation.
 * Features a glowing pulsing neon central core and planetary particles spinning in orbit.
 */
@Composable
fun PulsingNeonOrbit(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    centerColor: Color = Color(0xFF6366F1), // Neon Indigo
    orbitColor: Color = Color(0xFF10B981), // Emerald Neon
    particleColor: Color = Color(0xFF00E5FF) // Cyan Neon
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbitTransition")

    // Rotation angle of orbital planet particles
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbitAngle"
    )

    // Breathing pulse for center sphere
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CenterScale"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // 1. Ambient radial center glow (blurred)
        Box(
            modifier = Modifier
                .size(size * 0.4f * scalePulse)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            centerColor.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
                .blur(10.dp)
        )

        // 2. Premium frosted glass center sphere
        Box(
            modifier = Modifier
                .size(size * 0.35f * scalePulse)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            centerColor.copy(alpha = 0.9f),
                            centerColor.copy(alpha = 0.4f)
                        )
                    ),
                    shape = CircleShape
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.4f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = CircleShape
                )
        )

        // 3. Orbital path and spinning planetary objects
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = this.center
            val radius = (size.toPx() / 2f) - 12.dp.toPx()

            // Draw orbital trace
            drawCircle(
                color = Color.White.copy(alpha = 0.07f),
                radius = radius,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Calculate planetary coordinate (Particle 1)
            val rad1 = Math.toRadians(angle.toDouble())
            val px1 = center.x + radius * cos(rad1).toFloat()
            val py1 = center.y + radius * sin(rad1).toFloat()

            // Glow for particle 1
            drawCircle(
                color = particleColor.copy(alpha = 0.35f),
                radius = 10.dp.toPx(),
                center = Offset(px1, py1)
            )

            // Inner particle 1
            drawCircle(
                color = particleColor,
                radius = 5.dp.toPx(),
                center = Offset(px1, py1)
            )

            // Particle 2 (offset by 180 degrees, smaller, orbiting slightly closer)
            val rad2 = Math.toRadians((angle + 180f).toDouble())
            val p2Radius = radius * 0.8f
            val px2 = center.x + p2Radius * cos(rad2).toFloat()
            val py2 = center.y + p2Radius * sin(rad2).toFloat()

            // Glow for particle 2
            drawCircle(
                color = orbitColor.copy(alpha = 0.3f),
                radius = 8.dp.toPx(),
                center = Offset(px2, py2)
            )

            // Inner particle 2
            drawCircle(
                color = orbitColor,
                radius = 3.5.dp.toPx(),
                center = Offset(px2, py2)
            )
        }
    }
}

/**
 * A premium glassmorphic linear progress bar.
 * Features a frosted-glass channel, a neon gradient filled bar, an animated light shimmer,
 * and a glowing leading edge.
 */
@Composable
fun GlassmorphicNeonProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
    glowColor: Color = Color(0xFF6366F1),
    colors: List<Color> = listOf(
        Color(0xFF6366F1), // Indigo
        Color(0xFFEC4899), // Pink
        Color(0xFF3B82F6)  // Blue
    )
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ShimmerTransition")
    
    // Animate the light shimmer translation
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -500f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerOffset"
    )

    // Smooth progress fill changes
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "ProgressBarProgress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.02f)
                    )
                ),
                shape = RoundedCornerShape(height / 2)
            )
            .border(
                width = 0.8.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(height / 2)
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        if (animatedProgress > 0.01f) {
            // 1. Progress glow backdrop (blurred glow)
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.horizontalGradient(colors = colors),
                        shape = RoundedCornerShape(height / 2)
                    )
                    .blur(4.dp)
            )

            // 2. Sharp progress fill bar with shimmer overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.horizontalGradient(colors = colors),
                        shape = RoundedCornerShape(height / 2)
                    )
            ) {
                // Shimmer Overlay effect
                val shimmerBrush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0f),
                        Color.White.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0f)
                    ),
                    start = Offset(shimmerOffset, 0f),
                    end = Offset(shimmerOffset + 180f, 0f)
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(shimmerBrush)
                )
            }

            // 3. Glowing leading edge indicator dot
            if (animatedProgress < 0.99f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(height),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(
                        modifier = Modifier
                            .offset(x = (height / 4))
                            .size(height * 1.4f)
                            .background(Color.White, shape = CircleShape)
                            .border(1.dp, glowColor, CircleShape)
                            .blur(1.5.dp)
                    )
                }
            }
        }
    }
}

/**
 * A full screen glassmorphic loading overlay.
 * Blurs the background with frosted glass texture and displays a premium glow loader.
 */
@Composable
fun GlassmorphicLoadingOverlay(
    message: String = "Loading...",
    subMessage: String? = null,
    onDismiss: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1115).copy(alpha = 0.7f)) // Frosted-glass backdrop dark tint
            .blur(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            PulsingNeonOrbit(size = 96.dp)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = message,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            if (subMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subMessage,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * A premium connecting dialog utilizing glassmorphism and the [SweepGradientGlowRing].
 */
@Composable
fun GlassmorphicConnectingDialog(
    title: String = "Connecting to Server",
    statusText: String = "Initializing handshake...",
    progress: Float,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier
                .width(320.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E222B).copy(alpha = 0.85f),
                            Color(0xFF12151C).copy(alpha = 0.95f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.02f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                SweepGradientGlowRing(
                    progress = progress,
                    size = 80.dp,
                    strokeWidth = 5.dp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF6366F1)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = statusText,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.08f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

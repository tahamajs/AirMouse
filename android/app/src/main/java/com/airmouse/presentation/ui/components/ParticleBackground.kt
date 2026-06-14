package com.airmouse.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun ParticleBackground(
    modifier: Modifier = Modifier,
    particleCount: Int = 50,
    particleColor: Color = Color(0xFF00BCD4).copy(alpha = 0.3f),
    interactive: Boolean = true
) {
    val particles = remember { mutableStateListOf<Particle>() }
    val infiniteTransition = rememberInfiniteTransition()
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    LaunchedEffect(particleCount) {
        particles.clear()
        repeat(particleCount) {
            particles.add(Particle.random())
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { particle ->
            particle.update(size.width, size.height, time)
            drawCircle(
                color = particleColor,
                radius = particle.radius,
                center = Offset(particle.x, particle.y),
                blendMode = BlendMode.Screen
            )
            
            // Draw glow
            drawCircle(
                color = particleColor.copy(alpha = 0.1f),
                radius = particle.radius * 2,
                center = Offset(particle.x, particle.y),
                blendMode = BlendMode.Screen
            )
        }
    }
}

data class Particle(
    var x: Float,
    var y: Float,
    var radius: Float,
    var speedX: Float,
    var speedY: Float,
    var oscillation: Float
) {
    fun update(width: Float, height: Float, time: Float) {
        x += speedX
        y += speedY
        x += sin(time + oscillation) * 0.5f
        y += cos(time + oscillation) * 0.5f
        
        if (x < -radius) x = width + radius
        if (x > width + radius) x = -radius
        if (y < -radius) y = height + radius
        if (y > height + radius) y = -radius
    }
    
    companion object {
        fun random(): Particle = Particle(
            x = Random.nextFloat() * 1000,
            y = Random.nextFloat() * 2000,
            radius = 1f + Random.nextFloat() * 3,
            speedX = -0.5f + Random.nextFloat() * 1f,
            speedY = -0.5f + Random.nextFloat() * 1f,
            oscillation = Random.nextFloat() * 360f
        )
    }
}
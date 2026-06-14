package com.airmouse.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun FloatingParticles(
    modifier: Modifier = Modifier,
    count: Int = 30,
    colors: List<Color> = listOf(
        Color(0xFF00BCD4),
        Color(0xFF4CAF50),
        Color(0xFFFF9800),
        Color(0xFFE91E63)
    )
) {
    val particles = remember { mutableStateListOf<FloatingParticle>() }
    
    LaunchedEffect(count) {
        particles.clear()
        repeat(count) {
            particles.add(FloatingParticle.random(colors))
        }
        
        while (true) {
            delay(16)
            particles.forEach { it.update() }
        }
    }
    
    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { particle ->
            drawCircle(
                color = particle.color,
                radius = particle.size,
                center = Offset(particle.x, particle.y),
                alpha = particle.alpha
            )
        }
    }
}

data class FloatingParticle(
    var x: Float,
    var y: Float,
    var size: Float,
    var speedX: Float,
    var speedY: Float,
    var color: Color,
    var alpha: Float,
    var life: Float
) {
    fun update() {
        x += speedX
        y += speedY
        life -= 0.01f
        alpha = life.coerceIn(0f, 1f)
        
        if (life <= 0) {
            reset()
        }
    }
    
    fun reset() {
        x = Random.nextFloat() * 1000
        y = Random.nextFloat() * 2000
        size = 2f + Random.nextFloat() * 4
        speedX = -0.3f + Random.nextFloat() * 0.6f
        speedY = -0.5f + Random.nextFloat() * 1f
        life = 0.5f + Random.nextFloat() * 0.5f
        alpha = life
    }
    
    companion object {
        fun random(colors: List<Color>): FloatingParticle = FloatingParticle(
            x = Random.nextFloat() * 1000,
            y = Random.nextFloat() * 2000,
            size = 1f + Random.nextFloat() * 3,
            speedX = -0.2f + Random.nextFloat() * 0.4f,
            speedY = -0.3f + Random.nextFloat() * 0.6f,
            color = colors.random(),
            alpha = 0.5f + Random.nextFloat() * 0.5f,
            life = 0.5f + Random.nextFloat() * 0.5f
        )
    }
}
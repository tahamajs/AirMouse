package com.airmouse.presentation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlin.random.Random


private data class Particle(
    var x: Float,
    var y: Float,
    val radius: Float,
    val speedX: Float,
    val speedY: Float,
    val alpha: Float
)

@Composable
fun ParticleBackground(
    particleCount: Int = 15,
    modifier: Modifier = Modifier,
    particleColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
) {
    
    val particles = remember { mutableStateListOf<Particle>() }

    
    LaunchedEffect(Unit) {
        while (isActive) {
            
            if (particles.isEmpty()) {
                repeat(particleCount) {
                    particles.add(
                        Particle(
                            x = Random.nextFloat(),
                            y = Random.nextFloat(),
                            radius = Random.nextFloat() * 6f + 4f, 
                            speedX = (Random.nextFloat() - 0.5f) * 0.002f,
                            speedY = (Random.nextFloat() - 0.5f) * 0.003f - 0.001f, 
                            alpha = Random.nextFloat() * 0.5f + 0.5f
                        )
                    )
                }
            } else {
                
                for (i in particles.indices) {
                    val p = particles[i]
                    var newX = p.x + p.speedX
                    var newY = p.y + p.speedY

                    
                    if (newX < 0f || newX > 1f || newY < 0f || newY > 1f) {
                        newX = Random.nextFloat()
                        newY = if (p.speedY < 0) 1f else 0f 
                    }

                    particles[i] = p.copy(x = newX, y = newY)
                }
            }
            
            delay(16L)
        }
    }

    
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        particles.forEach { particle ->
            drawCircle(
                color = particleColor.copy(alpha = particleColor.alpha * particle.alpha),
                radius = particle.radius,
                center = Offset(
                    x = particle.x * width,
                    y = particle.y * height
                )
            )
        }
    }
}
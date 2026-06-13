// app/src/main/java/com/airmouse/ui/components/NeonButton.kt
package com.airmouse.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun NeonButton(
    onClick: () -> Unit,
    icon: ImageVector? = null,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    glowing: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition()
    val glowIntensity by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val brush = if (glowing && enabled) {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF00BCD4).copy(alpha = 0.5f + glowIntensity * 0.3f),
                Color(0xFF4CAF50).copy(alpha = 0.5f + glowIntensity * 0.3f)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF1A1D24),
                Color(0xFF0F1115)
            )
        )
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(brush),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color(0xFF2B3341)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (glowing && enabled) (glowIntensity * 8).dp else 0.dp
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = text,
                    modifier = Modifier.size(24.dp),
                    tint = if (enabled) Color.White else Color(0xFF96A0AE)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) Color.White else Color(0xFF96A0AE)
            )
        }
    }
}// app/src/main/java/com/airmouse/ui/components/NeonButton.kt
package com.airmouse.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun NeonButton(
    onClick: () -> Unit,
    icon: ImageVector? = null,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    glowing: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition()
    val glowIntensity by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val brush = if (glowing && enabled) {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF00BCD4).copy(alpha = 0.5f + glowIntensity * 0.3f),
                Color(0xFF4CAF50).copy(alpha = 0.5f + glowIntensity * 0.3f)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF1A1D24),
                Color(0xFF0F1115)
            )
        )
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(brush),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color(0xFF2B3341)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (glowing && enabled) (glowIntensity * 8).dp else 0.dp
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = text,
                    modifier = Modifier.size(24.dp),
                    tint = if (enabled) Color.White else Color(0xFF96A0AE)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) Color.White else Color(0xFF96A0AE)
            )
        }
    }
}
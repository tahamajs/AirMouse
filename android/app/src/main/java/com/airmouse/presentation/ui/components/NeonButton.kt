package com.airmouse.presentation.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NeonButton(
    onClick: () -> Unit,
    icon: ImageVector? = null,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    glowing: Boolean = true,
    gradient: List<Color> = listOf(Color(0xFF00BCD4), Color(0xFF4CAF50)),
    size: ButtonSize = ButtonSize.MEDIUM
) {
    val infiniteTransition = rememberInfiniteTransition(label = "neon")
    val glowIntensity by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val buttonHeight = when (size) {
        ButtonSize.SMALL -> 40.dp
        ButtonSize.MEDIUM -> 56.dp
        ButtonSize.LARGE -> 64.dp
    }
    
    val fontSize = when (size) {
        ButtonSize.SMALL -> 12.sp
        ButtonSize.MEDIUM -> 16.sp
        ButtonSize.LARGE -> 18.sp
    }
    
    val iconSize = when (size) {
        ButtonSize.SMALL -> 18.dp
        ButtonSize.MEDIUM -> 24.dp
        ButtonSize.LARGE -> 28.dp
    }

    val brush = if (glowing && enabled) {
        Brush.horizontalGradient(gradient.map { it.copy(alpha = 0.5f + glowIntensity * 0.3f) })
    } else {
        Brush.horizontalGradient(gradient.map { it.copy(alpha = 0.6f) })
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(buttonHeight)
            .clip(RoundedCornerShape(buttonHeight / 2))
            .background(brush),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color(0xFF2B3341)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (glowing && enabled) (glowIntensity * 12).dp else 4.dp,
            pressedElevation = 2.dp
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = text,
                    modifier = Modifier.size(iconSize),
                    tint = if (enabled) Color.White else Color(0xFF96A0AE)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontSize = fontSize,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) Color.White else Color(0xFF96A0AE)
            )
        }
    }
}

@Composable
fun PulseButton(
    onClick: () -> Unit,
    icon: ImageVector? = null,
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .scale(scale),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(28.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 4.dp
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = text, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

enum class ButtonSize {
    SMALL, MEDIUM, LARGE
}

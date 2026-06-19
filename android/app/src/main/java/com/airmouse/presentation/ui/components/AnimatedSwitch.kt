package com.airmouse.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    description: String = ""
) {
    val animatedOffset by animateDpAsState(
        targetValue = if (checked) 28.dp else 4.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "switch"
    )
    
    val animatedColor by animateColorAsState(
        targetValue = if (checked) Color(0xFF4CAF50) else Color(0xFF96A0AE),
        animationSpec = tween(300),
        label = "color"
    )
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (description.isNotEmpty()) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(animatedColor)
        ) {
            Box(
                modifier = Modifier
                    .offset(x = animatedOffset)
                    .size(24.dp)
                    .align(Alignment.CenterStart)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

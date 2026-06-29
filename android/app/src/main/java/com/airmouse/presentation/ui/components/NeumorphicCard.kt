package com.airmouse.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NeumorphicCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0F1115)
    val lightColor = if (isDark) Color(0xFF2B3341) else Color.White
    val darkColor = if (isDark) Color(0xFF0F1115) else Color(0xFFE0E0E0)
    
    Card(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = darkColor,
                spotColor = lightColor
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1A1D24) else Color(0xFFF5F5F5)
        )
    ) {
        content()
    }
}

@Composable
fun NeumorphicButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0F1115)
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp,
        tonalElevation = 2.dp,
        color = if (isDark) Color(0xFF1A1D24) else Color(0xFFF5F5F5),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = text, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

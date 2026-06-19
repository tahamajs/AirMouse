package com.airmouse.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingActionMenu(
    items: List<FABMenuItem>,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box(modifier = modifier, contentAlignment = Alignment.BottomEnd) {
        // Menu items
        items.forEachIndexed { index, item ->
            val delayMillis = (index * 50L).toInt()
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + scaleIn(
                    initialScale = 0f,
                    animationSpec = tween(300, delayMillis = delayMillis)
                ) + slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300, delayMillis = delayMillis)
                ),
                exit = fadeOut() + scaleOut() + slideOutVertically()
            ) {
                FloatingActionButton(
                    onClick = {
                        onItemClick(item.id)
                        expanded = false
                    },
                    modifier = Modifier
                        .padding(bottom = ((items.size - index) * 70).dp)
                        .size(48.dp),
                    containerColor = item.color,
                    shape = CircleShape
                ) {
                    Icon(item.icon, contentDescription = item.label)
                }
            }
        }
        
        // Main FAB
        FloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor = if (expanded) Color(0xFFF44336) else MaterialTheme.colorScheme.primary,
            shape = CircleShape
        ) {
            Icon(
                if (expanded) Icons.Default.Close else Icons.Default.Add,
                contentDescription = if (expanded) "Close" else "Open"
            )
        }
    }
}

data class FABMenuItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val color: Color = Color(0xFF4F46E5)
)

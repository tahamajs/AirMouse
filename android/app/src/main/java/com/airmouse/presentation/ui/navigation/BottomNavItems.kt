package com.airmouse.presentation.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AirMouseBottomBar(
    currentRoute: String?,
    onItemSelected: (Destinations) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = Destinations.bottomNavItems
    val selectedIndex = Destinations.getBottomNavIndex(currentRoute)
    
    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        items.forEachIndexed { index, destination ->
            val selected = selectedIndex == index
            
            NavigationBarItem(
                selected = selected,
                onClick = { onItemSelected(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon ?: Icons.Default.Circle,
                        contentDescription = destination.title,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = destination.title,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF00BCD4),
                    selectedTextColor = Color(0xFF00BCD4),
                    unselectedIconColor = Color(0xFF96A0AE),
                    unselectedTextColor = Color(0xFF96A0AE),
                    indicatorColor = Color(0xFF00BCD4).copy(alpha = 0.2f)
                )
            )
        }
    }
}
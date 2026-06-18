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

/**
 * Modern animated bottom navigation bar.
 *
 * @param currentRoute The current navigation route (used to highlight the active item).
 * @param onItemSelected Callback when an item is tapped.
 * @param modifier Modifier for the bar.
 * @param items List of [BottomNavItem] to display. Defaults to [bottomNavItems].
 * @param showLabels Whether to show text labels under icons.
 * @param containerColor Background color of the bar.
 */
@Composable
fun AirMouseBottomBar(
    currentRoute: String?,
    onItemSelected: (Destinations) -> Unit,
    modifier: Modifier = Modifier,
    items: List<BottomNavItem> = bottomNavItems,
    showLabels: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surface
) {
    val selectedIndex = getSelectedBottomNavIndex(currentRoute)

    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        containerColor = containerColor,
        tonalElevation = 8.dp
    ) {
        items.forEachIndexed { index, item ->
            val selected = selectedIndex == index

            // Animate icon and text color on selection change
            val animatedColor by animateColorAsState(
                targetValue = if (selected) Color(0xFF00BCD4) else Color(0xFF96A0AE),
                animationSpec = tween(300),
                label = "navColor"
            )

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (item.enabled) {
                        onItemSelected(item.destination)
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.getIcon(selected),
                        contentDescription = item.title,
                        modifier = Modifier.size(24.dp),
                        tint = animatedColor
                    )
                },
                label = {
                    if (showLabels) {
                        Text(
                            text = item.title,
                            fontSize = 11.sp,
                            maxLines = 1,
                            color = animatedColor
                        )
                    }
                },
                badge = {
                    if (item.badgeCount > 0) {
                        Badge(
                            containerColor = Color(0xFFF44336),
                            modifier = Modifier.offset(x = 8.dp, y = (-8).dp)
                        ) {
                            Text(
                                text = if (item.badgeCount > 99) "99+" else item.badgeCount.toString(),
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF00BCD4),
                    selectedTextColor = Color(0xFF00BCD4),
                    unselectedIconColor = Color(0xFF96A0AE),
                    unselectedTextColor = Color(0xFF96A0AE),
                    indicatorColor = Color(0xFF00BCD4).copy(alpha = 0.15f)
                )
            )
        }
    }
}
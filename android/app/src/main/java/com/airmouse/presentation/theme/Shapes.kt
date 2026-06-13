package com.airmouse.presentation.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(Dimensions.radius4),
    small = RoundedCornerShape(Dimensions.radius8),
    medium = RoundedCornerShape(Dimensions.radius12),
    large = RoundedCornerShape(Dimensions.radius16),
    extraLarge = RoundedCornerShape(Dimensions.radius24)
)

/**
 * Alternative shape system with more granular control
 */
object Shapes {

    // ==================== ROUNDED CORNERS ====================

    /** Square corners (0dp) */
    val square = RoundedCornerShape(Dimensions.radius0)

    /** Minimal rounding (2dp) */
    val roundedMinimal = RoundedCornerShape(Dimensions.radius2)

    /** Small rounding (4dp) */
    val roundedSmall = RoundedCornerShape(Dimensions.radius4)

    /** Default rounding (8dp) */
    val roundedDefault = RoundedCornerShape(Dimensions.radius8)

    /** Medium rounding (12dp) */
    val roundedMedium = RoundedCornerShape(Dimensions.radius12)

    /** Large rounding (16dp) */
    val roundedLarge = RoundedCornerShape(Dimensions.radius16)

    /** Extra large rounding (24dp) */
    val roundedExtraLarge = RoundedCornerShape(Dimensions.radius24)

    /** Full rounding (50dp - pill shape) */
    val roundedFull = RoundedCornerShape(Dimensions.radius50)

    // ==================== CUT CORNERS ====================

    /** Small cut corner (4dp) */
    val cutSmall = CutCornerShape(Dimensions.radius4)

    /** Medium cut corner (8dp) */
    val cutMedium = CutCornerShape(Dimensions.radius8)

    /** Large cut corner (12dp) */
    val cutLarge = CutCornerShape(Dimensions.radius12)

    // ==================== SPECIFIC COMPONENT SHAPES ====================

    /** Card shape - Medium rounding with subtle elevation */
    val card = RoundedCornerShape(Dimensions.radius12)

    /** Bottom sheet shape - Large rounding on top corners only */
    val bottomSheet = RoundedCornerShape(
        topStart = Dimensions.radius16,
        topEnd = Dimensions.radius16,
        bottomStart = Dimensions.radius0,
        bottomEnd = Dimensions.radius0
    )

    /** Dialog shape - Extra large rounding */
    val dialog = RoundedCornerShape(Dimensions.radius24)

    /** Button shape - Small rounding */
    val button = RoundedCornerShape(Dimensions.radius8)

    /** Small button shape - Minimal rounding */
    val buttonSmall = RoundedCornerShape(Dimensions.radius4)

    /** Large button shape - Default rounding */
    val buttonLarge = RoundedCornerShape(Dimensions.radius12)

    /** FAB (Floating Action Button) shape - Full rounding (circle/pill) */
    val fab = RoundedCornerShape(Dimensions.radiusFull)

    /** Chip shape - Medium rounding */
    val chip = RoundedCornerShape(Dimensions.radius8)

    /** Navigation bar shape - No rounding */
    val navigationBar = RoundedCornerShape(Dimensions.radius0)

    /** Top app bar shape - No rounding */
    val topAppBar = RoundedCornerShape(Dimensions.radius0)

    /** Search bar shape - Full rounding (pill shape) */
    val searchBar = RoundedCornerShape(Dimensions.radiusFull)

    /** Text field shape - Small rounding */
    val textField = RoundedCornerShape(Dimensions.radius4)

    /** Outlined text field shape - Minimal rounding */
    val outlinedTextField = RoundedCornerShape(Dimensions.radius4)

    /** Dropdown menu shape - Small rounding */
    val dropdownMenu = RoundedCornerShape(Dimensions.radius8)

    /** Tooltip shape - Small rounding */
    val tooltip = RoundedCornerShape(Dimensions.radius4)

    /** Snackbar shape - Small rounding */
    val snackbar = RoundedCornerShape(Dimensions.radius8)

    /** Modal shape - Large rounding */
    val modal = RoundedCornerShape(Dimensions.radius16)

    /** Image thumbnail shape - Small rounding */
    val thumbnail = RoundedCornerShape(Dimensions.radius8)

    /** Avatar shape - Full rounding (circle) */
    val avatar = RoundedCornerShape(Dimensions.radiusFull)

    /** Progress indicator shape - Full rounding */
    val progressIndicator = RoundedCornerShape(Dimensions.radiusFull)

    /** Switch track shape - Full rounding */
    val switchTrack = RoundedCornerShape(Dimensions.radiusFull)

    /** Slider thumb shape - Full rounding */
    val sliderThumb = RoundedCornerShape(Dimensions.radiusFull)

    // ==================== RESPONSIVE SHAPES ====================

    /**
     * Returns a shape that adapts based on screen size
     * @param isLargeScreen Whether the screen is considered large (tablet/desktop)
     */
    fun adaptiveCard(isLargeScreen: Boolean): CornerBasedShape {
        return if (isLargeScreen) {
            roundedLarge  // 16dp on large screens
        } else {
            roundedMedium // 12dp on mobile
        }
    }

    /**
     * Returns a shape with custom corner radii
     */
    fun custom(
        topStart: androidx.compose.ui.unit.Dp = Dimensions.radius0,
        topEnd: androidx.compose.ui.unit.Dp = Dimensions.radius0,
        bottomEnd: androidx.compose.ui.unit.Dp = Dimensions.radius0,
        bottomStart: androidx.compose.ui.unit.Dp = Dimensions.radius0
    ): RoundedCornerShape {
        return RoundedCornerShape(
            topStart = topStart,
            topEnd = topEnd,
            bottomEnd = bottomEnd,
            bottomStart = bottomStart
        )
    }
}

// ==================== HELPER FUNCTIONS ====================

/**
 * Helper function to check if screen is tablet size
 */
fun isTabletSize(widthDp: Int): Boolean {
    return widthDp >= 600
}

/**
 * Helper function to check if screen is desktop size
 */
fun isDesktopSize(widthDp: Int): Boolean {
    return widthDp >= 840
}

/**
 * Get the appropriate shape for bottom sheets
 */
@Composable
fun getBottomSheetShape(): RoundedCornerShape {
    return Shapes.bottomSheet
}

/**
 * Get the appropriate shape for dialogs
 */
@Composable
fun getDialogShape(): RoundedCornerShape {
    return Shapes.dialog
}

/**
 * Get the appropriate shape for cards based on screen size
 */
@Composable
fun getAdaptiveCardShape(isLargeScreen: Boolean): CornerBasedShape {
    return Shapes.adaptiveCard(isLargeScreen)
}
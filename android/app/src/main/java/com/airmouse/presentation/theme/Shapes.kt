package com.airmouse.presentation.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(Dimensions.radius4),
    small = RoundedCornerShape(Dimensions.radius8),
    medium = RoundedCornerShape(Dimensions.radius12),
    large = RoundedCornerShape(Dimensions.radius16),
    extraLarge = RoundedCornerShape(Dimensions.radius24)
)

/**
 * Alternative shape system providing granular component control.
 */
object AppShapesProvider {

    // ==================== ROUNDED CORNERS ====================

    val square = RoundedCornerShape(Dimensions.radius0)
    val roundedMinimal = RoundedCornerShape(Dimensions.radius2)
    val roundedSmall = RoundedCornerShape(Dimensions.radius4)
    val roundedDefault = RoundedCornerShape(Dimensions.radius8)
    val roundedMedium = RoundedCornerShape(Dimensions.radius12)
    val roundedLarge = RoundedCornerShape(Dimensions.radius16)
    val roundedExtraLarge = RoundedCornerShape(Dimensions.radius24)
    val roundedFull = RoundedCornerShape(Dimensions.radius50)

    // ==================== CUT CORNERS ====================

    val cutSmall = CutCornerShape(Dimensions.radius4)
    val cutMedium = CutCornerShape(Dimensions.radius8)
    val cutLarge = CutCornerShape(Dimensions.radius12)

    // ==================== SPECIFIC COMPONENT SHAPES ====================

    val card = RoundedCornerShape(Dimensions.radius12)

    val bottomSheet = RoundedCornerShape(
        topStart = Dimensions.radius16,
        topEnd = Dimensions.radius16,
        bottomStart = Dimensions.radius0,
        bottomEnd = Dimensions.radius0
    )

    val dialog = RoundedCornerShape(Dimensions.radius24)
    val button = RoundedCornerShape(Dimensions.radius8)
    val buttonSmall = RoundedCornerShape(Dimensions.radius4)
    val buttonLarge = RoundedCornerShape(Dimensions.radius12)
    val fab = RoundedCornerShape(Dimensions.radiusFull)
    val chip = RoundedCornerShape(Dimensions.radius8)
    val navigationBar = RoundedCornerShape(Dimensions.radius0)
    val topAppBar = RoundedCornerShape(Dimensions.radius0)
    val searchBar = RoundedCornerShape(Dimensions.radiusFull)
    val textField = RoundedCornerShape(Dimensions.radius4)
    val outlinedTextField = RoundedCornerShape(Dimensions.radius4)
    val dropdownMenu = RoundedCornerShape(Dimensions.radius8)
    val tooltip = RoundedCornerShape(Dimensions.radius4)
    val snackbar = RoundedCornerShape(Dimensions.radius8)
    val modal = RoundedCornerShape(Dimensions.radius16)
    val thumbnail = RoundedCornerShape(Dimensions.radius8)
    val avatar = RoundedCornerShape(Dimensions.radiusFull)
    val progressIndicator = RoundedCornerShape(Dimensions.radiusFull)
    val switchTrack = RoundedCornerShape(Dimensions.radiusFull)
    val sliderThumb = RoundedCornerShape(Dimensions.radiusFull)

    // ==================== RESPONSIVE SHAPES ====================

    fun adaptiveCard(isLargeScreen: Boolean): CornerBasedShape {
        return if (isLargeScreen) roundedLarge else roundedMedium
    }

    fun custom(
        topStart: Dp = Dimensions.radius0,
        topEnd: Dp = Dimensions.radius0,
        bottomEnd: Dp = Dimensions.radius0,
        bottomStart: Dp = Dimensions.radius0
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

fun isTabletSize(widthDp: Int): Boolean = widthDp >= 600

fun isDesktopSize(widthDp: Int): Boolean = widthDp >= 840

@Composable
fun getBottomSheetShape(): RoundedCornerShape = AppShapesProvider.bottomSheet

@Composable
fun getDialogShape(): RoundedCornerShape = AppShapesProvider.dialog

@Composable
fun getAdaptiveCardShape(isLargeScreen: Boolean): CornerBasedShape {
    return AppShapesProvider.adaptiveCard(isLargeScreen)
}
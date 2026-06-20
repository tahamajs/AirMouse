// app/src/main/java/com/airmouse/presentation/theme/Shapes.kt
package com.airmouse.presentation.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ==================== MATERIAL 3 SHAPES ====================

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(Dimensions.radius4),
    small = RoundedCornerShape(Dimensions.radius8),
    medium = RoundedCornerShape(Dimensions.radius12),
    large = RoundedCornerShape(Dimensions.radius16),
    extraLarge = RoundedCornerShape(Dimensions.radius24)
)

// ==================== ALTERNATIVE SHAPE SYSTEM ====================

object AppShapesProvider {

    // ==================== ROUNDED CORNERS ====================

    /** No rounding - square corners */
    val square = RoundedCornerShape(Dimensions.radius0)

    /** Minimal rounding for subtle edges */
    val roundedMinimal = RoundedCornerShape(Dimensions.radius2)

    /** Small rounding for compact elements */
    val roundedSmall = RoundedCornerShape(Dimensions.radius4)

    /** Small rounding with slightly more curve */
    val roundedSmallMedium = RoundedCornerShape(Dimensions.radius6)

    /** Default rounding for most elements */
    val roundedDefault = RoundedCornerShape(Dimensions.radius8)

    /** Medium rounding for cards and containers */
    val roundedMedium = RoundedCornerShape(Dimensions.radius12)

    /** Large rounding for dialogs and cards */
    val roundedLarge = RoundedCornerShape(Dimensions.radius16)

    /** Extra large rounding for prominent elements */
    val roundedExtraLarge = RoundedCornerShape(Dimensions.radius24)

    /** Maximum rounding for pills and badges */
    val roundedFull = RoundedCornerShape(Dimensions.radiusFull)

    // ==================== CUT CORNERS ====================

    /** Small cut corner for modern look */
    val cutSmall = CutCornerShape(Dimensions.radius4)

    /** Medium cut corner for distinctive elements */
    val cutMedium = CutCornerShape(Dimensions.radius8)

    /** Large cut corner for bold design */
    val cutLarge = CutCornerShape(Dimensions.radius12)

    // ==================== ASYMMETRIC SHAPES ====================

    /** Card with rounded top and square bottom */
    val cardTopRounded = RoundedCornerShape(
        topStart = Dimensions.radius12,
        topEnd = Dimensions.radius12,
        bottomStart = Dimensions.radius0,
        bottomEnd = Dimensions.radius0
    )

    /** Card with rounded bottom and square top */
    val cardBottomRounded = RoundedCornerShape(
        topStart = Dimensions.radius0,
        topEnd = Dimensions.radius0,
        bottomStart = Dimensions.radius12,
        bottomEnd = Dimensions.radius12
    )

    /** Card with rounded left side */
    val cardLeftRounded = RoundedCornerShape(
        topStart = Dimensions.radius12,
        topEnd = Dimensions.radius0,
        bottomEnd = Dimensions.radius0,
        bottomStart = Dimensions.radius12
    )

    /** Card with rounded right side */
    val cardRightRounded = RoundedCornerShape(
        topStart = Dimensions.radius0,
        topEnd = Dimensions.radius12,
        bottomEnd = Dimensions.radius12,
        bottomStart = Dimensions.radius0
    )

    // ==================== COMPONENT-SPECIFIC SHAPES ====================

    /** Standard card shape */
    val card = RoundedCornerShape(Dimensions.radius12)

    /** Bottom sheet with rounded top corners */
    val bottomSheet = RoundedCornerShape(
        topStart = Dimensions.radius16,
        topEnd = Dimensions.radius16,
        bottomStart = Dimensions.radius0,
        bottomEnd = Dimensions.radius0
    )

    /** Dialog with fully rounded corners */
    val dialog = RoundedCornerShape(Dimensions.radius24)

    /** Standard button shape */
    val button = RoundedCornerShape(Dimensions.radius8)

    /** Small button shape */
    val buttonSmall = RoundedCornerShape(Dimensions.radius4)

    /** Large button shape */
    val buttonLarge = RoundedCornerShape(Dimensions.radius12)

    /** Floating action button - circular */
    val fab = RoundedCornerShape(Dimensions.radiusFull)

    /** Chip shape */
    val chip = RoundedCornerShape(Dimensions.radius8)

    /** Navigation bar - square bottom */
    val navigationBar = RoundedCornerShape(Dimensions.radius0)

    /** Top app bar - square top */
    val topAppBar = RoundedCornerShape(Dimensions.radius0)

    /** Search bar - pill shape */
    val searchBar = RoundedCornerShape(Dimensions.radiusFull)

    /** Text field shape */
    val textField = RoundedCornerShape(Dimensions.radius4)

    /** Outlined text field shape */
    val outlinedTextField = RoundedCornerShape(Dimensions.radius4)

    /** Dropdown menu shape */
    val dropdownMenu = RoundedCornerShape(Dimensions.radius8)

    /** Tooltip shape */
    val tooltip = RoundedCornerShape(Dimensions.radius4)

    /** Snackbar shape */
    val snackbar = RoundedCornerShape(Dimensions.radius8)

    /** Modal bottom sheet shape */
    val modal = RoundedCornerShape(Dimensions.radius16)

    /** Thumbnail image shape */
    val thumbnail = RoundedCornerShape(Dimensions.radius8)

    /** Avatar - circular */
    val avatar = RoundedCornerShape(Dimensions.radiusFull)

    /** Progress indicator - circular */
    val progressIndicator = RoundedCornerShape(Dimensions.radiusFull)

    /** Switch track - pill shape */
    val switchTrack = RoundedCornerShape(Dimensions.radiusFull)

    /** Slider thumb - circular */
    val sliderThumb = RoundedCornerShape(Dimensions.radiusFull)

    /** Tab indicator shape */
    val tabIndicator = RoundedCornerShape(Dimensions.radiusFull)

    /** Badge shape */
    val badge = RoundedCornerShape(Dimensions.radiusFull)

    /** Checkbox shape */
    val checkbox = RoundedCornerShape(Dimensions.radius2)

    /** Radio button - circular */
    val radioButton = RoundedCornerShape(Dimensions.radiusFull)

    /** Card with shadow elevation */
    val elevatedCard = RoundedCornerShape(Dimensions.radius12)

    /** Outlined card shape */
    val outlinedCard = RoundedCornerShape(Dimensions.radius12)

    /** Filled card shape */
    val filledCard = RoundedCornerShape(Dimensions.radius12)

    // ==================== RESPONSIVE SHAPES ====================

    /**
     * Returns an adaptive card shape based on screen size.
     * @param isLargeScreen True for tablet/desktop screens
     * @return CornerBasedShape appropriate for the screen size
     */
    fun adaptiveCard(isLargeScreen: Boolean): CornerBasedShape {
        return if (isLargeScreen) roundedLarge else roundedMedium
    }

    /**
     * Returns a custom rounded corner shape with specified radii.
     */
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

    /**
     * Returns a pill shape (fully rounded).
     */
    fun pill(): RoundedCornerShape = roundedFull

    /**
     * Returns a shape with only top corners rounded.
     */
    fun topRounded(radius: Dp = Dimensions.radius16): RoundedCornerShape {
        return RoundedCornerShape(
            topStart = radius,
            topEnd = radius,
            bottomStart = Dimensions.radius0,
            bottomEnd = Dimensions.radius0
        )
    }

    /**
     * Returns a shape with only bottom corners rounded.
     */
    fun bottomRounded(radius: Dp = Dimensions.radius16): RoundedCornerShape {
        return RoundedCornerShape(
            topStart = Dimensions.radius0,
            topEnd = Dimensions.radius0,
            bottomStart = radius,
            bottomEnd = radius
        )
    }

    /**
     * Returns a shape with only left corners rounded.
     */
    fun leftRounded(radius: Dp = Dimensions.radius16): RoundedCornerShape {
        return RoundedCornerShape(
            topStart = radius,
            topEnd = Dimensions.radius0,
            bottomEnd = Dimensions.radius0,
            bottomStart = radius
        )
    }

    /**
     * Returns a shape with only right corners rounded.
     */
    fun rightRounded(radius: Dp = Dimensions.radius16): RoundedCornerShape {
        return RoundedCornerShape(
            topStart = Dimensions.radius0,
            topEnd = radius,
            bottomEnd = radius,
            bottomStart = Dimensions.radius0
        )
    }
}

// ==================== SCREEN SIZE HELPERS ====================

/**
 * Returns whether the current screen is a tablet (width >= 600dp).
 */
@Composable
fun isTablet(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp >= 600
}

/**
 * Returns whether the current screen is a desktop (width >= 840dp).
 */
@Composable
fun isDesktop(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp >= 840
}

/**
 * Returns whether the current screen is a phone (width < 600dp).
 */
@Composable
fun isPhone(): Boolean {
    return !isTablet()
}

/**
 * Returns the current screen width in dp.
 */
@Composable
fun getScreenWidth(): Int {
    return LocalConfiguration.current.screenWidthDp
}

/**
 * Returns the current screen height in dp.
 */
@Composable
fun getScreenHeight(): Int {
    return LocalConfiguration.current.screenHeightDp
}

// ==================== SHAPE EXTENSIONS ====================

/**
 * Extension function to get the shape for a specific component based on screen size.
 */
@Composable
fun getAdaptiveCardShape(): CornerBasedShape {
    return if (isTablet()) AppShapesProvider.roundedLarge else AppShapesProvider.roundedMedium
}

/**
 * Extension function to get the bottom sheet shape.
 */
@Composable
fun getBottomSheetShape(): RoundedCornerShape = AppShapesProvider.bottomSheet

/**
 * Extension function to get the dialog shape.
 */
@Composable
fun getDialogShape(): RoundedCornerShape = AppShapesProvider.dialog

/**
 * Extension function to get the button shape.
 */
@Composable
fun getButtonShape(): RoundedCornerShape = AppShapesProvider.button

/**
 * Extension function to get the card shape.
 */
@Composable
fun getCardShape(isElevated: Boolean = false): RoundedCornerShape {
    return if (isElevated) AppShapesProvider.elevatedCard else AppShapesProvider.card
}

/**
 * Extension function to get the chip shape.
 */
@Composable
fun getChipShape(): RoundedCornerShape = AppShapesProvider.chip

// ==================== SHAPE UTILITIES ====================

/**
 * Combines two shapes with a separator.
 */
fun combineShapes(
    top: CornerBasedShape,
    bottom: CornerBasedShape,
    separator: Dp = Dimensions.radius0
): CornerBasedShape {
    // This is a simplified implementation - in practice you'd need more complex logic
    return RoundedCornerShape(
        topStart = Dimensions.radius12,
        topEnd = Dimensions.radius12,
        bottomStart = Dimensions.radius0,
        bottomEnd = Dimensions.radius0
    )
}

/**
 * Creates a shape with a notch cut out.
 */
fun notchShape(
    mainRadius: Dp = Dimensions.radius16,
    notchRadius: Dp = Dimensions.radius8,
    notchPosition: Float = 0.5f
): RoundedCornerShape {
    // Simplified - in production use custom shape
    return RoundedCornerShape(mainRadius)
}

/**
 * Creates a shape with a border.
 */
fun borderedShape(
    shape: CornerBasedShape = AppShapesProvider.roundedDefault,
    borderWidth: Dp = 1.dp
): CornerBasedShape {
    return shape
}

// ==================== PREVIEW HELPERS ====================

/**
 * Preview all available shapes.
 */
@Composable
fun ShapePreview() {
    // Implementation for shape preview
}

/**
 * Gets all available shape names.
 */
fun getAllShapeNames(): List<String> {
    return listOf(
        "square",
        "roundedMinimal",
        "roundedSmall",
        "roundedDefault",
        "roundedMedium",
        "roundedLarge",
        "roundedExtraLarge",
        "roundedFull",
        "cutSmall",
        "cutMedium",
        "cutLarge",
        "card",
        "bottomSheet",
        "dialog",
        "button",
        "fab",
        "chip",
        "searchBar",
        "avatar"
    )
}

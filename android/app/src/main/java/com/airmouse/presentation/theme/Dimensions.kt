// app/src/main/java/com/airmouse/presentation/theme/Dimensions.kt
package com.airmouse.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Centralized dimensions for consistent spacing, sizing, and elevations throughout the app.
 * Following Material Design 3 guidelines with custom adjustments for Air Mouse.
 */
object Dimensions {

    // ==================== SPACING ====================
    // Base spacing unit is 4dp (following Material Design 8dp grid with 4dp increments)

    /** 0dp - No spacing */
    val space0 = 0.dp

    /** 1dp - Minimal spacing */
    val space1 = 1.dp

    /** 2dp - Extra small spacing */
    val space2 = 2.dp

    /** 4dp - Small spacing (base unit) */
    val space4 = 4.dp

    /** 6dp - Between small elements */
    val space6 = 6.dp

    /** 8dp - Default spacing */
    val space8 = 8.dp

    /** 10dp - Medium-small spacing */
    val space10 = 10.dp

    /** 12dp - Medium spacing */
    val space12 = 12.dp

    /** 14dp - Between related elements */
    val space14 = 14.dp

    /** 16dp - Large spacing */
    val space16 = 16.dp

    /** 18dp - Between sections */
    val space18 = 18.dp

    /** 20dp - Extra large spacing */
    val space20 = 20.dp

    /** 24dp - XXL spacing */
    val space24 = 24.dp

    /** 28dp - Section spacing */
    val space28 = 28.dp

    /** 32dp - Screen edge padding */
    val space32 = 32.dp

    /** 40dp - Large screen spacing */
    val space40 = 40.dp

    /** 48dp - Huge spacing */
    val space48 = 48.dp

    /** 56dp - Between major sections */
    val space56 = 56.dp

    /** 64dp - Massive spacing */
    val space64 = 64.dp

    /** 72dp - Extra massive spacing */
    val space72 = 72.dp

    /** 80dp - Bottom sheet peek height */
    val space80 = 80.dp

    /** 96dp - Extra massive spacing */
    val space96 = 96.dp

    /** 128dp - Maximum spacing */
    val space128 = 128.dp

    // ==================== PADDING (Semantic) ====================

    /** Default screen horizontal padding */
    val screenPaddingHorizontal = space16

    /** Default screen vertical padding */
    val screenPaddingVertical = space16

    /** Card content padding */
    val cardPadding = space16

    /** Dialog content padding */
    val dialogPadding = space24

    /** List item padding */
    val listItemPadding = space12

    /** Button content padding horizontal */
    val buttonPaddingHorizontal = space16

    /** Button content padding vertical */
    val buttonPaddingVertical = space8

    /** Chip padding horizontal */
    val chipPaddingHorizontal = space8

    /** Chip padding vertical */
    val chipPaddingVertical = space4

    /** Tab padding horizontal */
    val tabPaddingHorizontal = space12

    /** Tab padding vertical */
    val tabPaddingVertical = space8

    /** Navigation bar item padding */
    val navBarItemPadding = space8

    /** Bottom sheet padding */
    val bottomSheetPadding = space24

    /** Toast padding */
    val toastPadding = space16

    // ==================== ELEVATION ====================

    /** 0dp - No elevation */
    val elevation0 = 0.dp

    /** 1dp - Minimal elevation (hover state) */
    val elevation1 = 1.dp

    /** 2dp - Small elevation (cards, buttons) */
    val elevation2 = 2.dp

    /** 3dp - Medium elevation */
    val elevation3 = 3.dp

    /** 4dp - Standard elevation (dialogs, bottom sheets) */
    val elevation4 = 4.dp

    /** 6dp - Raised elevation */
    val elevation6 = 6.dp

    /** 8dp - High elevation (FAB, dropdown menus) */
    val elevation8 = 8.dp

    /** 12dp - Very high elevation (modal dialogs) */
    val elevation12 = 12.dp

    /** 16dp - Maximum elevation */
    val elevation16 = 16.dp

    /** 24dp - Overlay elevation */
    val elevation24 = 24.dp

    // ==================== CORNER RADIUS ====================

    /** 0dp - Square corners */
    val radius0 = 0.dp

    /** 2dp - Minimal rounding */
    val radius2 = 2.dp

    /** 4dp - Small rounded corners */
    val radius4 = 4.dp

    /** 6dp - Between small and medium */
    val radius6 = 6.dp

    /** 8dp - Medium rounded corners (cards) */
    val radius8 = 8.dp

    /** 10dp - Medium-large rounding */
    val radius10 = 10.dp

    /** 12dp - Large rounded corners (dialogs) */
    val radius12 = 12.dp

    /** 14dp - Between large and extra large */
    val radius14 = 14.dp

    /** 16dp - Extra large corners (bottom sheets) */
    val radius16 = 16.dp

    /** 18dp - Extra large corners */
    val radius18 = 18.dp

    /** 20dp - Very large corners */
    val radius20 = 20.dp

    /** 24dp - XXL corners (rounded containers) */
    val radius24 = 24.dp

    /** 28dp - Pill-shaped corners */
    val radius28 = 28.dp

    /** 32dp - Fully rounded (pill) */
    val radius32 = 32.dp

    /** 40dp - Round shape (circles, pills) */
    val radius40 = 40.dp

    /** 50dp - Round shape (circles, pills) */
    val radius50 = 50.dp

    /** 100dp - Completely round */
    val radiusFull = 100.dp

    // ==================== ICON SIZES ====================

    /** 12dp - Extra small icon */
    val iconExtraSmall = 12.dp

    /** 16dp - Small icon (status bar) */
    val iconSmall = 16.dp

    /** 18dp - Small-medium icon */
    val iconSmallMedium = 18.dp

    /** 20dp - Medium-small icon */
    val iconMediumSmall = 20.dp

    /** 24dp - Standard icon size (default) */
    val iconMedium = 24.dp

    /** 28dp - Medium-large icon */
    val iconMediumLarge = 28.dp

    /** 32dp - Large icon (toolbar) */
    val iconLarge = 32.dp

    /** 36dp - Extra large icon */
    val iconExtraLarge = 36.dp

    /** 40dp - Huge icon (empty states) */
    val iconHuge = 40.dp

    /** 48dp - Massive icon (feature graphics) */
    val iconMassive = 48.dp

    /** 56dp - Extra massive icon */
    val iconExtraMassive = 56.dp

    /** 64dp - Maximum icon size */
    val iconMax = 64.dp

    // ==================== BUTTON SIZES ====================

    /** 32dp - Mini button height */
    val buttonMiniHeight = 32.dp

    /** 36dp - Small button height */
    val buttonSmallHeight = 36.dp

    /** 40dp - Compact button height */
    val buttonCompactHeight = 40.dp

    /** 48dp - Default button height (Material 3 default) */
    val buttonHeight = 48.dp

    /** 56dp - Large button height */
    val buttonLargeHeight = 56.dp

    /** 64dp - Extra large button height */
    val buttonExtraLargeHeight = 64.dp

    /** 72dp - Huge button height */
    val buttonHugeHeight = 72.dp

    // ==================== INPUT FIELD SIZES ====================

    /** 48dp - Default input field height */
    val inputFieldHeight = 48.dp

    /** 56dp - Large input field height */
    val inputFieldLargeHeight = 56.dp

    /** 32dp - Small input field height */
    val inputFieldSmallHeight = 32.dp

    /** 40dp - Compact input field height */
    val inputFieldCompactHeight = 40.dp

    // ==================== APP BAR SIZES ====================

    /** 56dp - Small top app bar height */
    val appBarSmallHeight = 56.dp

    /** 64dp - Medium top app bar height (default) */
    val appBarMediumHeight = 64.dp

    /** 128dp - Large top app bar height */
    val appBarLargeHeight = 128.dp

    /** 152dp - Extra large top app bar height */
    val appBarExtraLargeHeight = 152.dp

    /** 56dp - Bottom app bar height */
    val bottomAppBarHeight = 56.dp

    /** 80dp - Navigation bar height */
    val navigationBarHeight = 80.dp

    // ==================== CARD SIZES ====================

    /** 80dp - Mini card height */
    val cardMiniHeight = 80.dp

    /** 100dp - Small card height */
    val cardSmallHeight = 100.dp

    /** 120dp - Medium card height */
    val cardMediumHeight = 120.dp

    /** 160dp - Large card height */
    val cardLargeHeight = 160.dp

    /** 200dp - Extra large card height */
    val cardExtraLargeHeight = 200.dp

    /** 240dp - Huge card height */
    val cardHugeHeight = 240.dp

    // ==================== DIVIDER SIZES ====================

    /** 0.5dp - Hairline divider */
    val dividerHairline = 0.5.dp

    /** 1dp - Thin divider (default) */
    val dividerThin = 1.dp

    /** 2dp - Thick divider */
    val dividerThick = 2.dp

    // ==================== SCREEN CONSTRAINTS ====================

    /** Minimum screen width for tablets (600dp) */
    val minTabletWidth = 600.dp

    /** Minimum screen width for desktops (840dp) */
    val minDesktopWidth = 840.dp

    /** Maximum content width on large screens */
    val maxContentWidth = 1200.dp

    /** Minimum screen height for landscape */
    val minLandscapeHeight = 480.dp

    // ==================== ANIMATION DURATIONS (milliseconds) ====================

    /** 50ms - Instant animation */
    val animationInstant = 50

    /** 100ms - Very fast animation */
    val animationVeryFast = 100

    /** 200ms - Fast animation (default) */
    val animationFast = 200

    /** 300ms - Normal animation */
    val animationNormal = 300

    /** 400ms - Slow animation */
    val animationSlow = 400

    /** 500ms - Very slow animation */
    val animationVerySlow = 500

    /** 700ms - Extra slow animation */
    val animationExtraSlow = 700

    /** 1000ms - Maximum animation duration */
    val animationMax = 1000

    // ==================== FONT SIZES ====================

    /** 8sp - Tiny text */
    val textTiny = 8.sp

    /** 10sp - Extra small text */
    val textExtraSmall = 10.sp

    /** 11sp - Extra small text */
    val textExtraSmallAlt = 11.sp

    /** 12sp - Small text (captions) */
    val textSmall = 12.sp

    /** 13sp - Small text (captions alt) */
    val textSmallAlt = 13.sp

    /** 14sp - Body small text */
    val textBodySmall = 14.sp

    /** 15sp - Body small text alt */
    val textBodySmallAlt = 15.sp

    /** 16sp - Body medium text (default) */
    val textBodyMedium = 16.sp

    /** 17sp - Body medium text alt */
    val textBodyMediumAlt = 17.sp

    /** 18sp - Body large text */
    val textBodyLarge = 18.sp

    /** 20sp - Title small */
    val textTitleSmall = 20.sp

    /** 22sp - Title small alt */
    val textTitleSmallAlt = 22.sp

    /** 24sp - Title medium */
    val textTitleMedium = 24.sp

    /** 28sp - Title large */
    val textTitleLarge = 28.sp

    /** 32sp - Headline small */
    val textHeadlineSmall = 32.sp

    /** 36sp - Headline medium alt */
    val textHeadlineMediumAlt = 36.sp

    /** 40sp - Headline medium */
    val textHeadlineMedium = 40.sp

    /** 48sp - Headline large */
    val textHeadlineLarge = 48.sp

    /** 56sp - Display small */
    val textDisplaySmall = 56.sp

    /** 64sp - Display medium */
    val textDisplayMedium = 64.sp

    /** 72sp - Display large */
    val textDisplayLarge = 72.sp

    // ==================== COMPONENT SPECIFIC ====================

    /** Switch width */
    val switchWidth = 48.dp

    /** Switch height */
    val switchHeight = 26.dp

    /** Slider height */
    val sliderHeight = 4.dp

    /** Slider thumb size */
    val sliderThumbSize = 20.dp

    /** Progress bar height */
    val progressBarHeight = 4.dp

    /** Circular progress size */
    val circularProgressSize = 48.dp

    /** Small circular progress size */
    val circularProgressSmallSize = 24.dp

    /** Large circular progress size */
    val circularProgressLargeSize = 72.dp

    /** Avatar size */
    val avatarSize = 40.dp

    /** Small avatar size */
    val avatarSmallSize = 32.dp

    /** Large avatar size */
    val avatarLargeSize = 56.dp

    /** Extra large avatar size */
    val avatarExtraLargeSize = 72.dp

    /** Floating action button size */
    val fabSize = 56.dp

    /** Small floating action button size */
    val fabSmallSize = 40.dp

    /** Large floating action button size */
    val fabLargeSize = 72.dp

    /** Badge size */
    val badgeSize = 20.dp

    /** Small badge size */
    val badgeSmallSize = 16.dp

    /** Tooltip max width */
    val tooltipMaxWidth = 300.dp

    /** Toast max width */
    val toastMaxWidth = 400.dp

    /** Snackbar min height */
    val snackbarMinHeight = 48.dp

    /** Dialog max width */
    val dialogMaxWidth = 560.dp

    /** Bottom sheet max height */
    val bottomSheetMaxHeight = 800.dp
}

// ==================== EXTENSION FUNCTIONS ====================

/**
 * Convert dp to px (pixels) - useful for custom drawing
 */
@Composable
fun Dp.toPxInt(): Int {
    return (this.value * LocalDensity.current.density).toInt()
}

/**
 * Convert dp to px (pixels) as Float
 */
@Composable
fun Dp.toPxFloat(): Float {
    return this.value * LocalDensity.current.density
}

/**
 * Convert sp to px (pixels)
 */
@Composable
fun androidx.compose.ui.unit.TextUnit.toPxInt(): Int {
    return (this.value * LocalDensity.current.density).toInt()
}

/**
 * Check if the screen width is considered tablet size (>= 600dp)
 */
@Composable
fun isTabletSize(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp >= Dimensions.minTabletWidth.value
}

/**
 * Check if the screen width is considered desktop size (>= 840dp)
 */
@Composable
fun isDesktopSize(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp >= Dimensions.minDesktopWidth.value
}

/**
 * Check if the screen is in landscape orientation
 */
@Composable
fun isLandscape(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
}

/**
 * Check if the screen is in portrait orientation
 */
@Composable
fun isPortrait(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
}

/**
 * Get the current screen width in dp
 */
@Composable
fun getScreenWidthDp(): Int {
    return LocalConfiguration.current.screenWidthDp
}

/**
 * Get the current screen height in dp
 */
@Composable
fun getScreenHeightDp(): Int {
    return LocalConfiguration.current.screenHeightDp
}

/**
 * Get the current screen density
 */
@Composable
fun getScreenDensity(): Float {
    return LocalDensity.current.density
}

/**
 * Get adaptive padding based on screen size
 */
@Composable
fun getAdaptivePadding(): Dp {
    return when {
        isDesktopSize() -> Dimensions.space32
        isTabletSize() -> Dimensions.space24
        else -> Dimensions.space16
    }
}

/**
 * Get adaptive card spacing based on screen size
 */
@Composable
fun getAdaptiveCardSpacing(): Dp {
    return when {
        isDesktopSize() -> Dimensions.space24
        isTabletSize() -> Dimensions.space16
        else -> Dimensions.space12
    }
}

/**
 * Check if the screen has enough space for side-by-side layout
 */
@Composable
fun canShowSideBySide(): Boolean {
    return getScreenWidthDp() >= 840
}

/**
 * Get the number of columns for a grid based on screen size
 */
@Composable
fun getGridColumns(): Int {
    return when {
        isDesktopSize() -> 4
        isTabletSize() -> 3
        else -> 2
    }
}

// ==================== FLUID SCALING ====================

/**
 * Scale a value based on screen size
 * @param phone Value for phone screens
 * @param tablet Value for tablet screens  
 * @param desktop Value for desktop screens
 */
@Composable
fun <T> fluidScale(
    phone: T,
    tablet: T,
    desktop: T
): T {
    return when {
        isDesktopSize() -> desktop
        isTabletSize() -> tablet
        else -> phone
    }
}

/**
 * Scale a Dp value based on screen size
 */
@Composable
fun fluidDp(
    phone: Dp,
    tablet: Dp = phone * 1.5f,
    desktop: Dp = phone * 2f
): Dp {
    return when {
        isDesktopSize() -> desktop
        isTabletSize() -> tablet
        else -> phone
    }
}

/**
 * Scale a TextUnit based on screen size
 */
@Composable
fun fluidSp(
    phone: androidx.compose.ui.unit.TextUnit,
    tablet: androidx.compose.ui.unit.TextUnit = phone * 1.3f,
    desktop: androidx.compose.ui.unit.TextUnit = phone * 1.6f
): androidx.compose.ui.unit.TextUnit {
    return when {
        isDesktopSize() -> desktop
        isTabletSize() -> tablet
        else -> phone
    }
}

// ==================== DIMENSION HELPERS ====================

/**
 * Get the height for a bottom sheet based on screen height
 */
@Composable
fun getBottomSheetHeight(): Dp {
    val screenHeight = getScreenHeightDp()
    return when {
        isDesktopSize() -> (screenHeight * 0.6).dp
        isTabletSize() -> (screenHeight * 0.7).dp
        else -> (screenHeight * 0.8).dp
    }
}

/**
 * Get the max width for content based on screen size
 */
@Composable
fun getMaxContentWidth(): Dp {
    return when {
        isDesktopSize() -> Dimensions.maxContentWidth
        isTabletSize() -> Dimensions.maxContentWidth * 0.8f
        else -> Dimensions.maxContentWidth * 0.95f
    }
}
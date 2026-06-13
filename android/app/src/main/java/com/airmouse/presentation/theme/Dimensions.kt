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
    
    /** 20dp - Very large corners */
    val radius20 = 20.dp
    
    /** 24dp - XXL corners (rounded containers) */
    val radius24 = 24.dp
    
    /** 28dp - Pill-shaped corners */
    val radius28 = 28.dp
    
    /** 32dp - Fully rounded (pill) */
    val radius32 = 32.dp
    
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
    
    // ==================== INPUT FIELD SIZES ====================
    
    /** 48dp - Default input field height */
    val inputFieldHeight = 48.dp
    
    /** 56dp - Large input field height */
    val inputFieldLargeHeight = 56.dp
    
    /** 32dp - Small input field height */
    val inputFieldSmallHeight = 32.dp
    
    // ==================== APP BAR SIZES ====================
    
    /** 64dp - Small top app bar height */
    val appBarSmallHeight = 64.dp
    
    /** 128dp - Medium top app bar height */
    val appBarMediumHeight = 128.dp
    
    /** 56dp - Bottom app bar height */
    val bottomAppBarHeight = 56.dp
    
    /** 80dp - Navigation bar height */
    val navigationBarHeight = 80.dp
    
    // ==================== CARD SIZES ====================
    
    /** 100dp - Small card height */
    val cardSmallHeight = 100.dp
    
    /** 120dp - Medium card height */
    val cardMediumHeight = 120.dp
    
    /** 160dp - Large card height */
    val cardLargeHeight = 160.dp
    
    /** 200dp - Extra large card height */
    val cardExtraLargeHeight = 200.dp
    
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
    
    // ==================== ANIMATION DURATIONS ====================
    
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
    
    // ==================== FONT SIZES ====================
    
    /** 10sp - Extra small text */
    val textExtraSmall = 10.sp
    
    /** 12sp - Small text (captions) */
    val textSmall = 12.sp
    
    /** 14sp - Body small text */
    val textBodySmall = 14.sp
    
    /** 16sp - Body medium text (default) */
    val textBodyMedium = 16.sp
    
    /** 18sp - Body large text */
    val textBodyLarge = 18.sp
    
    /** 20sp - Title small */
    val textTitleSmall = 20.sp
    
    /** 24sp - Title medium */
    val textTitleMedium = 24.sp
    
    /** 28sp - Title large */
    val textTitleLarge = 28.sp
    
    /** 32sp - Headline small */
    val textHeadlineSmall = 32.sp
    
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
 * Check if the screen width is considered tablet size
 */
@Composable
fun isTabletSize(): Boolean {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    return configuration.screenWidthDp >= Dimensions.minTabletWidth.value
}

/**
 * Check if the screen width is considered desktop size
 */
@Composable
fun isDesktopSize(): Boolean {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    return configuration.screenWidthDp >= Dimensions.minDesktopWidth.value
}

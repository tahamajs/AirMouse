# 📘 Air Mouse Theme Package – Complete Documentation

## 📁 Package Overview

The `com.airmouse.presentation.theme` package provides a **complete theming system** for the Air Mouse application. Built on Jetpack Compose Material 3, it offers dynamic theming with multiple themes, accent colors, custom typography, shapes, and dimensions.

```
com.airmouse.presentation.theme/
├── Theme.kt                    # Main theme composable and configuration
├── ThemeColors.kt              # Color scheme definitions
├── Color.kt                    # Static color definitions
├── Dimensions.kt               # Spacing, sizing, and typography constants
└── Shapes.kt                   # Shape definitions for components
```

---

## 🎨 1. Theme.kt

### Purpose
The **main theme composable** that wraps the entire application. Provides Material 3 theming with support for dynamic colors, custom themes, and system bar integration.

### Key Features

| Feature | Description |
|---------|-------------|
| **Dynamic Colors** | Material You support (Android 12+) |
| **Theme Switching** | Light, Dark, Pure Black, High Contrast |
| **Accent Colors** | 14+ accent color options |
| **Custom Typography** | Inter font family with custom styles |
| **Shapes** | Rounded corner shapes for all components |
| **System Bars** | Automatic status/navigation bar coloring |

### Theme Configuration

```kotlin
@Composable
fun AirMouseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = true,
    themeColors: ThemeColorScheme? = null,
    themeId: String = "system",
    accentColor: AccentColor = AccentColor.ORANGE,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    // Build color scheme
    val colors = themeColors ?: remember(themeId, accentColor) {
        getThemeColorScheme(themeId, accentColor)
    }

    // Apply color scheme
    val colorScheme = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        buildMaterialColorScheme(colors, darkTheme)
    }

    // Apply system bars
    SideEffect {
        val window = (view.context as? Activity)?.window
        window?.let {
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AirMouseTypography,
        shapes = AirMouseShapes,
        content = content
    )
}
```

### Typography

```kotlin
private val InterFontFamily = FontFamily.SansSerif

val AirMouseTypography = Typography(
    displayLarge = Typography().displayLarge.copy(fontFamily = InterFontFamily),
    displayMedium = Typography().displayMedium.copy(fontFamily = InterFontFamily),
    displaySmall = Typography().displaySmall.copy(fontFamily = InterFontFamily),
    headlineLarge = Typography().headlineLarge.copy(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold
    ),
    headlineMedium = Typography().headlineMedium.copy(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold
    ),
    headlineSmall = Typography().headlineSmall.copy(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold
    ),
    titleLarge = Typography().titleLarge.copy(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold
    ),
    // ... all other styles
)
```

### Shapes

```kotlin
val AirMouseShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)
```

---

## 🎨 2. ThemeColors.kt

### Purpose
Defines the **color system** for all themes, including `ThemeColorScheme` data class and color scheme builders for each theme variant.

### ThemeColorScheme

```kotlin
data class ThemeColorScheme(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primary: Color,
    val primaryContainer: Color,
    val secondary: Color,
    val secondaryContainer: Color,
    val tertiary: Color,
    val tertiaryContainer: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val onPrimary: Color,
    val onPrimaryContainer: Color,
    val onSecondary: Color,
    val onSecondaryContainer: Color,
    val onTertiary: Color,
    val onTertiaryContainer: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val outline: Color,
    val outlineVariant: Color,
    val scrim: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val inversePrimary: Color,
    val surfaceTint: Color
)
```

### Theme Builders

```kotlin
object ThemeColorSchemes {
    
    // Light Theme
    fun lightTheme(accent: AccentColor): ThemeColorScheme {
        val accentColor = Color(accent.colorCode)
        return ThemeColorScheme(
            background = Color(0xFFFFFFFF),
            surface = Color(0xFFF8F9FA),
            surfaceVariant = Color(0xFFE9ECEF),
            primary = accentColor,
            primaryContainer = Color(accent.lightColor),
            // ... all other colors
        )
    }
    
    // Dark Theme
    fun darkTheme(accent: AccentColor): ThemeColorScheme {
        val accentColor = Color(accent.colorCode)
        return ThemeColorScheme(
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            surfaceVariant = Color(0xFF2D2D2D),
            primary = Color(accent.lightColor),
            primaryContainer = accentColor,
            // ... all other colors
        )
    }
    
    // Pure Black Theme (AMOLED friendly)
    fun pureBlackTheme(accent: AccentColor): ThemeColorScheme {
        return ThemeColorScheme(
            background = Color(0xFF000000),
            surface = Color(0xFF0D0D0D),
            surfaceVariant = Color(0xFF1A1A1A),
            // ... all other colors
        )
    }
    
    // Premium Themes
    fun oceanTheme(accent: AccentColor): ThemeColorScheme
    fun sunsetTheme(accent: AccentColor): ThemeColorScheme
    fun forestTheme(accent: AccentColor): ThemeColorScheme
    // ... etc
}
```

### Accent Colors

```kotlin
enum class AccentColor(
    val displayName: String,
    val colorCode: Long,
    val lightColor: Long,
    val darkColor: Long
) {
    ORANGE("Orange", 0xFFFF5722, 0xFFFF8A65, 0xFFBF360C),
    BLUE("Blue", 0xFF2196F3, 0xFF64B5F6, 0xFF0D47A1),
    GREEN("Green", 0xFF4CAF50, 0xFF81C784, 0xFF1B5E20),
    PURPLE("Purple", 0xFF9C27B0, 0xFFCE93D8, 0xFF4A148C),
    PINK("Pink", 0xFFE91E63, 0xFFF06292, 0xFF880E4F),
    RED("Red", 0xFFF44336, 0xFFEF9A9A, 0xFFB71C1C),
    TEAL("Teal", 0xFF009688, 0xFF4DB6AC, 0xFF004D40),
    INDIGO("Indigo", 0xFF3F51B5, 0xFF7986CB, 0xFF1A237E),
    CYAN("Cyan", 0xFF00BCD4, 0xFF4DD0E1, 0xFF006064),
    AMBER("Amber", 0xFFFFC107, 0xFFFFD54F, 0xFFFF6F00),
    ROSE("Rose", 0xFFE91E63, 0xFFF48FB1, 0xFF880E4F),
    LIME("Lime", 0xFFCDDC39, 0xFFD4E157, 0xFF827717),
    BROWN("Brown", 0xFF795548, 0xFFA1887F, 0xFF3E2723),
    GREY("Grey", 0xFF607D8B, 0xFF90A4AE, 0xFF263238)
}
```

### Theme Selection

```kotlin
fun getThemeColorScheme(themeId: String, accent: AccentColor): ThemeColorScheme {
    return when (themeId) {
        "light" -> ThemeColorSchemes.lightTheme(accent)
        "dark" -> ThemeColorSchemes.darkTheme(accent)
        "pure_black" -> ThemeColorSchemes.pureBlackTheme(accent)
        "ocean" -> ThemeColorSchemes.oceanTheme(accent)
        "sunset" -> ThemeColorSchemes.sunsetTheme(accent)
        "forest" -> ThemeColorSchemes.darkTheme(AccentColor.GREEN)
        "purple_haze" -> ThemeColorSchemes.darkTheme(AccentColor.PURPLE)
        "cherry" -> ThemeColorSchemes.darkTheme(AccentColor.PINK)
        "neon" -> ThemeColorSchemes.darkTheme(AccentColor.CYAN)
        "lavender" -> ThemeColorSchemes.darkTheme(AccentColor.PURPLE)
        "mint" -> ThemeColorSchemes.darkTheme(AccentColor.GREEN)
        "peach" -> ThemeColorSchemes.darkTheme(AccentColor.ORANGE)
        "sky" -> ThemeColorSchemes.darkTheme(AccentColor.BLUE)
        "midnight" -> ThemeColorSchemes.pureBlackTheme(AccentColor.INDIGO)
        "gold" -> ThemeColorSchemes.darkTheme(AccentColor.AMBER)
        "matrix" -> ThemeColorSchemes.pureBlackTheme(AccentColor.GREEN)
        "cotton_candy" -> ThemeColorSchemes.darkTheme(AccentColor.PINK)
        "coffee" -> ThemeColorSchemes.darkTheme(AccentColor.BROWN)
        else -> ThemeColorSchemes.darkTheme(accent)
    }
}
```

### CompositionLocal

```kotlin
val LocalThemeColors = staticCompositionLocalOf { 
    ThemeColorSchemes.darkTheme(AccentColor.ORANGE) 
}

@Composable
fun ProvideThemeColors(colors: ThemeColorScheme, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalThemeColors provides colors) {
        content()
    }
}
```

---

## 🎨 3. Color.kt

### Purpose
Defines **static color constants** used throughout the application, including Material colors, custom colors, and gradient definitions.

### Material Colors

```kotlin
// Deep Orange
val DeepOrange50 = Color(0xFFFBE9E7)
val DeepOrange100 = Color(0xFFFFCCBC)
val DeepOrange200 = Color(0xFFFFAB91)
val DeepOrange300 = Color(0xFFFF8A65)
val DeepOrange400 = Color(0xFFFF7043)
val DeepOrange500 = Color(0xFFFF5722)
val DeepOrange600 = Color(0xFFF4511E)
val DeepOrange700 = Color(0xFFE64A19)
val DeepOrange800 = Color(0xFFD84315)
val DeepOrange900 = Color(0xFFBF360C)

// Amber
val Amber50 = Color(0xFFFFF8E1)
val Amber100 = Color(0xFFFFECB3)
val Amber200 = Color(0xFFFFE082)
val Amber300 = Color(0xFFFFD54F)
val Amber400 = Color(0xFFFFCA28)
val Amber500 = Color(0xFFFFC107)
val Amber600 = Color(0xFFFFB300)
val Amber700 = Color(0xFFFFA000)
val Amber800 = Color(0xFFFF8F00)
val Amber900 = Color(0xFFF57F17)

// Teal
val Teal50 = Color(0xFFE0F2F1)
val Teal100 = Color(0xFFB2DFDB)
val Teal200 = Color(0xFF80CBC4)
val Teal300 = Color(0xFF4DB6AC)
val Teal400 = Color(0xFF26A69A)
val Teal500 = Color(0xFF009688)
val Teal600 = Color(0xFF00897B)
val Teal700 = Color(0xFF00796B)
val Teal800 = Color(0xFF00695C)
val Teal900 = Color(0xFF004D40)
```

### Semantic Colors

```kotlin
val Success = Color(0xFF10B981)
val SuccessLight = Color(0xFFD1FAE5)
val SuccessDark = Color(0xFF065F46)

val Warning = Color(0xFFF59E0B)
val WarningLight = Color(0xFFFEF3C7)
val WarningDark = Color(0xFF92400E)

val Error = Color(0xFFEF4444)
val ErrorLight = Color(0xFFFEE2E2)
val ErrorDark = Color(0xFF991B1B)

val Info = Color(0xFF3B82F6)
val InfoLight = Color(0xFFDBEAFE)
val InfoDark = Color(0xFF1E3A5F)
```

### Theme Colors

```kotlin
// Dark Theme
val DarkBackground = Color(0xFF0F1115)
val DarkSurface = Color(0xFF1D2430)
val DarkSurfaceVariant = Color(0xFF2B3341)
val DarkOnSurface = Color(0xFFE5E7EB)

// Light Theme
val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF1F5F9)
val LightOnSurface = Color(0xFF1E293B)

// Pure Black
val PureBlack = Color(0xFF000000)
val PureBlackSurface = Color(0xFF0A0A0A)
val PureBlackOnSurface = Color(0xFFF0F0F0)

// High Contrast
val HighContrastPrimary = Color(0xFF000000)
val HighContrastOnPrimary = Color(0xFFFFFFFF)
val HighContrastBackground = Color(0xFFFFFFFF)
val HighContrastOnSurface = Color(0xFF000000)
```

### Gradients

```kotlin
object Gradients {
    val primaryGradient = listOf(DeepOrange500, DeepOrange700)
    val secondaryGradient = listOf(Amber500, Amber800)
    val tertiaryGradient = listOf(Teal500, Teal700)
    val successGradient = listOf(Success, SuccessDark)
    val warningGradient = listOf(Warning, WarningDark)
    val errorGradient = listOf(Error, ErrorDark)
    val infoGradient = listOf(Info, InfoDark)
    val darkGradient = listOf(DarkSurface, DarkSurfaceDim)
    val lightGradient = listOf(LightSurface, LightSurfaceDim)
}
```

---

## 📐 4. Dimensions.kt

### Purpose
Centralizes **all spacing, sizing, and typography constants** for consistent UI across the application.

### Spacing (4dp grid system)

```kotlin
object Dimensions {
    val space0 = 0.dp
    val space1 = 1.dp
    val space2 = 2.dp
    val space4 = 4.dp
    val space6 = 6.dp
    val space8 = 8.dp
    val space10 = 10.dp
    val space12 = 12.dp
    val space14 = 14.dp
    val space16 = 16.dp
    val space18 = 18.dp
    val space20 = 20.dp
    val space24 = 24.dp
    val space28 = 28.dp
    val space32 = 32.dp
    val space40 = 40.dp
    val space48 = 48.dp
    val space56 = 56.dp
    val space64 = 64.dp
    val space72 = 72.dp
    val space80 = 80.dp
    val space96 = 96.dp
    val space128 = 128.dp
}
```

### Corner Radius

```kotlin
val radius0 = 0.dp
val radius2 = 2.dp
val radius4 = 4.dp
val radius6 = 6.dp
val radius8 = 8.dp
val radius10 = 10.dp
val radius12 = 12.dp
val radius14 = 14.dp
val radius16 = 16.dp
val radius18 = 18.dp
val radius20 = 20.dp
val radius24 = 24.dp
val radius28 = 28.dp
val radius32 = 32.dp
val radius40 = 40.dp
val radius50 = 50.dp
val radiusFull = 100.dp
```

### Icon Sizes

```kotlin
val iconExtraSmall = 12.dp
val iconSmall = 16.dp
val iconMedium = 24.dp
val iconMediumLarge = 28.dp
val iconLarge = 32.dp
val iconExtraLarge = 36.dp
val iconHuge = 40.dp
val iconMassive = 48.dp
val iconMax = 64.dp
```

### Button Sizes

```kotlin
val buttonMiniHeight = 32.dp
val buttonSmallHeight = 36.dp
val buttonCompactHeight = 40.dp
val buttonHeight = 48.dp
val buttonLargeHeight = 56.dp
val buttonExtraLargeHeight = 64.dp
```

### Typography (Font Sizes)

```kotlin
val textTiny = 8.sp
val textExtraSmall = 10.sp
val textSmall = 12.sp
val textBodySmall = 14.sp
val textBodyMedium = 16.sp
val textBodyLarge = 18.sp
val textTitleSmall = 20.sp
val textTitleMedium = 24.sp
val textTitleLarge = 28.sp
val textHeadlineSmall = 32.sp
val textHeadlineMedium = 40.sp
val textHeadlineLarge = 48.sp
val textDisplaySmall = 56.sp
val textDisplayMedium = 64.sp
val textDisplayLarge = 72.sp
```

### Screen Helpers

```kotlin
@Composable
fun isTabletSize(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp >= Dimensions.minTabletWidth.value
}

@Composable
fun isDesktopSize(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp >= Dimensions.minDesktopWidth.value
}

@Composable
fun fluidDp(phone: Dp, tablet: Dp = phone * 1.5f, desktop: Dp = phone * 2f): Dp {
    return when {
        isDesktopSize() -> desktop
        isTabletSize() -> tablet
        else -> phone
    }
}
```

---

## 🔷 5. Shapes.kt

### Purpose
Defines **reusable shape definitions** for all components, including rounded corners, cut corners, and asymmetric shapes.

### Basic Shapes

```kotlin
object AppShapesProvider {
    // Rounded Corners
    val square = RoundedCornerShape(Dimensions.radius0)
    val roundedMinimal = RoundedCornerShape(Dimensions.radius2)
    val roundedSmall = RoundedCornerShape(Dimensions.radius4)
    val roundedDefault = RoundedCornerShape(Dimensions.radius8)
    val roundedMedium = RoundedCornerShape(Dimensions.radius12)
    val roundedLarge = RoundedCornerShape(Dimensions.radius16)
    val roundedExtraLarge = RoundedCornerShape(Dimensions.radius24)
    val roundedFull = RoundedCornerShape(Dimensions.radiusFull)
    
    // Cut Corners
    val cutSmall = CutCornerShape(Dimensions.radius4)
    val cutMedium = CutCornerShape(Dimensions.radius8)
    val cutLarge = CutCornerShape(Dimensions.radius12)
}
```

### Component-Specific Shapes

```kotlin
val card = RoundedCornerShape(Dimensions.radius12)
val bottomSheet = RoundedCornerShape(
    topStart = Dimensions.radius16,
    topEnd = Dimensions.radius16,
    bottomStart = Dimensions.radius0,
    bottomEnd = Dimensions.radius0
)
val dialog = RoundedCornerShape(Dimensions.radius24)
val button = RoundedCornerShape(Dimensions.radius8)
val fab = RoundedCornerShape(Dimensions.radiusFull)
val chip = RoundedCornerShape(Dimensions.radius8)
val searchBar = RoundedCornerShape(Dimensions.radiusFull)
val avatar = RoundedCornerShape(Dimensions.radiusFull)
```

### Custom Shape Builders

```kotlin
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

fun topRounded(radius: Dp = Dimensions.radius16): RoundedCornerShape {
    return RoundedCornerShape(
        topStart = radius,
        topEnd = radius,
        bottomStart = Dimensions.radius0,
        bottomEnd = Dimensions.radius0
    )
}

fun bottomRounded(radius: Dp = Dimensions.radius16): RoundedCornerShape {
    return RoundedCornerShape(
        topStart = Dimensions.radius0,
        topEnd = Dimensions.radius0,
        bottomStart = radius,
        bottomEnd = radius
    )
}
```

### Responsive Shape Helpers

```kotlin
@Composable
fun getAdaptiveCardShape(): CornerBasedShape {
    return if (isTablet()) AppShapesProvider.roundedLarge 
           else AppShapesProvider.roundedMedium
}

@Composable
fun getBottomSheetShape(): RoundedCornerShape = AppShapesProvider.bottomSheet

@Composable
fun getDialogShape(): RoundedCornerShape = AppShapesProvider.dialog
```

---

## 📊 Theme Usage Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         THEME USAGE FLOW                               │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. User selects theme in Settings                                     │
│         │                                                               │
│         ▼                                                               │
│  2. PreferencesManager.putString("theme", "dark")                     │
│         │                                                               │
│         ▼                                                               │
│  3. MainActivity reads theme preference                                │
│         │                                                               │
│         ▼                                                               │
│  4. AirMouseTheme(themeId = "dark", accentColor = AccentColor.ORANGE) │
│         │                                                               │
│         ├── getThemeColorScheme("dark", AccentColor.ORANGE)           │
│         │   └── ThemeColorSchemes.darkTheme(AccentColor.ORANGE)       │
│         │                                                               │
│         ├── ProvideThemeColors(colors)                                │
│         │   └── CompositionLocalProvider(LocalThemeColors)            │
│         │                                                               │
│         └── MaterialTheme(colorScheme = materialColorScheme)          │
│                                                                         │
│  5. All components use LocalThemeColors.current                        │
│         │                                                               │
│         ▼                                                               │
│  6. UI reflects new theme colors                                       │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 📋 Theme Summary

| Component | Purpose | Key Features |
|-----------|---------|--------------|
| **Theme.kt** | Main theme composable | Dynamic colors, system bars, typography |
| **ThemeColors.kt** | Color system | 20+ themes, accent colors, CompositionLocal |
| **Color.kt** | Static colors | Material colors, semantic colors, gradients |
| **Dimensions.kt** | Spacing & sizing | 4dp grid, responsive helpers |
| **Shapes.kt** | Shape definitions | Rounded/cut corners, component shapes |

---

## ✅ Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Consistency** | Centralized colors, spacing, and shapes |
| **Flexibility** | 20+ themes with accent color support |
| **Dynamic** | Material You support (Android 12+) |
| **Responsive** | Screen-size aware dimensions |
| **Accessibility** | High contrast mode available |
| **Performance** | Optimized recomposition with `remember` |
| **Maintainability** | All theme constants in one place |

---

**The Theme Package provides a complete, flexible, and maintainable theming system for the Air Mouse application, supporting multiple themes, accent colors, and responsive design.**
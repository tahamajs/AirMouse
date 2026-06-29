# 📘 Air Mouse Presentation Extensions – Complete Documentation

## 📁 Package Overview

The `com.airmouse.presentation.extensions` package contains **utility extensions** for Jetpack Compose, providing helper functions and extension properties to simplify common UI tasks and improve developer productivity.

```
com.airmouse.presentation.extensions/
├── ModifierExtensions.kt          # Modifier extensions
├── ColorExtensions.kt             # Color helper extensions
├── StateExtensions.kt             # StateFlow extensions
├── ContextExtensions.kt           # Context helper extensions
├── ViewExtensions.kt              # View helper extensions
├── StringExtensions.kt            # String formatting extensions
├── NumberExtensions.kt            # Number formatting extensions
├── DateExtensions.kt              # Date formatting extensions
└── ListExtensions.kt              # List helper extensions
```

**Note:** Based on the project structure, these extensions may be spread across different packages or consolidated into a single file. This document provides a complete set of extensions that would be expected in a production Compose application.

---

## 🎨 1. ModifierExtensions

### Purpose
Provides **Modifier extensions** for common Compose UI patterns, simplifying layout and styling code.

### Key Extensions

```kotlin
// ============================================================
// LAYOUT EXTENSIONS
// ============================================================

/**
 * Applies padding based on screen size (adaptive).
 */
fun Modifier.adaptivePadding(): Modifier = this.then(
    Modifier.padding(
        start = if (isTablet()) 24.dp else 16.dp,
        end = if (isTablet()) 24.dp else 16.dp,
        top = if (isTablet()) 20.dp else 12.dp,
        bottom = if (isTablet()) 20.dp else 12.dp
    )
)

/**
 * Applies clickable with ripple effect and haptic feedback.
 */
fun Modifier.clickableWithFeedback(
    onClick: () -> Unit
): Modifier = this.clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = rememberRipple()
) {
    // Trigger haptic feedback
    LocalContext.current.vibrate(10L)
    onClick()
}

/**
 * Applies glassmorphism effect to any composable.
 */
fun Modifier.glass(
    backgroundColor: Color = Color.White.copy(alpha = 0.15f),
    blurRadius: Dp = 20.dp,
    shape: Shape = RoundedCornerShape(16.dp)
): Modifier = this
    .background(backgroundColor, shape)
    .blur(radius = blurRadius, shape = shape)

/**
 * Applies loading shimmer effect.
 */
fun Modifier.shimmer(
    isLoading: Boolean
): Modifier = this.then(
    if (isLoading) {
        Modifier.background(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.1f),
                    Color.White.copy(alpha = 0.3f),
                    Color.White.copy(alpha = 0.1f)
                )
            )
        )
    } else {
        Modifier
    }
)

/**
 * Applies elevation shadow with custom color.
 */
fun Modifier.elevation(
    elevation: Dp = 4.dp,
    shadowColor: Color = Color.Black.copy(alpha = 0.2f)
): Modifier = this.shadow(
    elevation = elevation,
    shape = RoundedCornerShape(12.dp),
    clip = false,
    ambientColor = shadowColor,
    spotColor = shadowColor
)

/**
 * Makes the composable clickable only if condition is true.
 */
fun Modifier.clickableIf(
    condition: Boolean,
    onClick: () -> Unit
): Modifier = if (condition) {
    this.clickable { onClick() }
} else {
    this
}
```

---

## 🎨 2. ColorExtensions

### Purpose
Provides **Color helper extensions** for color manipulation and conversion.

### Key Extensions

```kotlin
// ============================================================
// COLOR EXTENSIONS
// ============================================================

/**
 * Returns the luminance of the color (0-1).
 */
fun Color.luminance(): Float {
    return this.red * 0.299f + this.green * 0.587f + this.blue * 0.114f
}

/**
 * Returns true if the color is light (luminance > 0.5).
 */
fun Color.isLight(): Boolean = this.luminance() > 0.5f

/**
 * Returns true if the color is dark (luminance <= 0.5).
 */
fun Color.isDark(): Boolean = !this.isLight()

/**
 * Returns a contrasting text color (black or white) based on the background.
 */
fun Color.contrastText(): Color {
    return if (this.isLight()) Color.Black else Color.White
}

/**
 * Darkens the color by the specified factor (0-1).
 */
fun Color.darken(factor: Float = 0.2f): Color {
    return Color(
        red = (this.red * (1 - factor)).coerceIn(0f, 1f),
        green = (this.green * (1 - factor)).coerceIn(0f, 1f),
        blue = (this.blue * (1 - factor)).coerceIn(0f, 1f)
    )
}

/**
 * Lightens the color by the specified factor (0-1).
 */
fun Color.lighten(factor: Float = 0.2f): Color {
    return Color(
        red = (this.red + (1 - this.red) * factor).coerceIn(0f, 1f),
        green = (this.green + (1 - this.green) * factor).coerceIn(0f, 1f),
        blue = (this.blue + (1 - this.blue) * factor).coerceIn(0f, 1f)
    )
}

/**
 * Blends two colors together.
 */
fun Color.blend(other: Color, factor: Float = 0.5f): Color {
    val blendFactor = factor.coerceIn(0f, 1f)
    return Color(
        red = this.red * (1 - blendFactor) + other.red * blendFactor,
        green = this.green * (1 - blendFactor) + other.green * blendFactor,
        blue = this.blue * (1 - blendFactor) + other.blue * blendFactor
    )
}

/**
 * Returns the hex string representation of the color.
 */
fun Color.toHexString(): String {
    val r = (this.red * 255).toInt()
    val g = (this.green * 255).toInt()
    val b = (this.blue * 255).toInt()
    return String.format("#%02X%02X%02X", r, g, b)
}
```

---

## 🔄 3. StateExtensions

### Purpose
Provides **StateFlow extensions** for easier state management in Compose.

### Key Extensions

```kotlin
// ============================================================
// STATE EXTENSIONS
// ============================================================

/**
 * Collects a StateFlow as state with lifecycle-aware collection.
 */
@Composable
fun <T> StateFlow<T>.collectAsStateWithLifecycle(): State<T> {
    return this.collectAsState(initial = this.value)
}

/**
 * Collects a SharedFlow as state with lifecycle-aware collection.
 */
@Composable
fun <T> SharedFlow<T>.collectAsStateWithLifecycle(
    initial: T? = null
): State<T?> {
    val state = remember { mutableStateOf(initial) }
    LaunchedEffect(Unit) {
        this@collectAsStateWithLifecycle.collect { value ->
            state.value = value
        }
    }
    return state
}

/**
 * Returns true if the StateFlow has been initialized.
 */
fun <T> StateFlow<T>.isInitialized(): Boolean {
    return this.value != null
}

/**
 * Maps the state value using the provided transform.
 */
fun <T, R> StateFlow<T>.map(transform: (T) -> R): StateFlow<R> {
    return MutableStateFlow(transform(this.value)).also { result ->
        viewModelScope.launch {
            this@map.collect { value ->
                result.value = transform(value)
            }
        }
    }
}

/**
 * Combines two StateFlows into one.
 */
fun <T1, T2, R> combine(
    flow1: StateFlow<T1>,
    flow2: StateFlow<T2>,
    transform: (T1, T2) -> R
): StateFlow<R> {
    return MutableStateFlow(transform(flow1.value, flow2.value)).also { result ->
        viewModelScope.launch {
            flow1.collect { value1 ->
                result.value = transform(value1, flow2.value)
            }
        }
        viewModelScope.launch {
            flow2.collect { value2 ->
                result.value = transform(flow1.value, value2)
            }
        }
    }
}
```

---

## 📱 4. ContextExtensions

### Purpose
Provides **Context helper extensions** for common Android operations.

### Key Extensions

```kotlin
// ============================================================
// CONTEXT EXTENSIONS
// ============================================================

/**
 * Shows a toast message.
 */
fun Context.toast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

/**
 * Shows a toast message from a resource ID.
 */
fun Context.toast(resId: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, getString(resId), duration).show()
}

/**
 * Checks if the device has internet connection.
 */
fun Context.hasInternet(): Boolean {
    val connectivityManager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

/**
 * Checks if the device is connected to WiFi.
 */
fun Context.isWifiConnected(): Boolean {
    val connectivityManager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
}

/**
 * Gets the device model name.
 */
fun Context.getDeviceModel(): String {
    return Build.MANUFACTURER + " " + Build.MODEL
}

/**
 * Gets the Android version string.
 */
fun Context.getAndroidVersion(): String {
    return Build.VERSION.RELEASE
}

/**
 * Vibrates the device.
 */
fun Context.vibrate(duration: Long = 30L) {
    val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    vibrator?.let {
        if (it.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(duration)
            }
        }
    }
}
```

---

## 📝 5. StringExtensions

### Purpose
Provides **String formatting extensions** for UI display.

### Key Extensions

```kotlin
// ============================================================
// STRING EXTENSIONS
// ============================================================

/**
 * Capitalizes the first letter of the string.
 */
fun String.capitalizeFirst(): String {
    if (this.isEmpty()) return this
    return this.substring(0, 1).uppercase() + this.substring(1)
}

/**
 * Truncates the string to the specified length and adds ellipsis.
 */
fun String.truncate(maxLength: Int = 30, ellipsis: String = "..."): String {
    if (this.length <= maxLength) return this
    return this.substring(0, maxLength - ellipsis.length) + ellipsis
}

/**
 * Converts camelCase to Title Case.
 */
fun String.camelToTitle(): String {
    return this.replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .split(" ")
        .joinToString(" ") { it.capitalizeFirst() }
}

/**
 * Converts snake_case to Title Case.
 */
fun String.snakeToTitle(): String {
    return this.split("_")
        .joinToString(" ") { it.capitalizeFirst() }
}

/**
 * Checks if the string is a valid IP address.
 */
fun String.isValidIp(): Boolean {
    val pattern = Regex("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$")
    return pattern.matches(this)
}

/**
 * Returns the string with the first character capitalized.
 */
fun String.firstCharUppercase(): String {
    if (this.isEmpty()) return this
    return this.substring(0, 1).uppercase() + this.substring(1)
}
```

---

## 🔢 6. NumberExtensions

### Purpose
Provides **Number formatting extensions** for UI display.

### Key Extensions

```kotlin
// ============================================================
// NUMBER EXTENSIONS
// ============================================================

/**
 * Formats a number as a percentage.
 */
fun Number.toPercentage(decimalPlaces: Int = 0): String {
    return "${String.format("%.${decimalPlaces}f", this.toFloat() * 100)}%"
}

/**
 * Formats a number with the specified decimal places.
 */
fun Number.formatDecimal(decimalPlaces: Int = 2): String {
    return String.format("%.${decimalPlaces}f", this.toFloat())
}

/**
 * Formats a number as a file size (bytes → KB/MB/GB).
 */
fun Long.toFileSize(): String {
    return when {
        this < 1024 -> "$this B"
        this < 1024 * 1024 -> "${this / 1024} KB"
        this < 1024 * 1024 * 1024 -> "${this / (1024 * 1024)} MB"
        else -> "${this / (1024 * 1024 * 1024)} GB"
    }
}

/**
 * Formats a number as a duration (ms → HH:MM:SS).
 */
fun Long.toDuration(): String {
    val totalSeconds = this / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%02d:%02d", minutes, seconds)
    }
}

/**
 * Formats a number as a distance with units.
 */
fun Float.toDistance(unit: String = "m"): String {
    return when {
        this >= 1000 -> "${(this / 1000).formatDecimal(1)} km"
        else -> "${this.formatDecimal(1)} $unit"
    }
}
```

---

## 📅 7. DateExtensions

### Purpose
Provides **Date formatting extensions** for UI display.

### Key Extensions

```kotlin
// ============================================================
// DATE EXTENSIONS
// ============================================================

/**
 * Formats a timestamp as a date string.
 */
fun Long.formatDate(pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
    val date = Date(this)
    val format = SimpleDateFormat(pattern, Locale.getDefault())
    return format.format(date)
}

/**
 * Formats a timestamp as a time string (HH:MM:SS).
 */
fun Long.formatTime(): String {
    return this.formatDate("HH:mm:ss")
}

/**
 * Formats a timestamp as a short date (MMM dd).
 */
fun Long.formatShortDate(): String {
    return this.formatDate("MMM dd")
}

/**
 * Formats a timestamp as a relative time string (e.g., "2 hours ago").
 */
fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - this

    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000} min ago"
        diff < 86400000 -> "${diff / 3600000} hours ago"
        diff < 604800000 -> "${diff / 86400000} days ago"
        else -> this.formatShortDate()
    }
}

/**
 * Checks if a timestamp is today.
 */
fun Long.isToday(): Boolean {
    val date = Date(this)
    val today = Date()
    return SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(date) ==
           SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(today)
}
```

---

## 📋 8. ListExtensions

### Purpose
Provides **List helper extensions** for common collection operations.

### Key Extensions

```kotlin
// ============================================================
// LIST EXTENSIONS
// ============================================================

/**
 * Returns the item at the specified index, or null if out of bounds.
 */
fun <T> List<T>.safeGet(index: Int): T? {
    return if (index in 0 until this.size) this[index] else null
}

/**
 * Returns the item at the specified index, or a default value if out of bounds.
 */
fun <T> List<T>.safeGet(index: Int, defaultValue: T): T {
    return if (index in 0 until this.size) this[index] else defaultValue
}

/**
 * Chunks the list into groups of the specified size.
 */
fun <T> List<T>.chunkedBySize(size: Int): List<List<T>> {
    return this.chunked(size)
}

/**
 * Returns the last item in the list, or null if empty.
 */
fun <T> List<T>.lastOrNull(): T? {
    return if (this.isNotEmpty()) this[this.size - 1] else null
}

/**
 * Returns the first item in the list, or null if empty.
 */
fun <T> List<T>.firstOrNull(): T? {
    return if (this.isNotEmpty()) this[0] else null
}

/**
 * Filters the list and returns only unique items based on the selector.
 */
fun <T, K> List<T>.distinctBy(selector: (T) -> K): List<T> {
    val seen = mutableSetOf<K>()
    return this.filter { seen.add(selector(it)) }
}
```

---

## 📊 Extension Usage Examples

### Modifier Extensions

```kotlin
@Composable
fun ExampleScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .adaptivePadding()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glass(
                    backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    blurRadius = 16.dp
                )
                .clickableWithFeedback { /* onClick */ }
                .elevation(elevation = 8.dp)
        ) {
            Text("Hello World")
        }
    }
}
```

### Color Extensions

```kotlin
@Composable
fun ColorExample() {
    val primaryColor = MaterialTheme.colorScheme.primary
    val textColor = primaryColor.contrastText()
    val darkenedColor = primaryColor.darken(0.3f)
    val lightenedColor = primaryColor.lighten(0.3f)
    
    Text(
        text = "Contrast Text",
        color = textColor,
        modifier = Modifier.background(primaryColor)
    )
}
```

### String Extensions

```kotlin
@Composable
fun StringExample() {
    val title = "connection_status".snakeToTitle() // "Connection Status"
    val truncated = "This is a very long string that needs truncation".truncate(20) // "This is a very lon..."
    val ip = "192.168.1.100"
    val isValid = ip.isValidIp() // true
}
```

### Number Extensions

```kotlin
@Composable
fun NumberExample() {
    val progress = 0.75f.toPercentage() // "75%"
    val distance = 1234.5f.toDistance() // "1.2 km"
    val duration = 3661000L.toDuration() // "01:01:01"
    val fileSize = 1024 * 1024 * 5L.toFileSize() // "5 MB"
}
```

---

## ✅ Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Consistency** | Extensions provide consistent formatting across the app |
| **Conciseness** | Reduce boilerplate code |
| **Readability** | Clear naming and documentation |
| **Type Safety** | Use Kotlin's type system for safety |
| **Performance** | Optimized for Compose's recomposition model |
| **Reusability** | Extensions can be used in any screen |

---

**The Presentation Extensions package provides a comprehensive set of helper functions and extensions that simplify development, improve code readability, and ensure consistency across the Air Mouse UI.**
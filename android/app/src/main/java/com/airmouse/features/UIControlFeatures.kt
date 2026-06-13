// UIControlFeatures.kt
package com.airmouse.features

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

/**
 * UI control features: floating panel, radial menu, gesture training UI,
 * sensitivity curve editor, on‑screen keyboard.
 * All components are Compose‑based and can be displayed as overlays.
 */
class UIControlFeatures {

    // ==================== 1. Floating control panel ====================
    data class FloatingPanelConfig(
        val position: PanelPosition = PanelPosition.TOP_RIGHT,
        val size: Float = 0.15f,      // percentage of screen width
        val opacity: Float = 0.8f,
        val autoHideDelay: Long = 3000L,
        val buttons: List<PanelButton> = defaultButtons()
    )

    enum class PanelPosition { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, FREE }

    data class PanelButton(
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val label: String,
        val onClick: () -> Unit,
        val longPressAction: (() -> Unit)? = null
    )

    fun defaultButtons(onClick: (String) -> Unit = {}): List<PanelButton> = listOf(
        PanelButton(Icons.Default.Home, "Home", { onClick("home") }),
        PanelButton(Icons.Default.Settings, "Settings", { onClick("settings") }),
        PanelButton(Icons.Default.CalendarMonth, "Calibrate", { onClick("calibrate") }),
        PanelButton(Icons.Default.Gesture, "Gestures", { onClick("gestures") }),
        PanelButton(Icons.Default.Mic, "Voice", { onClick("voice") })
    )

    @Composable
    fun FloatingControlPanel(
        config: FloatingPanelConfig,
        onButtonClick: (String) -> Unit,
        modifier: Modifier = Modifier
    ) {
        var visible by remember { mutableStateOf(true) }
        var offsetX by remember { mutableStateOf(0f) }
        var offsetY by remember { mutableStateOf(0f) }
        var panelSize by remember { mutableStateOf(0 to 0) }
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp
        val screenHeight = configuration.screenHeightDp.dp
        val density = LocalDensity.current

        val panelWidth = (screenWidth.value * config.size).dp
        val panelHeight = panelWidth // square

        // Auto‑hide
        LaunchedEffect(visible) {
            if (visible && config.autoHideDelay > 0) {
                delay(config.autoHideDelay)
                visible = false
            }
        }

        // Initial position based on config.position
        LaunchedEffect(Unit) {
            offsetX = when (config.position) {
                PanelPosition.TOP_LEFT -> 0f
                PanelPosition.TOP_RIGHT -> (screenWidth.value - panelWidth.value)
                PanelPosition.BOTTOM_LEFT -> 0f
                PanelPosition.BOTTOM_RIGHT -> (screenWidth.value - panelWidth.value)
                PanelPosition.FREE -> offsetX
            }
            offsetY = when (config.position) {
                PanelPosition.TOP_LEFT, PanelPosition.TOP_RIGHT -> 0f
                PanelPosition.BOTTOM_LEFT, PanelPosition.BOTTOM_RIGHT -> (screenHeight.value - panelHeight.value)
                PanelPosition.FREE -> offsetY
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.toInt(), offsetY.toInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                        // keep inside screen
                        offsetX = offsetX.coerceIn(0f, screenWidth.value - panelWidth.value)
                        offsetY = offsetY.coerceIn(0f, screenHeight.value - panelHeight.value)
                        visible = true // show on drag
                    }
                }
                .onGloballyPositioned { layout ->
                    panelSize = layout.size.width to layout.size.height
                }
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Card(
                    modifier = Modifier
                        .size(panelWidth, panelHeight)
                        .clip(RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = config.opacity)
                    ),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        config.buttons.forEach { button ->
                            IconButton(
                                onClick = {
                                    button.onClick()
                                    visible = false
                                },
                                onLongClick = button.longPressAction?.let { { it(); visible = false } }
                            ) {
                                Icon(button.icon, contentDescription = button.label, modifier = Modifier.size(32.dp))
                            }
                            Text(button.label, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }

    // ==================== 2. Quick action radial menu ====================
    data class RadialMenuItem(val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String, val action: () -> Unit)

    data class RadialMenuConfig(
        val centerX: Float,
        val centerY: Float,
        val radius: Float,
        val items: List<RadialMenuItem>,
        val animationDuration: Long = 200L
    )

    @Composable
    fun RadialMenu(
        config: RadialMenuConfig,
        onDismiss: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        var selectedIndex by remember { mutableStateOf(-1) }
        val angleStep = (2 * PI / config.items.size).toFloat()

        Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
            detectDragGestures { change, _ ->
                val dx = change.position.x - config.centerX
                val dy = change.position.y - config.centerY
                val distance = sqrt(dx*dx + dy*dy)
                if (distance <= config.radius) {
                    var angle = atan2(dy, dx)
                    if (angle < 0) angle += 2 * PI.toFloat()
                    val index = (angle / angleStep).toInt() % config.items.size
                    selectedIndex = index
                } else {
                    selectedIndex = -1
                }
                change.consume()
            }
        }.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.changes.all { !it.pressed } && selectedIndex != -1) {
                        config.items[selectedIndex].action()
                        onDismiss()
                        selectedIndex = -1
                        break
                    }
                }
            }
        }) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    radius = config.radius,
                    center = Offset(config.centerX, config.centerY)
                )
                drawCircle(
                    color = MaterialTheme.colorScheme.primary,
                    radius = config.radius,
                    center = Offset(config.centerX, config.centerY),
                    style = Stroke(width = 2f)
                )
                for (i in config.items.indices) {
                    val angle = i * angleStep
                    val x = config.centerX + config.radius * 0.7f * cos(angle)
                    val y = config.centerY + config.radius * 0.7f * sin(angle)
                    drawCircle(
                        color = if (selectedIndex == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        radius = 28f,
                        center = Offset(x, y)
                    )
                }
            }
            config.items.forEachIndexed { i, item ->
                val angle = i * angleStep
                val x = config.centerX + config.radius * 0.7f * cos(angle)
                val y = config.centerY + config.radius * 0.7f * sin(angle)
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.offset(
                        x = (x - 24).dp,
                        y = (y - 24).dp
                    )
                ) {
                    Icon(item.icon, contentDescription = item.label, tint = if (selectedIndex == i) Color.White else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }

    // ==================== 3. Gesture training UI ====================
    data class GestureTrainingConfig(
        val targetGesture: String,
        val requiredConfidence: Float = 0.8f,
        val attemptsNeeded: Int = 5,
        val showGuidelines: Boolean = true,
        val hapticFeedback: Boolean = true
    )

    @Composable
    fun GestureTrainingScreen(
        config: GestureTrainingConfig,
        onComplete: () -> Unit,
        onCancel: () -> Unit,
        onAddSensorData: (Float, Float, Float, Float, Float, Float) -> Unit, // gyroX,gyroY,gyroZ,accX,accY,accZ
        viewModel: androidx.lifecycle.ViewModel
    ) {
        var attempts by remember { mutableStateOf(0) }
        var bestConfidence by remember { mutableStateOf(0f) }
        var currentConfidence by remember { mutableStateOf(0f) }
        var isRecording by remember { mutableStateOf(false) }
        var countdown by remember { mutableStateOf(3) }
        var recordingTime by remember { mutableStateOf(0) }

        LaunchedEffect(isRecording) {
            if (isRecording) {
                // Simulated recognition loop
                while (isRecording && recordingTime < 3000) {
                    delay(100)
                    recordingTime += 100
                    currentConfidence = Random.nextFloat() * 0.8f + 0.2f
                }
                isRecording = false
                if (currentConfidence >= config.requiredConfidence) {
                    attempts++
                    bestConfidence = max(bestConfidence, currentConfidence)
                }
                recordingTime = 0
            }
        }

        Scaffold(
            topBar = { TopAppBar(title = { Text("Train: ${config.targetGesture}") }) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Perform the gesture naturally", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (config.showGuidelines) {
                            Text("Move the phone in a ${config.targetGesture} pattern", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Progress
                Text("Attempts: $attempts / ${config.attemptsNeeded}", style = MaterialTheme.typography.headlineSmall)
                LinearProgressIndicator(progress = attempts.toFloat() / config.attemptsNeeded, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(16.dp))

                Text("Best confidence: ${(bestConfidence * 100).toInt()}%", style = MaterialTheme.typography.bodyLarge)

                Spacer(modifier = Modifier.height(32.dp))

                if (isRecording) {
                    CircularProgressIndicator()
                    Text("Recording... ${recordingTime / 1000}s", style = MaterialTheme.typography.bodyLarge)
                } else {
                    Button(
                        onClick = {
                            isRecording = true
                            recordingTime = 0
                        },
                        enabled = attempts < config.attemptsNeeded,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start Recording")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(onClick = onCancel) { Text("Cancel") }
                    Button(
                        onClick = onComplete,
                        enabled = attempts >= config.attemptsNeeded
                    ) {
                        Text("Complete")
                    }
                }
            }
        }
    }

    // ==================== 4. Real-time sensitivity curve editor ====================
    data class Point(val x: Float, val y: Float) // x in 0..1, y in 0..1

    enum class InterpolationType { LINEAR, SPLINE }

    data class SensitivityCurveEditor(
        val points: List<Point>,
        val interpolation: InterpolationType = InterpolationType.SPLINE,
        val previewEnabled: Boolean = true
    )

    @Composable
    fun SensitivityCurveEditorDialog(
        initialPoints: List<Point>,
        onSave: (List<Point>) -> Unit,
        onDismiss: () -> Unit
    ) {
        var points by remember { mutableStateOf(initialPoints.sortedBy { it.x }) }
        var selectedPointIndex by remember { mutableStateOf(-1) }
        var dragging by remember { mutableStateOf(false) }
        val density = LocalDensity.current

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Edit Sensitivity Curve") },
            text = {
                Column {
                    // Curve preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val x = offset.x / size.width
                                        val y = 1f - (offset.y / size.height)
                                        val idx = points.indexOfFirst { abs(it.x - x) < 0.05f }
                                        selectedPointIndex = if (idx >= 0) idx else -1
                                        dragging = true
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        if (selectedPointIndex >= 0) {
                                            val newX = (change.position.x / size.width).coerceIn(0f, 1f)
                                            val newY = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                                            val newPoints = points.toMutableList()
                                            newPoints[selectedPointIndex] = Point(newX, newY)
                                            points = newPoints.sortedBy { it.x }
                                        }
                                    },
                                    onDragEnd = { dragging = false }
                                )
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Draw grid
                            for (i in 0..4) {
                                val x = size.width * i / 4f
                                val y = size.height * i / 4f
                                drawLine(
                                    color = Color.Gray.copy(alpha = 0.3f),
                                    start = Offset(x, 0f),
                                    end = Offset(x, size.height)
                                )
                                drawLine(
                                    color = Color.Gray.copy(alpha = 0.3f),
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y)
                                )
                            }
                            // Draw control points
                            points.forEach { point ->
                                val x = point.x * size.width
                                val y = (1f - point.y) * size.height
                                drawCircle(
                                    color = MaterialTheme.colorScheme.primary,
                                    radius = 8f,
                                    center = Offset(x, y)
                                )
                            }
                            // Draw curve
                            if (points.size > 1) {
                                val path = androidx.compose.ui.graphics.Path()
                                for (i in 0..100) {
                                    val t = i / 100f
                                    val x = t * size.width
                                    val y = interpolate(points, t) * size.height
                                    if (i == 0) path.moveTo(x, y)
                                    else path.lineTo(x, y)
                                }
                                drawPath(path, MaterialTheme.colorScheme.primary, style = Stroke(width = 3f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Add point button
                    Button(
                        onClick = {
                            // Insert new point at mid x
                            val newX = 0.5f
                            val newY = interpolate(points, newX)
                            val newPoints = (points + Point(newX, newY)).sortedBy { it.x }
                            points = newPoints
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Point")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onSave(points) }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        )
    }

    private fun interpolate(points: List<Point>, x: Float): Float {
        if (points.size < 2) return x
        if (x <= points.first().x) return points.first().y
        if (x >= points.last().x) return points.last().y
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            if (x in p1.x..p2.x) {
                val t = (x - p1.x) / (p2.x - p1.x)
                return p1.y + t * (p2.y - p1.y)
            }
        }
        return x
    }

    // ==================== 5. On-screen keyboard ====================
    data class OnScreenKeyboard(
        val layout: KeyboardLayout = KeyboardLayout.QWERTY,
        val keySize: Int = 40,
        val hapticOnKeyPress: Boolean = true,
        val autoCorrect: Boolean = true
    )

    enum class KeyboardLayout { QWERTY, AZERTY, NUMERIC, CUSTOM }

    @Composable
    fun OnScreenKeyboard(
        config: OnScreenKeyboard,
        onKeyPress: (String) -> Unit,
        onDismiss: () -> Unit
    ) {
        val rows = when (config.layout) {
            KeyboardLayout.QWERTY -> listOf(
                listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
                listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
                listOf("Shift", "z", "x", "c", "v", "b", "n", "m", "⌫"),
                listOf("123", " ", "Enter")
            )
            KeyboardLayout.AZERTY -> listOf(
                listOf("a", "z", "e", "r", "t", "y", "u", "i", "o", "p"),
                listOf("q", "s", "d", "f", "g", "h", "j", "k", "l", "m"),
                listOf("Shift", "w", "x", "c", "v", "b", "n", "⌫"),
                listOf("123", " ", "Enter")
            )
            else -> listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("0","⌫","Enter"))
        }

        var shift by remember { mutableStateOf(false) }
        var numbers by remember { mutableStateOf(false) }

        val vibrator = if (config.hapticOnKeyPress) {
            val v = androidx.compose.ui.platform.LocalContext.current.getSystemService(android.os.Vibrator::class.java)
            { duration: Long -> if (android.os.Build.VERSION.SDK_INT >= 26) v?.vibrate(android.os.VibrationEffect.createOneShot(duration, 128)) else v?.vibrate(duration) }
        } else { _: Long -> }

        ModalBottomSheet(onDismissRequest = onDismiss) {
            Column(modifier = Modifier.padding(16.dp)) {
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { key ->
                            val displayKey = when {
                                shift && key.matches(Regex("[a-z]")) -> key.uppercase()
                                numbers -> when (key) {
                                    "q"->"1";"w"->"2";"e"->"3";"r"->"4";"t"->"5";"y"->"6";"u"->"7";"i"->"8";"o"->"9";"p"->"0"
                                    else -> key
                                }
                                else -> key
                            }
                            OutlinedButton(
                                onClick = {
                                    when (key) {
                                        "⌫" -> onKeyPress("backspace")
                                        "Shift" -> shift = !shift
                                        "123" -> numbers = !numbers
                                        "Enter" -> onKeyPress("enter")
                                        " " -> onKeyPress(" ")
                                        else -> onKeyPress(displayKey)
                                    }
                                    vibrator(20)
                                },
                                modifier = Modifier.padding(4.dp).size(config.keySize.dp)
                            ) {
                                Text(displayKey, fontSize = if (key == " ") 8.sp else 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
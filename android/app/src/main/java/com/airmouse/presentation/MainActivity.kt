package com.airmouse.presentation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.airmouse.presentation.ui.home.HomeViewModel
import android.view.KeyEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.airmouse.domain.repository.IMouseRepository
import com.airmouse.network.ConnectionManager
import com.airmouse.presentation.navigation.Destinations
import com.airmouse.presentation.theme.AirMouseTheme
import com.airmouse.presentation.ui.main.MainScreen
import com.airmouse.presentation.ui.themes.*
import com.airmouse.sensors.SensorService
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main entry point for the Air Mouse Android app.
 * Handles splash screen, permissions, sensor lifecycle, and navigation.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var prefs: PreferencesManager

    private val viewModel: HomeViewModel by viewModels()

    @Inject
    lateinit var sensorService: SensorService

    @Inject
    lateinit var connectionManager: ConnectionManager

    @Inject
    lateinit var mouseRepository: IMouseRepository

    private var isReady = false
    private var permissionGranted = false
    private var pendingRoute by mutableStateOf<String?>(null)

    // ============================================================
    // Permission Launchers
    // ============================================================

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            requestPermissions()
        } else {
            showRationaleDialog()
        }
    }

    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            requestPermissions()
        } else {
            showRationaleDialog()
        }
    }

    private val generalPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            permissionGranted = true
            proceedToMain()
        } else {
            showRationaleDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen and keep it up while we check permissions.
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !isReady }

        super.onCreate(savedInstanceState)

        // Eagerly initialize the ViewModel on the Main thread to avoid thread assertion crash when accessed on SensorThread
        viewModel.uiState

        getRouteFromIntent(intent)?.let { route ->
            pendingRoute = route
        }

        // Setup sensor callback – sends movement data to the server via MouseRepository
        var lastPitch = 0f
        var lastYaw = 0f
        var isFirstOrientation = true
        
        // Cache isActive locally to avoid accessing viewModel on background thread
        val isAirMouseActive = java.util.concurrent.atomic.AtomicBoolean(false)
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                isAirMouseActive.set(state.isActive)
            }
        }

        // Use a conflated channel to queue high-frequency sensor updates and process on IO thread
        val orientationChannel = kotlinx.coroutines.channels.Channel<Pair<Float, Float>>(
            kotlinx.coroutines.channels.Channel.CONFLATED
        )

        lifecycleScope.launch(Dispatchers.IO) {
            for (delta in orientationChannel) {
                try {
                    mouseRepository.move(delta.first, delta.second)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error sending mouse movement: ${e.message}")
                }
            }
        }

        sensorService.onOrientationChanged = { pitch, yaw ->
            val activeMode = prefs.getActiveMode()
            if (isAirMouseActive.get() && activeMode.moveSource == "gyro" && connectionManager.isMouseControlEnabled && connectionManager.isConnected()) {
                if (isFirstOrientation) {
                    lastPitch = pitch
                    lastYaw = yaw
                    isFirstOrientation = false
                } else {
                    var dYaw = (yaw - lastYaw) % 360f
                    if (dYaw > 180f) dYaw -= 360f
                    if (dYaw < -180f) dYaw += 360f

                    var dPitch = (pitch - lastPitch) % 360f
                    if (dPitch > 180f) dPitch -= 360f
                    if (dPitch < -180f) dPitch += 360f

                    lastPitch = pitch
                    lastYaw = yaw

                    // Filter out genuine massive noise jumps, but allow legitimate rapid movement
                    if (kotlin.math.abs(dYaw) < 90f && kotlin.math.abs(dPitch) < 90f) {
                        orientationChannel.trySend(Pair(dYaw, dPitch))
                    }
                }
            }
        }
        sensorService.start()

        // Set up system bars and window flags.
        setupWindow()

        // Apply orientation lock from preferences
        val isLocked = prefs.getBoolean("orientation_locked", false)
        requestedOrientation = if (isLocked) {
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        // Check permissions
        val missingPerms = getMissingPermissions()
        if (missingPerms.isEmpty()) {
            permissionGranted = true
            Handler(Looper.getMainLooper()).post {
                proceedToMain()
            }
        } else {
            requestPermissions()
        }

        // Safety timeout
        Handler(Looper.getMainLooper()).postDelayed({
            if (permissionGranted && !isReady) {
                proceedToMain()
            }
        }, 1000)

        // Dim screen when Air Mouse is active
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                val params = window.attributes
                if (state.isActive) {
                    params.screenBrightness = 0.05f
                } else {
                    params.screenBrightness = -1f
                }
                window.attributes = params
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateSystemBarsColor()
        if (::sensorService.isInitialized) {
            sensorService.start()
        }
        if (!connectionManager.isConnected() && prefs.isAutoConnect()) {
            lifecycleScope.launch(Dispatchers.IO) {
                connectionManager.reconnect()
            }
        }
        if (!isReady && getMissingPermissions().isEmpty()) {
            permissionGranted = true
            proceedToMain()
        }
    }

    override fun onPause() {
        super.onPause()
        // Pause sensor sampling when app goes into background or screen turns off
        if (::sensorService.isInitialized) {
            sensorService.stop()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        getRouteFromIntent(intent)?.let { route ->
            pendingRoute = route
        }
    }

    private fun getRouteFromIntent(intent: Intent?): String? {
        if (intent == null) return null
        return when {
            intent.getBooleanExtra("open_status_center", false) -> Destinations.NotificationsCenter.route
            intent.getBooleanExtra("open_network_discovery", false) -> Destinations.NetworkDiscovery.route
            intent.getBooleanExtra("open_touchpad", false) -> Destinations.Touchpad.route
            else -> null
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateSystemBarsColor()
    }

    override fun onDestroy() {
        if (::sensorService.isInitialized) {
            sensorService.stop()
        }
        super.onDestroy()
    }
 
    // ============================================================
    // Volume Key Toggle (Air Mouse activation)
    // ============================================================
 
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (prefs.getBoolean("edge_gestures_enabled", false)) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                executeEdgeAction(prefs.getString("edge_gestures_volume_up", "LEFT_CLICK"))
                return true
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                executeEdgeAction(prefs.getString("edge_gestures_volume_down", "RIGHT_CLICK"))
                return true
            }
        }

        if (prefs.isVolumeKeyToggleEnabled()) {
            val activeMode = prefs.getActiveMode()
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                if (activeMode.moveSource == "touchpad") {
                    connectionManager.isMouseControlEnabled = true
                } else {
                    viewModel.startAirMouse()
                    connectionManager.isMouseControlEnabled = true
                }
                return true // Consume the key event
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                if (activeMode.moveSource == "touchpad") {
                    connectionManager.isMouseControlEnabled = false
                } else {
                    viewModel.stopAirMouse()
                    connectionManager.isMouseControlEnabled = false
                }
                return true // Consume the key event
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun executeEdgeAction(actionName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                when (actionName) {
                    "LEFT_CLICK" -> connectionManager.sendClick("left")
                    "RIGHT_CLICK" -> connectionManager.sendClick("right")
                    "DOUBLE_CLICK" -> connectionManager.sendDoubleClick()
                    "SCROLL_UP" -> connectionManager.sendScroll(3)
                    "SCROLL_DOWN" -> connectionManager.sendScroll(-3)
                    "VOLUME_UP" -> connectionManager.sendControl("volume_up")
                    "VOLUME_DOWN" -> connectionManager.sendControl("volume_down")
                    "PREV_TRACK" -> connectionManager.sendControl("prev_track")
                    "NEXT_TRACK" -> connectionManager.sendControl("next_track")
                    "PLAY_PAUSE" -> connectionManager.sendControl("play_pause")
                    "LOCK_SCREEN" -> connectionManager.sendControl("lock_screen")
                    "SHOW_DESKTOP" -> connectionManager.sendControl("show_desktop")
                    "TASK_VIEW" -> connectionManager.sendControl("task_view")
                }
            } catch (e: Exception) {
                android.util.Log.e("EdgeGestures", "Error executing gesture action: $actionName", e)
            }
        }
    }

    // ============================================================
    // Permission Handling
    // ============================================================

    private fun requestPermissions() {
        val missingPerms = getMissingPermissions()
        if (missingPerms.isEmpty()) {
            permissionGranted = true
            proceedToMain()
            return
        }

        when {
            missingPerms.any { it == Manifest.permission.CAMERA } -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            missingPerms.any { it.startsWith("android.permission.BLUETOOTH") } -> {
                bluetoothPermissionLauncher.launch(missingPerms.toTypedArray())
            }
            else -> {
                generalPermissionLauncher.launch(missingPerms.toTypedArray())
            }
        }
    }

    private fun getMissingPermissions(): List<String> {
        val perms = mutableListOf<String>()

        // Camera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.CAMERA)
        }

        // Bluetooth (Android 12+ split permissions)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE)
                != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            }
        } else {
            // Legacy Bluetooth permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.BLUETOOTH)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
                != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.BLUETOOTH_ADMIN)
            }
            // Location required for Bluetooth scanning on older Android
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }

        // Microphone (voice commands)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.RECORD_AUDIO)
        }

        // Notifications (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Body sensors (Android 12+ for heart rate, etc.)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
                != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.BODY_SENSORS)
            }
        }

        // Activity recognition (Android 10+ / Q for pedometer/step sensors)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }

        return perms
    }

    private fun showRationaleDialog() {
        setContent {
            val isDarkTheme = when (prefs.getString("theme", "system")) {
                "dark", "pure_black" -> true
                "light" -> false
                "high_contrast" -> false
                else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            }

            AirMouseTheme(
                darkTheme = isDarkTheme,
                useDynamicColor = prefs.getBoolean("dynamic_colors", true)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PermissionRationaleScreen(
                        onRetry = {
                            permissionGranted = false
                            requestPermissions()
                        },
                        onContinue = {
                            permissionGranted = true
                            proceedToMain()
                        }
                    )
                }
            }
        }
    }

    // ============================================================
    // Launch Main Screen
    // ============================================================

    private fun proceedToMain() {
        if (isReady) return
        isReady = true

        setContent {
            // Read theme and accent preferences reactively.
            val sharedPrefs = remember { getSharedPreferences("airmouse_prefs", Context.MODE_PRIVATE) }
            var themeId by remember { mutableStateOf(sharedPrefs.getString("theme", "system") ?: "system") }
            var accentName by remember { mutableStateOf(sharedPrefs.getString("accent_color", "ORANGE") ?: "ORANGE") }
            var dynamicColors by remember { mutableStateOf(sharedPrefs.getBoolean("dynamic_colors", true)) }
            var fontSize by remember { mutableFloatStateOf(sharedPrefs.getFloat("font_size", 16f)) }

            DisposableEffect(sharedPrefs) {
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    when (key) {
                        "theme" -> themeId = sharedPrefs.getString("theme", "system") ?: "system"
                        "accent_color" -> accentName = sharedPrefs.getString("accent_color", "ORANGE") ?: "ORANGE"
                        "dynamic_colors" -> dynamicColors = sharedPrefs.getBoolean("dynamic_colors", true)
                        "font_size" -> fontSize = sharedPrefs.getFloat("font_size", 16f)
                    }
                }
                sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose {
                    sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }

            val accentColor = try {
                AccentColor.valueOf(accentName)
            } catch (e: Exception) {
                AccentColor.ORANGE
            }

            val isDarkTheme = when (themeId) {
                "light" -> false
                "high_contrast" -> false
                else -> true
            }

            val themeColors = remember(themeId, accentColor) {
                getThemeColorScheme(themeId, accentColor)
            }

            LaunchedEffect(themeId) {
                updateSystemBarsForTheme(themeId)
            }

            ProvideThemeColors(themeColors) {
                AirMouseTheme(
                    darkTheme = isDarkTheme,
                    useDynamicColor = dynamicColors,
                    themeColors = themeColors,
                    fontScale = (fontSize / 16f).coerceIn(0.85f, 1.20f)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = themeColors.background
                    ) {
                        val startDest = Destinations.Splash.route
                        MainScreen(
                            startDestination = startDest,
                            pendingRoute = pendingRoute,
                            onRouteHandled = { pendingRoute = null }
                        )
                    }
                }
            }
        }
    }

    /**
     * Configures the window for edge‑to‑edge display and transparent system bars.
     */
    private fun setupWindow() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        }
        updateSystemBarsColor()
    }

    /**
     * Updates the system bar appearance (light/dark icons) based on the current theme.
     */
    private fun updateSystemBarsColor() {
        val themeId = prefs.getString("theme", "system")
        updateSystemBarsForTheme(themeId)
    }

    /**
     * Updates system bars for a specific theme.
     * @param themeId The theme identifier (e.g., "light", "dark", "system", "high_contrast").
     */
    private fun updateSystemBarsForTheme(themeId: String) {
        val isDark = when (themeId) {
            "light" -> false
            "high_contrast" -> false
            else -> true
        }

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = !isDark
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            controller.isAppearanceLightNavigationBars = !isDark
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val accentName = prefs.getString("accent_color", "ORANGE")
            val accentColor = try {
                AccentColor.valueOf(accentName)
            } catch (e: Exception) {
                AccentColor.ORANGE
            }

            val colorScheme = getThemeColorScheme(themeId, accentColor)
            window.navigationBarColor = colorScheme.background.toArgb()
            window.statusBarColor = Color.TRANSPARENT
        }
    }

    // ============================================================
    // Permission Rationale Screen (Composable)
    // ============================================================

    @Composable
    fun PermissionRationaleScreen(onRetry: () -> Unit, onContinue: () -> Unit) {
        var visible by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            delay(300)
            visible = true
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(animationSpec = tween(500)) +
                            scaleIn(initialScale = 0.8f, animationSpec = spring())
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Permissions",
                            modifier = Modifier
                                .size(80.dp)
                                .scale(1f),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Permissions Required",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Air Mouse needs the following permissions to work properly:",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Permission list
                        PermissionItem(
                            icon = "📷",
                            name = "Camera",
                            description = "Scan QR codes for quick pairing"
                        )
                        PermissionItem(
                            icon = "🔵",
                            name = "Bluetooth",
                            description = "Detect proximity and act as a wireless mouse"
                        )
                        PermissionItem(
                            icon = "🎤",
                            name = "Microphone",
                            description = "Voice commands for hands-free control"
                        )
                        PermissionItem(
                            icon = "🔔",
                            name = "Notifications",
                            description = "Keep you informed about connection status"
                        )
                        PermissionItem(
                            icon = "📍",
                            name = "Location",
                            description = "Required for Bluetooth scanning (Android < 12)"
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = onContinue,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Continue Anyway")
                            }

                            Button(
                                onClick = onRetry,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Grant Permissions")
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun PermissionItem(icon: String, name: String, description: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                fontSize = MaterialTheme.typography.headlineSmall.fontSize
            )
            Column {
                Text(
                    text = name,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

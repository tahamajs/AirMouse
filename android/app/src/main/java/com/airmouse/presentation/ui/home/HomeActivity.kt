package com.airmouse.presentation.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.airmouse.presentation.theme.AirMouseTheme
import com.airmouse.presentation.ui.main.MainScreen
import com.airmouse.network.ConnectionManager
import com.airmouse.sensors.SensorService
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivity : ComponentActivity() {

    // ============================================================
    // Dependencies
    // ============================================================

    @Inject
    lateinit var prefs: PreferencesManager

    private val viewModel: HomeViewModel by viewModels()

    @Inject
    lateinit var sensorService: SensorService

    @Inject
    lateinit var connectionManager: ConnectionManager

    // ============================================================
    // State
    // ============================================================

    private var isReady = false
    private var permissionGranted = false
    private var isInitialLaunch = true

    // ============================================================
    // Permission Launchers
    // ============================================================

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
        if (isGranted) {
            proceedToMain()
        } else {
            showRationaleDialog()
        }
    }

    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionGranted = permissions.values.all { it }
        if (permissionGranted) {
            proceedToMain()
        } else {
            showRationaleDialog()
        }
    }

    private val generalPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionGranted = permissions.values.all { it }
        if (permissionGranted) {
            proceedToMain()
        } else {
            showRationaleDialog()
        }
    }

    // ============================================================
    // Lifecycle
    // ============================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !isReady }

        super.onCreate(savedInstanceState)

        // First launch flag
        isInitialLaunch = prefs.getBoolean("is_first_launch", true)
        if (isInitialLaunch) {
            prefs.putBoolean("is_first_launch", false)
        }

        // Setup sensor callback – sends movement data to the server
        sensorService.onOrientationChanged = { roll, yaw ->
            val sensitivity = prefs.getSensitivity()
            val dx = yaw * sensitivity * 0.5f
            val dy = roll * sensitivity * 0.5f
            Log.d("HomeActivity", "Orientation callback -> sendMove dx=$dx dy=$dy")
            lifecycleScope.launch(Dispatchers.IO) {
                connectionManager.sendMove(dx, dy)
            }
        }
        sensorService.start()

        // Apply theme (affects the splash screen and UI)
        applyTheme()

        // Check permissions
        val missingPerms = getMissingPermissions()
        if (missingPerms.isEmpty()) {
            permissionGranted = true
            proceedToMain()
        } else {
            requestPermissions()
        }

        // Safety timeout – if permissions are granted but UI didn't load
        Handler(Looper.getMainLooper()).postDelayed({
            if (permissionGranted && !isReady) {
                proceedToMain()
            }
        }, 1000)
    }

    override fun onResume() {
        super.onResume()
        // If permissions were granted while app was in background, proceed
        if (!isReady && getMissingPermissions().isEmpty()) {
            permissionGranted = true
            proceedToMain()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyTheme()
    }

    override fun onDestroy() {
        sensorService.stop()
        super.onDestroy()
    }

    // ============================================================
    // Volume Key Toggle (Air Mouse activation)
    // ============================================================

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (prefs.isVolumeKeyToggleEnabled()) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                // Toggle Air Mouse activation
                viewModel.toggleAirMouse()
                return true // Consume the key event (prevents volume change)
            }
        }
        return super.onKeyDown(keyCode, event)
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
                    MainScreen()
                }
            }
        }
    }

    // ============================================================
    // Theme
    // ============================================================

    private fun applyTheme() {
        val theme = prefs.getString("theme", "dark")
        when (theme) {
            "light" -> setTheme(com.airmouse.R.style.Theme_AirMouse_Light)
            "dark" -> setTheme(com.airmouse.R.style.Theme_AirMouse_Dark)
            "pure_black" -> setTheme(com.airmouse.R.style.Theme_AirMouse_PureBlack)
            "high_contrast" -> setTheme(com.airmouse.R.style.Theme_AirMouse_HighContrast)
            else -> setTheme(com.airmouse.R.style.Theme_AirMouse)
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
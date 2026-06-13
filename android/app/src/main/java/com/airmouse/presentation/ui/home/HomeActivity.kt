package com.airmouse.presentation.ui.home

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.airmouse.presentation.theme.AirMouseTheme
import com.airmouse.presentation.ui.main.MainScreen
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivity : ComponentActivity() {

    @Inject
    lateinit var prefs: PreferencesManager

    private var isReady = false
    private var permissionGranted = false

    // Permission launchers
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
        if (isGranted) proceedToMain() else showRationaleDialog()
    }

    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionGranted = permissions.values.all { it }
        if (permissionGranted) proceedToMain() else showRationaleDialog()
    }

    private val generalPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionGranted = permissions.values.all { it }
        if (permissionGranted) proceedToMain() else showRationaleDialog()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !isReady }

        super.onCreate(savedInstanceState)

        applyTheme()
        requestPermissions()

        // Simulate a short loading time for splash screen
        Handler(Looper.getMainLooper()).postDelayed({
            if (permissionGranted) proceedToMain()
        }, 1000)
    }

    private fun proceedToMain() {
        if (isReady) return
        isReady = true
        setContent {
            AirMouseTheme(
                theme = prefs.getTheme(),
                useDynamicColor = true,
                darkTheme = prefs.getTheme() == "system"
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

    private fun applyTheme() {
        val theme = prefs.getTheme()
        when (theme) {
            "light" -> setTheme(com.airmouse.R.style.Theme_AirMouse_Light)
            "dark" -> setTheme(com.airmouse.R.style.Theme_AirMouse_Dark)
            "pure_black" -> setTheme(com.airmouse.R.style.Theme_AirMouse_PureBlack)
            "high_contrast" -> setTheme(com.airmouse.R.style.Theme_AirMouse_HighContrast)
            else -> setTheme(com.airmouse.R.style.Theme_AirMouse)
        }
    }

    private fun requestPermissions() {
        val missingPerms = getMissingPermissions()
        if (missingPerms.isEmpty()) {
            permissionGranted = true
            return
        }

        when {
            missingPerms.first().startsWith("android.permission.CAMERA") -> {
                cameraPermissionLauncher.launch(missingPerms.first())
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.CAMERA)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.BLUETOOTH)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
                != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.BLUETOOTH_ADMIN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.RECORD_AUDIO)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
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
            AirMouseTheme(
                theme = prefs.getTheme(),
                useDynamicColor = true,
                darkTheme = prefs.getTheme() == "system"
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PermissionRationaleScreen(
                        onRetry = { requestPermissions() },
                        onContinue = { proceedToMain() }
                    )
                }
            }
        }
    }

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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                            text = "Air Mouse needs the following permissions to work properly:\n\n" +
                                    "• Camera – to scan QR codes\n" +
                                    "• Bluetooth – to detect proximity and act as a mouse\n" +
                                    "• Microphone – for voice commands\n" +
                                    "• Notifications – to keep you informed\n" +
                                    "• Body Sensors – for step detection (optional)",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
}
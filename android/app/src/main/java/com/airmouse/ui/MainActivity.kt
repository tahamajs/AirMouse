package com.airmouse.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.airmouse.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))
        drawerLayout = findViewById(R.id.drawer_layout)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        findViewById<BottomNavigationView>(R.id.bottom_nav).setupWithNavController(navController)
        findViewById<NavigationView>(R.id.nav_view).setupWithNavController(navController)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment,
                R.id.calibrationFragment,
                R.id.networkDiscoveryFragment,
                R.id.serverLogFragment,
                R.id.profilesFragment,
                R.id.voiceCommandFragment
            ),
            drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}


// MainActivity.kt - additions
import com.airmouse.gesture.GestureInferenceService
import com.airmouse.network.WebSocketManager

class MainActivity : AppCompatActivity() {
    private var inferenceService: GestureInferenceService? = null
    private var isInferenceBound = false
    private var serverWsUrl = "ws://192.168.1.10:8081"  // Set from config

    private val inferenceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            inferenceService = (service as GestureInferenceService.LocalBinder).getService()
            isInferenceBound = true
            inferenceService?.onGestureDetected = { gesture, confidence ->
                Log.d("Gesture", "Detected: $gesture ($confidence)")
                WebSocketManager.sendGesture(gesture, confidence)
                // Also optionally vibrate or show toast
                if (confidence > 0.8f) {
                    vibrate(50)
                }
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            inferenceService = null
            isInferenceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ... existing code ...
        // Start WebSocket connection (to PC server)
        val ip = preferences.getLastIp()
        val port = preferences.getLastPort()
        serverWsUrl = "ws://$ip:${port+1}"  // assuming WebSocket port = TCP port + 1
        WebSocketManager.connect(serverWsUrl)
        // Start gesture inference service
        val inferenceIntent = Intent(this, GestureInferenceService::class.java)
        startService(inferenceIntent)
        bindService(inferenceIntent, inferenceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        if (isInferenceBound) unbindService(inferenceConnection)
        WebSocketManager.disconnect()
        super.onDestroy()
    }

    private fun vibrate(duration: Long) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(duration, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }
}


// MainActivity.kt – add to existing class
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.airmouse.gesture.GestureInferenceService
import com.airmouse.network.WebSocketManager
import com.airmouse.proximity.ProximityAwareService

class MainActivity : AppCompatActivity() {

    // ... existing code ...

    // Gesture inference
    private var inferenceService: GestureInferenceService? = null
    private var isInferenceBound = false

    // Proximity service
    private var proximityIntent: Intent? = null

    private val inferenceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            inferenceService = (service as GestureInferenceService.LocalBinder).getService()
            isInferenceBound = true
            inferenceService?.onGestureDetected = { gesture, confidence ->
                Log.d("Gesture", "Detected: $gesture ($confidence)")
                WebSocketManager.sendGesture(gesture, confidence)
                if (confidence > 0.8f && preferences.isHapticEnabled()) {
                    vibrate(50)
                }
            }
            inferenceService?.start()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            inferenceService = null
            isInferenceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ... existing setup ...

        // WebSocket connection
        val ip = preferences.getLastIp()
        val port = preferences.getLastPort() + 1  // WebSocket port = TCP+1
        WebSocketManager.connect("ws://$ip:$port")
        WebSocketManager.onConnected = {
            runOnUiThread { statusText.text = "WebSocket connected" }
        }
        WebSocketManager.onDisconnected = {
            runOnUiThread { statusText.text = "WebSocket disconnected" }
        }

        // Start & bind gesture inference service
        val inferenceIntent = Intent(this, GestureInferenceService::class.java)
        startService(inferenceIntent)
        bindService(inferenceIntent, inferenceConnection, Context.BIND_AUTO_CREATE)

        // Proximity service (start on demand, e.g., from settings)
        proximityIntent = Intent(this, ProximityAwareService::class.java)
    }

    override fun onDestroy() {
        if (isInferenceBound) unbindService(inferenceConnection)
        WebSocketManager.disconnect()
        stopService(proximityIntent)
        super.onDestroy()
    }

    private fun vibrate(duration: Long) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(duration, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }
}


// In MainActivity.onCreate()
PreferencesHelper.init(applicationContext)
if (PreferencesHelper.isAutoPauseEnabled()) {
    startService(Intent(this, OrientationMonitorService::class.java))
}
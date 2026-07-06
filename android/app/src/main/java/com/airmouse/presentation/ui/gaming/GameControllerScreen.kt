package com.airmouse.presentation.ui.gaming

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airmouse.presentation.navigation.NavigationActions
import com.airmouse.presentation.ui.components.PremiumDashboardBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ControllerMode {
    STEERING_WHEEL,
    SHOOTER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameControllerScreen(
    navigationActions: NavigationActions,
    viewModel: GameProfilesViewModel = hiltViewModel()
) {
    var currentMode by remember { mutableStateOf(ControllerMode.STEERING_WHEEL) }
    
    PremiumDashboardBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Game Controller", color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navigationActions.navigateBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        Row(modifier = Modifier.padding(end = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { currentMode = ControllerMode.STEERING_WHEEL }) {
                                Icon(
                                    Icons.Default.DirectionsCar, 
                                    contentDescription = "Steering", 
                                    tint = if (currentMode == ControllerMode.STEERING_WHEEL) Color(0xFF38BDF8) else Color.White.copy(alpha=0.5f)
                                )
                            }
                            IconButton(onClick = { currentMode = ControllerMode.SHOOTER }) {
                                Icon(
                                    Icons.Default.CrisisAlert, 
                                    contentDescription = "Shooter", 
                                    tint = if (currentMode == ControllerMode.SHOOTER) Color(0xFFEF4444) else Color.White.copy(alpha=0.5f)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (currentMode == ControllerMode.STEERING_WHEEL) {
                    SteeringWheelLayout()
                } else {
                    ShooterLayout()
                }
            }
        }
    }
}

@Composable
fun SteeringWheelLayout() {
    val context = LocalContext.current
    var steeringAngle by remember { mutableFloatStateOf(0f) }
    
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    // For steering wheel, we care about the X axis tilt when phone is horizontal landscape
                    val x = it.values[0]
                    val y = it.values[1]
                    // Calculate rough angle based on accelerometer
                    val angle = Math.toDegrees(Math.atan2(y.toDouble(), x.toDouble())).toFloat()
                    steeringAngle = angle
                    // Here we would dispatch the steering event to the server
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }
    
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(32.dp)) {
            Text("Steering Wheel Active", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            
            // Visual wheel indicator
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha=0.05f))
                    .border(4.dp, Color(0xFF38BDF8).copy(alpha=0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.DirectionsCar, 
                    contentDescription = null, 
                    tint = Color.White.copy(alpha=0.8f),
                    modifier = Modifier.size(80.dp).rotate(steeringAngle)
                )
            }
            
            Text("Angle: ${steeringAngle.toInt()}°", color = Color.White.copy(alpha=0.7f), fontSize = 18.sp)
        }
    }
}

@Composable
fun ShooterLayout() {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left half: movement joystick area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha=0.02f))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                // Simulate joystick press down
                                tryAwaitRelease()
                                // Simulate joystick release
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha=0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Movement", color = Color.White.copy(alpha=0.4f), fontWeight = FontWeight.Bold)
                }
            }
            
            // Right half: aim/shoot area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha=0.05f))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                // Trigger shoot action
                                tryAwaitRelease()
                                // Stop shoot action
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444).copy(alpha=0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CrisisAlert, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(48.dp))
                }
            }
        }
    }
}

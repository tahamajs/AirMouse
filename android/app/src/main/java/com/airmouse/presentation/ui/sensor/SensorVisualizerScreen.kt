package com.airmouse.presentation.ui.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SensorVisualizerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SensorVisualizerScreen() }
    }
}

@Composable
fun SensorVisualizerScreen() {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(SensorManager::class.java) }
    var roll by remember { mutableStateOf(0f) }
    var pitch by remember { mutableStateOf(0f) }
    var yaw by remember { mutableStateOf(0f) }

    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_GAME_ROTATION_VECTOR) {
                    val rotMat = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotMat, event.values)
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotMat, orientation)
                    roll = orientation[2]  // rotation around X
                    pitch = orientation[1] // rotation around Y
                    yaw = orientation[0]   // rotation around Z
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        SensorCubeView(roll = roll, pitch = pitch, yaw = yaw, modifier = Modifier.size(250.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Roll: ${"%.1f".format(Math.toDegrees(roll.toDouble()))}°")
        Text("Pitch: ${"%.1f".format(Math.toDegrees(pitch.toDouble()))}°")
        Text("Yaw: ${"%.1f".format(Math.toDegrees(yaw.toDouble()))}°")
    }
}

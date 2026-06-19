// app/src/main/java/com/airmouse/presentation/ui/calibration/CalibrationGuideDialog.kt
package com.airmouse.presentation.ui.calibration

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.airmouse.R
import kotlinx.coroutines.delay

@Composable
fun CalibrationGuideDialog(
    step: Int,
    onDismiss: () -> Unit
) {
    var currentImageIndex by remember { mutableIntStateOf(0) }
    val totalImages = when (step) {
        0 -> 3
        1 -> 4
        2 -> 6
        else -> 1
    }

    LaunchedEffect(step) {
        while (true) {
            delay(2000)
            currentImageIndex = (currentImageIndex + 1) % totalImages
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated instruction icon
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(Color(0xFF0F172A), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    when (step) {
                        0 -> GyroInstructionAnimation(currentImageIndex)
                        1 -> MagnetometerInstructionAnimation(currentImageIndex)
                        2 -> AccelerometerInstructionAnimation(currentImageIndex)
                        else -> Image(painterResource(R.drawable.ic_check), contentDescription = null)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = when (step) {
                        0 -> "Place device on a flat, stationary surface"
                        1 -> "Move device in a smooth figure-8 pattern"
                        2 -> "Rotate device to each shown orientation"
                        else -> "Calibration complete!"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = when (step) {
                        0 -> "Keep the device perfectly still during calibration"
                        1 -> "Make sure to cover all axes of movement"
                        2 -> "Hold each position steady for 3 seconds"
                        else -> "Your device is now calibrated for optimal performance"
                    },
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6366F1)
                    )
                ) {
                    Text("Got it", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun GyroInstructionAnimation(frame: Int) {
    val rotation = when (frame % 3) {
        0 -> 0f
        1 -> 5f
        else -> -5f
    }

    Box(
        modifier = Modifier
            .size(120.dp)
            .rotate(rotation)
            .background(Color(0xFF020617), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📱", fontSize = 48.sp)
            Text("Flat Surface", fontSize = 12.sp, color = Color(0xFF64748B))
        }
    }
}

@Composable
fun MagnetometerInstructionAnimation(frame: Int) {
    val angle = (frame * 45f) % 360f

    Box(
        modifier = Modifier
            .size(120.dp)
            .background(Color(0xFF020617), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("∞", fontSize = 48.sp, modifier = Modifier.rotate(angle))
            Text("Figure-8 Pattern", fontSize = 12.sp, color = Color(0xFF64748B))
        }
    }
}

@Composable
fun AccelerometerInstructionAnimation(frame: Int) {
    val orientations = listOf("⬆️", "⬇️", "⬅️", "➡️", "🔄", "↩️")
    val currentOrientation = orientations[frame % orientations.size]

    Box(
        modifier = Modifier
            .size(120.dp)
            .background(Color(0xFF020617), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(currentOrientation, fontSize = 48.sp)
            Text("Rotate Device", fontSize = 12.sp, color = Color(0xFF64748B))
        }
    }
}

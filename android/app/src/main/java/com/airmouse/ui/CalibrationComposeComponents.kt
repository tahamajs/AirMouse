package com.airmouse.ui

import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CalibrationHeader(status: String, overallProgress: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color(0xFF0F1115))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = status, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(progress = (overallProgress / 100f).coerceIn(0f, 1f), modifier = Modifier
            .fillMaxWidth()
            .height(12.dp), color = Color(0xFF007ACC))
    }
}

@Composable
fun CalibrationBottomControls(timerText: String, onBack: () -> Unit, onNext: () -> Unit, onStop: () -> Unit, backEnabled: Boolean, nextEnabled: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color(0xFF0F1115))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = timerText, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
            Button(onClick = onBack, enabled = backEnabled, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2B3341))) {
                Text("Back", color = Color(0xFFE5E7EB))
            }
            Button(onClick = onNext, enabled = nextEnabled, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF007ACC))) {
                Text("Next", color = Color.White)
            }
            Button(onClick = onStop, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFEF5B5B))) {
                Text("Stop", color = Color.White)
            }
        }
    }
}

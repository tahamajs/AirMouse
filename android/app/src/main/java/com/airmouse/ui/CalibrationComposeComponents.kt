package com.airmouse.ui

import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import androidx.compose.ui.res.stringResource
import com.airmouse.R
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer

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
        // Smooth animated progress
        val target = (overallProgress / 100f).coerceIn(0f, 1f)
        val animated = animateFloatAsState(targetValue = target)
        LinearProgressIndicator(progress = animated.value, modifier = Modifier
            .fillMaxWidth()
            .height(12.dp), color = Color(0xFF007ACC))
        Spacer(modifier = Modifier.height(8.dp))
        // Optional Lottie success indicator placeholder (will show when progress==100)
        AnimatedVisibility(visible = overallProgress >= 100, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
            // load from assets if provided: assets/lottie_success.json
            val composition by rememberLottieComposition(LottieCompositionSpec.Asset("lottie_success.json"))
            val progress by animateLottieCompositionAsState(composition)
            LottieAnimation(composition, progress, modifier = Modifier.size(64.dp))
        }
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
        // Animating timer changes
            val timerState = remember { mutableStateOf(timerText) }
            LaunchedEffect(timerText) { timerState.value = timerText }
            val timerAnimatedAlpha by animateFloatAsState(targetValue = 1f)
            Text(text = timerState.value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.alpha(timerAnimatedAlpha))
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
            Button(onClick = onBack, enabled = backEnabled, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2B3341))) {
                Text("Back", color = Color(0xFFE5E7EB))
            }
            // Next button with subtle scale animation when enabled
            val nextScale by animateFloatAsState(targetValue = if (nextEnabled) 1f else 0.98f)
            Button(onClick = onNext, enabled = nextEnabled, modifier = Modifier.weight(1f).graphicsLayer(scaleX = nextScale, scaleY = nextScale), colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF007ACC))) {
                Text("Next", color = Color.White)
            }
            Button(onClick = onStop, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFEF5B5B))) {
                Text("Stop", color = Color.White)
            }
        }
    }
}

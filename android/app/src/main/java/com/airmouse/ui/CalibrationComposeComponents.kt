package com.airmouse.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

@Composable
fun CalibrationHeader(status: String, overallProgress: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CalibrationUiTokens.ScreenBg)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Crossfade(targetState = status, label = "calibration_status") { currentStatus ->
            Text(
                text = currentStatus,
                color = CalibrationUiTokens.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        val target = (overallProgress / 100f).coerceIn(0f, 1f)
        val animatedProgress by animateFloatAsState(targetValue = target)
        LinearProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
            color = CalibrationUiTokens.Accent,
            backgroundColor = CalibrationUiTokens.CardStroke
        )

        Spacer(modifier = Modifier.height(10.dp))

        val chipColor by animateColorAsState(
            targetValue = if (overallProgress >= 100) CalibrationUiTokens.Success else CalibrationUiTokens.TextSecondary
        )
        Surface(
            color = CalibrationUiTokens.CardBg,
            shape = CalibrationUiTokens.ButtonShape,
            border = androidx.compose.foundation.BorderStroke(1.dp, CalibrationUiTokens.CardStroke)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (overallProgress >= 100) "Ready for next step" else "Calibrating…",
                    color = chipColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                AnimatedVisibility(
                    visible = overallProgress >= 100,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("lottie_success.json"))
                    val progress by animateLottieCompositionAsState(composition)
                    if (composition != null) {
                        LottieAnimation(
                            composition = composition,
                            progress = { progress },
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Text("✓", color = CalibrationUiTokens.Success, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CalibrationBottomControls(timerText: String, onBack: () -> Unit, onNext: () -> Unit, onStop: () -> Unit, backEnabled: Boolean, nextEnabled: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CalibrationUiTokens.ScreenBg)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val timerState by rememberUpdatedState(timerText)
        val timerAlpha by animateFloatAsState(targetValue = 1f)
        Text(
            text = timerState,
            color = CalibrationUiTokens.TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.alpha(timerAlpha)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            ActionButton(
                label = "Back",
                enabled = backEnabled,
                background = CalibrationUiTokens.CardBg,
                content = CalibrationUiTokens.TextPrimary,
                onClick = onBack
            )

            val nextScale by animateFloatAsState(targetValue = if (nextEnabled) 1f else 0.98f)
            ActionButton(
                label = "Next",
                enabled = nextEnabled,
                background = CalibrationUiTokens.Accent,
                content = CalibrationUiTokens.TextPrimary,
                onClick = onNext,
                modifier = Modifier.graphicsLayer(scaleX = nextScale, scaleY = nextScale)
            )

            ActionButton(
                label = "Stop",
                enabled = true,
                background = CalibrationUiTokens.Danger,
                content = CalibrationUiTokens.TextPrimary,
                onClick = onStop
            )
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    enabled: Boolean,
    background: Color,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = CalibrationUiTokens.ButtonShape,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = background,
            contentColor = content,
            disabledBackgroundColor = CalibrationUiTokens.CardStroke,
            disabledContentColor = CalibrationUiTokens.TextSecondary
        )
    ) {
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}

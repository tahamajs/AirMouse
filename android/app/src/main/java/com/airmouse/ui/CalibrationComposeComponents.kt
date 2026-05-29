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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
fun CalibrationStepScaffold(
    stepLabel: String,
    title: String,
    subtitle: String,
    status: String,
    overallProgress: Int,
    timerText: String,
    instruction: String,
    tip: String,
    nextEnabled: Boolean,
    backEnabled: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit,
    illustration: @Composable BoxScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CalibrationUiTokens.ScreenBg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Surface(
            color = CalibrationUiTokens.CardBg,
            shape = CalibrationUiTokens.CardShape,
            border = BorderStroke(1.dp, CalibrationUiTokens.CardStroke),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                StepHeader(label = stepLabel, title = title, subtitle = subtitle)
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(CalibrationUiTokens.ScreenBg, CalibrationUiTokens.CardShape)
                        .border(BorderStroke(1.dp, CalibrationUiTokens.CardStroke), CalibrationUiTokens.CardShape),
                    contentAlignment = Alignment.Center,
                    content = illustration
                )
                Spacer(modifier = Modifier.height(14.dp))
                CalibrationInstructionCard(
                    status = status,
                    instruction = instruction,
                    tip = tip,
                    progress = overallProgress
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        CalibrationHeader(status = status, overallProgress = overallProgress)
        CalibrationBottomControls(
            timerText = timerText,
            onBack = onBack,
            onNext = onNext,
            onStop = onStop,
            backEnabled = backEnabled,
            nextEnabled = nextEnabled
        )
    }
}

@Composable
private fun StepHeader(label: String, title: String, subtitle: String) {
    Column {
        Surface(
            color = CalibrationUiTokens.Accent.copy(alpha = 0.15f),
            shape = CalibrationUiTokens.ButtonShape,
            border = BorderStroke(1.dp, CalibrationUiTokens.Accent.copy(alpha = 0.35f))
        ) {
            Text(
                text = label,
                color = CalibrationUiTokens.Accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(title, color = CalibrationUiTokens.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(subtitle, color = CalibrationUiTokens.TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun CalibrationInstructionCard(status: String, instruction: String, tip: String, progress: Int) {
    Surface(
        color = CalibrationUiTokens.ScreenBg,
        shape = CalibrationUiTokens.ButtonShape,
        border = BorderStroke(1.dp, CalibrationUiTokens.CardStroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Crossfade(targetState = status, label = "status_crossfade") { currentStatus ->
                Text(currentStatus, color = CalibrationUiTokens.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(instruction, color = CalibrationUiTokens.TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = (progress / 100f).coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = CalibrationUiTokens.Accent,
                backgroundColor = CalibrationUiTokens.CardStroke
            )
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = CalibrationUiTokens.CardBg,
                shape = CalibrationUiTokens.ButtonShape,
                border = BorderStroke(1.dp, CalibrationUiTokens.CardStroke)
            ) {
                Text(
                    text = tip,
                    color = CalibrationUiTokens.TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

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

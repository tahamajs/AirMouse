package com.airmouse.calibration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.airmouse.ui.CalibrationStepScaffold
import com.airmouse.ui.CalibrationUiTokens

class AccelComposeFragment : Fragment(), CalibrationStepFragment {
    private lateinit var viewModel: AccelCalibrationViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory(requireActivity().application))
            .get(AccelCalibrationViewModel::class.java)

        return ComposeView(requireContext()).apply {
            setContent {
                val progress by viewModel.progress.collectAsState()
                val status by viewModel.status.collectAsState()
                val timer by viewModel.timerText.collectAsState()
                val pulse by animateFloatAsState(targetValue = if (progress in 1..99) 1.03f else 1f, label = "accelPulse")

                LaunchedEffect(Unit) {
                    if (progress == 0) viewModel.startCollection()
                }

                CalibrationStepScaffold(
                    stepLabel = "STEP 2 • ORIENTATION MAP",
                    title = "Accelerometer calibration",
                    subtitle = "Map the six phone orientations so gravity is read correctly.",
                    status = status,
                    overallProgress = progress,
                    timerText = timer,
                    instruction = "Rotate the phone to the requested positions and keep each pose steady until the sample ring completes.",
                    tip = "Tip: move slowly between poses — the cleaner the hold, the cleaner the calibration.",
                    nextEnabled = progress >= 100,
                    backEnabled = true,
                    onBack = { (activity as? com.airmouse.ui.CalibrationActivity)?.goToPreviousStepFromUi() },
                    onNext = { (activity as? com.airmouse.ui.CalibrationActivity)?.goToNextStepFromUi() },
                    onStop = { (activity as? com.airmouse.ui.CalibrationActivity)?.abortFromUi() }
                ) {
                    Surface(
                        color = CalibrationUiTokens.CardBg,
                        shape = CalibrationUiTokens.CardShape,
                        border = BorderStroke(1.dp, CalibrationUiTokens.CardStroke),
                        modifier = Modifier
                            .size(width = 180.dp, height = 180.dp)
                            .graphicsLayer(scaleX = pulse, scaleY = pulse)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("ACCEL", color = CalibrationUiTokens.TextPrimary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Flip through poses", color = CalibrationUiTokens.TextSecondary)
                        }
                    }
                }
            }
        }
    }

    override fun isStepComplete(): Boolean = viewModel.isComplete()
    override fun resetUI() {
        viewModel.reset()
        viewModel.startCollection()
    }
    override fun saveCalibrationData() = Unit
    override fun getProgress(): Int = viewModel.progress.value
    override fun isDataValid(): Boolean = viewModel.isComplete()
}

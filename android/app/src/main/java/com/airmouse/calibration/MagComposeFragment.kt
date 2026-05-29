package com.airmouse.calibration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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

class MagComposeFragment : Fragment(), CalibrationStepFragment {
    private lateinit var viewModel: MagCalibrationViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory(requireActivity().application))
            .get(MagCalibrationViewModel::class.java)

        return ComposeView(requireContext()).apply {
            setContent {
                val progress by viewModel.progress.collectAsState()
                val status by viewModel.status.collectAsState()
                val timer by viewModel.timerText.collectAsState()
                val pulse by animateFloatAsState(targetValue = if (progress in 1..99) 1.02f else 1f, label = "magPulse")

                LaunchedEffect(Unit) {
                    if (progress == 0) viewModel.startCollection()
                }

                CalibrationStepScaffold(
                    stepLabel = "STEP 3 • FIELD MAPPING",
                    title = "Magnetometer calibration",
                    subtitle = "Capture the surrounding field so heading and rotation feel natural.",
                    status = status,
                    overallProgress = progress,
                    timerText = timer,
                    instruction = "Move the phone in a smooth figure‑8 pattern until the field map fills out.",
                    tip = "Tip: keep the motion slow and wide to avoid clipping the field samples.",
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
                            .size(width = 190.dp, height = 190.dp)
                            .graphicsLayer(scaleX = pulse, scaleY = pulse)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("MAG", color = CalibrationUiTokens.TextPrimary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Figure‑8 motion", color = CalibrationUiTokens.TextSecondary)
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

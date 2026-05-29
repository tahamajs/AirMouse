package com.airmouse.calibration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.airmouse.ui.CalibrationBottomControls
import com.airmouse.ui.CalibrationHeader
import androidx.compose.runtime.LaunchedEffect

class AccelComposeFragment : Fragment(), CalibrationStepFragment {
    private lateinit var viewModel: AccelCalibrationViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory(requireActivity().application)).get(AccelCalibrationViewModel::class.java)
        val composeView = ComposeView(requireContext())
        composeView.setContent {
            val progress by viewModel.progress.collectAsState()
            val status by viewModel.status.collectAsState()
            val timer by viewModel.timerText.collectAsState()

            LaunchedEffect(Unit) {
                if (progress == 0) viewModel.startCollection()
            }

            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                CalibrationHeader(status = status, overallProgress = progress)
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                    Card(modifier = Modifier.size(140.dp), backgroundColor = Color(0xFF202734)) {
                        Box(contentAlignment = Alignment.Center) { Text("🪐", color = Color.White) }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Follow the orientation prompts", color = Color.White)
                Spacer(modifier = Modifier.weight(1f))
                CalibrationBottomControls(
                    timerText = timer,
                    onBack = {},
                    onNext = { viewModel.stopCollection() },
                    onStop = { viewModel.stopCollection() },
                    backEnabled = false,
                    nextEnabled = progress >= 100
                )
            }
        }
        return composeView
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

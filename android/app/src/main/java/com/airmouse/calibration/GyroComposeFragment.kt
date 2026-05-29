package com.airmouse.calibration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.airmouse.ui.CalibrationHeader
import com.airmouse.ui.CalibrationBottomControls
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

class GyroComposeFragment : Fragment() {
    private lateinit var viewModel: GyroCalibrationViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory(requireActivity().application)).get(GyroCalibrationViewModel::class.java)
        val composeView = ComposeView(requireContext())
        composeView.setContent {
            val progress by viewModel.progress.collectAsState()
            val status by viewModel.status.collectAsState()
            val timer by viewModel.timerText.collectAsState()

            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                CalibrationHeader(status = status, overallProgress = progress)
                Spacer(modifier = Modifier.height(12.dp))
                // Phone animation placeholder
                Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                    val scale = if (progress in 1..99) 1.03f else 1f
                    Card(modifier = Modifier.size(140.dp), backgroundColor = Color(0xFF2B3341)) {
                        Box(contentAlignment = Alignment.Center) { Text("📱", color = Color.White, modifier = Modifier.padding(8.dp)) }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(progress = (progress / 100f).coerceIn(0f,1f), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
                Text(status, color = Color.White)
                Spacer(modifier = Modifier.weight(1f))
                CalibrationBottomControls(timerText = timer, onBack = {}, onNext = { viewModel.stopCollection() }, onStop = { viewModel.stopCollection() }, backEnabled = false, nextEnabled = progress>=100)
            }
        }
        return composeView
    }
}

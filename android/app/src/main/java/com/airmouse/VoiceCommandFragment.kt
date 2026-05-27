package com.airmouse.ui.voice

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.airmouse.R
import com.airmouse.network.DataSender
import com.airmouse.utils.PreferencesManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar

class VoiceCommandFragment : Fragment() {

    private lateinit var listenButton: MaterialButton
    private lateinit var resultText: TextView
    private lateinit var preferences: PreferencesManager
    private var dataSender: DataSender? = null

    private val voiceLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val command = matches[0].lowercase().trim()
                handleCommand(command)
                resultText.text = getString(R.string.command_recognized, command)
            } else {
                resultText.text = getString(R.string.voice_no_match)
            }
        } else {
            resultText.text = getString(R.string.voice_failed)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_voice_commands, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferences = PreferencesManager(requireContext())
        listenButton = view.findViewById(R.id.voice_listen_btn)
        resultText = view.findViewById(R.id.voice_result_text)

        listenButton.setOnClickListener { startVoiceRecognition() }
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.voice_prompt))
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        voiceLauncher.launch(intent)
    }

    private fun handleCommand(command: String) {
        when {
            command.contains("click") && !command.contains("double") && !command.contains("right") -> {
                sendAction("click")
                showFeedback(R.string.action_click)
            }
            command.contains("double click") -> {
                sendAction("double_click")
                showFeedback(R.string.action_double_click)
            }
            command.contains("right click") -> {
                sendAction("right_click")
                showFeedback(R.string.action_right_click)
            }
            command.contains("scroll up") -> {
                sendAction("scroll_up")
                showFeedback(R.string.action_scroll_up)
            }
            command.contains("scroll down") -> {
                sendAction("scroll_down")
                showFeedback(R.string.action_scroll_down)
            }
            command.contains("calibrate") -> {
                // Navigate to HomeFragment and trigger calibration
                findNavController().navigate(R.id.action_voice_to_home)
                showFeedback(R.string.action_calibrate)
            }
            command.contains("start") -> {
                sendAction("start")
                showFeedback(R.string.action_start)
            }
            command.contains("stop") -> {
                sendAction("stop")
                showFeedback(R.string.action_stop)
            }
            else -> resultText.text = getString(R.string.unknown_command, command)
        }
    }

    private fun sendAction(action: String) {
        // Access the global DataSender from MainActivity or via singleton
        // For simplicity, we'll use a static reference (you can improve with DI)
        val sender = DataSender.getInstance()
        when (action) {
            "click" -> sender?.sendClick()
            "double_click" -> sender?.sendDoubleClick()
            "right_click" -> sender?.sendRightClick()
            "scroll_up" -> sender?.sendScroll(-1)
            "scroll_down" -> sender?.sendScroll(1)
        }
    }

    private fun showFeedback(messageRes: Int) {
        Snackbar.make(requireView(), messageRes, Snackbar.LENGTH_SHORT).show()
        if (preferences.isHapticEnabled()) {
            vibrate(50)
        }
    }

    private fun vibrate(durationMs: Long) {
        val vibrator = requireContext().getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(durationMs, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }

    private fun findNavController() = androidx.navigation.findNavController(requireView())
}
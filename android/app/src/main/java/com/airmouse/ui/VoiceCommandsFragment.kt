// VoiceCommandsFragment.kt
package com.airmouse.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.airmouse.R
import com.airmouse.voice.VoiceCommandService
import com.google.android.material.button.MaterialButton

class VoiceCommandsFragment : Fragment() {
    private lateinit var startBtn: MaterialButton
    private lateinit var stopBtn: MaterialButton
    private lateinit var statusText: TextView
    private var voiceService: VoiceCommandService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            voiceService = (service as? VoiceCommandService.LocalBinder)?.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            voiceService = null
            isBound = false
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_voice_commands, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startBtn = view.findViewById(R.id.voice_listen_btn)
        stopBtn = view.findViewById(R.id.voice_stop_btn)
        statusText = view.findViewById(R.id.voice_result_text)

        startBtn.setOnClickListener {
            val intent = Intent(requireContext(), VoiceCommandService::class.java).apply {
                action = "START_LISTENING"
            }
            requireContext().startService(intent)
            statusText.text = "Listening... Speak a command"
        }

        stopBtn.setOnClickListener {
            val intent = Intent(requireContext(), VoiceCommandService::class.java).apply {
                action = "STOP_LISTENING"
            }
            requireContext().startService(intent)
            statusText.text = "Stopped listening"
        }

        // Bind to service to potentially get recognition events (optional)
        val serviceIntent = Intent(requireContext(), VoiceCommandService::class.java)
        requireContext().bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroyView() {
        if (isBound) {
            requireContext().unbindService(connection)
            isBound = false
        }
        super.onDestroyView()
    }
}
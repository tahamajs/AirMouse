// app/src/main/java/com/airmouse/ui/GestureStudioActivity.kt
package com.airmouse.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.airmouse.R
import com.airmouse.gesture.GestureRecorderService
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GestureStudioActivity : AppCompatActivity() {

    private lateinit var gestureNameInput: EditText
    private lateinit var startRecordBtn: MaterialButton
    private lateinit var stopRecordBtn: MaterialButton
    private lateinit var exportBtn: MaterialButton
    private lateinit var statusText: TextView
    private lateinit var datasetInfoText: TextView

    private var recorderService: GestureRecorderService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            recorderService = (service as GestureRecorderService.LocalBinder).getService()
            isBound = true
            statusText.text = "Service connected. Ready to record."
            updateDatasetInfo()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            recorderService = null
            isBound = false
            statusText.text = "Service disconnected."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gesture_studio)

        gestureNameInput = findViewById(R.id.gesture_name_input)
        startRecordBtn = findViewById(R.id.start_record_btn)
        stopRecordBtn = findViewById(R.id.stop_record_btn)
        exportBtn = findViewById(R.id.export_dataset_btn)
        statusText = findViewById(R.id.status_text)
        datasetInfoText = findViewById(R.id.dataset_info_text)

        startRecordBtn.setOnClickListener {
            val name = gestureNameInput.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Enter gesture name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (isBound && recorderService != null) {
                val intent = Intent(this, GestureRecorderService::class.java).apply {
                    action = GestureRecorderService.ACTION_START_RECORDING
                    putExtra(GestureRecorderService.EXTRA_GESTURE_NAME, name)
                }
                startService(intent)
                statusText.text = "Recording '$name'... Perform gesture 5-10 times"
                Toast.makeText(this, "Recording started", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Service not ready", Toast.LENGTH_SHORT).show()
            }
        }

        stopRecordBtn.setOnClickListener {
            if (isBound) {
                val intent = Intent(this, GestureRecorderService::class.java).apply {
                    action = GestureRecorderService.ACTION_STOP_RECORDING
                }
                startService(intent)
                statusText.text = "Recording stopped."
                updateDatasetInfo()
                Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show()
            }
        }

        exportBtn.setOnClickListener {
            if (isBound && recorderService != null) {
                recorderService?.shareDataset()
            } else {
                Toast.makeText(this, "No dataset yet", Toast.LENGTH_SHORT).show()
            }
        }

        // Start and bind service
        val intent = Intent(this, GestureRecorderService::class.java)
        startService(intent)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun updateDatasetInfo() {
        lifecycleScope.launch {
            delay(500)
            if (isBound && recorderService != null) {
                val file = recorderService?.exportDataset()
                if (file != null && file.exists()) {
                    datasetInfoText.text = "Dataset: ${file.name}\nSize: ${file.length() / 1024} KB"
                } else {
                    datasetInfoText.text = "No dataset yet. Record gestures first."
                }
            }
        }
    }

    override fun onDestroy() {
        if (isBound) unbindService(connection)
        super.onDestroy()
    }
}
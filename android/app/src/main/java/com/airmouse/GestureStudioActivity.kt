// GestureStudioActivity.kt
package com.airmouse.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.airmouse.R
import com.airmouse.gesture.GestureRecorderService
import kotlinx.coroutines.*

class GestureStudioActivity : AppCompatActivity() {

    private lateinit var gestureNameInput: EditText
    private lateinit var startRecordBtn: Button
    private lateinit var stopRecordBtn: Button
    private lateinit var exportBtn: Button
    private lateinit var statusText: TextView

    private var recorderService: GestureRecorderService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            recorderService = (service as GestureRecorderService.LocalBinder).getService()
            isBound = true
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            recorderService = null
            isBound = false
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

        startRecordBtn.setOnClickListener {
            val name = gestureNameInput.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Enter gesture name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, GestureRecorderService::class.java).apply {
                action = GestureRecorderService.ACTION_START_RECORDING
                putExtra(GestureRecorderService.EXTRA_GESTURE_NAME, name)
            }
            startService(intent)
            statusText.text = "Recording '$name'..."
            Toast.makeText(this, "Recording started. Perform gesture 5-10 times.", Toast.LENGTH_LONG).show()
        }

        stopRecordBtn.setOnClickListener {
            val intent = Intent(this, GestureRecorderService::class.java).apply {
                action = GestureRecorderService.ACTION_STOP_RECORDING
            }
            startService(intent)
            statusText.text = "Recording stopped."
        }

        exportBtn.setOnClickListener {
            if (isBound && recorderService != null) {
                val file = recorderService!!.exportDataset()
                if (file != null && file.exists()) {
                    Toast.makeText(this, "Dataset saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                    // Share intent
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, androidx.core.content.FileProvider.getUriForFile(
                            this@GestureStudioActivity,
                            "${packageName}.fileprovider",
                            file
                        ))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(shareIntent, "Export Dataset"))
                } else {
                    Toast.makeText(this, "No dataset yet", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Service not bound", Toast.LENGTH_SHORT).show()
            }
        }

        bindService(Intent(this, GestureRecorderService::class.java), connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        if (isBound) unbindService(connection)
        super.onDestroy()
    }
}
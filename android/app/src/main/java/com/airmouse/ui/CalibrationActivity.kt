package com.airmouse.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.airmouse.R
import com.airmouse.calibration.CalibrationPagerAdapter
import java.util.Locale

class CalibrationActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var timerText: TextView
    private lateinit var backBtn: Button
    private lateinit var nextBtn: Button
    private lateinit var stopBtn: Button

    private var currentStep = 0
    private val totalSteps = CalibrationPagerAdapter.STEP_COUNT
    private var secondsElapsed = 0
    private val handler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            secondsElapsed++
            val minutes = secondsElapsed / 60
            val secs = secondsElapsed % 60
            timerText.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calibration)

        viewPager = findViewById(R.id.viewPager)
        timerText = findViewById(R.id.timerText)
        backBtn = findViewById(R.id.backBtn)
        nextBtn = findViewById(R.id.nextBtn)
        stopBtn = findViewById(R.id.stopBtn)

        viewPager.adapter = CalibrationPagerAdapter(this)
        viewPager.isUserInputEnabled = false

        // Start timer
        handler.postDelayed(timerRunnable, 1000)

        backBtn.setOnClickListener {
            if (currentStep > 0) {
                currentStep--
                viewPager.currentItem = currentStep
                resetStepState()
            }
        }

        nextBtn.setOnClickListener {
            if (currentStep < totalSteps - 1) {
                currentStep++
                viewPager.currentItem = currentStep
                resetStepState()
            } else {
                // Finish – save final data if needed
                showSuccessDialog()
            }
        }

        stopBtn.setOnClickListener {
            showAbortDialog()
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentStep = position
                updateButtons()
                resetStepState()
            }
        })

        updateButtons()
    }

    fun onStepComplete() {
        nextBtn.isEnabled = true
    }

    fun resetStepState() {
        val fragment = supportFragmentManager.findFragmentByTag("f${currentStep}") as? com.airmouse.calibration.CalibrationStepFragment
        fragment?.resetUI()
        nextBtn.isEnabled = fragment?.isStepComplete() ?: false
        updateButtons()
    }

    private fun updateButtons() {
        backBtn.isEnabled = currentStep > 0
        // Next button enabled only if step is complete (set by fragment callback)
    }

    private fun showSuccessDialog() {
        handler.removeCallbacks(timerRunnable)
        AlertDialog.Builder(this)
            .setTitle("Calibration Complete")
            .setMessage("All sensors calibrated!")
            .setPositiveButton("OK") { _, _ -> finish() }
            .show()
    }

    private fun showAbortDialog() {
        AlertDialog.Builder(this)
            .setTitle("Abort Calibration?")
            .setMessage("Progress will be lost.")
            .setPositiveButton("Yes") { _, _ -> finish() }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onDestroy() {
        handler.removeCallbacks(timerRunnable)
        super.onDestroy()
    }
}
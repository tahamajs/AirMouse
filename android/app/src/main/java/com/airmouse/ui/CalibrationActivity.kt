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
    private var timerRunning = false
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
        viewPager.isUserInputEnabled = false   // swipe only via buttons

        // Start timer automatically
        startTimer()

        backBtn.setOnClickListener {
            if (currentStep > 0) {
                currentStep--
                viewPager.currentItem = currentStep
                resetStepState()
            }
        }

        nextBtn.setOnClickListener {
            val fragment = supportFragmentManager.findFragmentByTag("f${currentStep}") as? CalibrationStepFragment
            if (fragment?.isStepComplete() == true) {
                if (currentStep < totalSteps - 1) {
                    currentStep++
                    viewPager.currentItem = currentStep
                    resetStepState()
                } else {
                    // All steps done – save and finish
                    fragment.saveCalibrationData()
                    stopTimer()
                    showSuccessDialog()
                }
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

        // Initially show first step
        updateButtons()
    }

    private fun resetStepState() {
        // Reset fragment's UI if needed (each fragment handles its own start)
        // But we can communicate with fragment to reset its status
        val fragment = supportFragmentManager.findFragmentByTag("f${currentStep}") as? CalibrationStepFragment
        fragment?.resetUI()
        nextBtn.isEnabled = fragment?.isStepComplete() ?: false
        updateButtons()
    }

    private fun updateButtons() {
        backBtn.isEnabled = currentStep > 0
        // Next enabled only if current step complete (checked in fragment)
        // We'll rely on fragment callback, but for now enable/disable via fragment method
        val fragment = supportFragmentManager.findFragmentByTag("f${currentStep}") as? CalibrationStepFragment
        nextBtn.isEnabled = fragment?.isStepComplete() ?: false
    }

    fun onStepComplete() {
        // Called from fragment when its calibration finishes successfully
        nextBtn.isEnabled = true
    }

    private fun startTimer() {
        timerRunning = true
        handler.postDelayed(timerRunnable, 1000)
    }

    private fun stopTimer() {
        timerRunning = false
        handler.removeCallbacks(timerRunnable)
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("Calibration Complete")
            .setMessage("All sensors calibrated successfully!")
            .setPositiveButton("OK") { _, _ -> finish() }
            .show()
    }

    private fun showAbortDialog() {
        AlertDialog.Builder(this)
            .setTitle("Abort Calibration?")
            .setMessage("Are you sure you want to stop? Progress will be lost.")
            .setPositiveButton("Yes") { _, _ -> finish() }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onDestroy() {
        stopTimer()
        super.onDestroy()
    }
}
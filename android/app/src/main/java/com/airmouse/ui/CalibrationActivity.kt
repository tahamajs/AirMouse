package com.airmouse.ui

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.airmouse.R
import com.airmouse.calibration.CalibrationPagerAdapter
import com.airmouse.calibration.CalibrationStepFragment
import com.airmouse.utils.PreferencesManager

class CalibrationActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var timerText: TextView
    private lateinit var backBtn: Button
    private lateinit var nextBtn: Button
    private lateinit var stopBtn: Button
    private lateinit var overallProgress: ProgressBar
    private lateinit var statusHeader: TextView

    private var currentStep = 0
    private val totalSteps = CalibrationPagerAdapter.STEP_COUNT
    private lateinit var preferences: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calibration)

        viewPager = findViewById(R.id.viewPager)
        timerText = findViewById(R.id.timerText)
        backBtn = findViewById(R.id.backBtn)
        nextBtn = findViewById(R.id.nextBtn)
        stopBtn = findViewById(R.id.stopBtn)
        overallProgress = findViewById(R.id.overallProgress)
        statusHeader = findViewById(R.id.statusHeader)

        preferences = PreferencesManager(this)

        viewPager.adapter = CalibrationPagerAdapter(this)
        viewPager.isUserInputEnabled = false

        backBtn.setOnClickListener {
            if (currentStep > 0) {
                currentStep--
                viewPager.currentItem = currentStep
                resetCurrentStepState()
                updateButtons()
            }
        }

        nextBtn.setOnClickListener {
            val fragment = getCurrentFragment()
            if (fragment?.isDataValid() != true) {
                Toast.makeText(this, "Please complete this step successfully before proceeding.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (currentStep < totalSteps - 1) {
                currentStep++
                viewPager.currentItem = currentStep
                resetCurrentStepState()
                updateButtons()
            } else {
                // All steps completed
                preferences.setCalibrated(true)
                showSuccessDialog()
            }
        }

        stopBtn.setOnClickListener {
            showAbortDialog()
        }

        // Initially, next button disabled, back disabled if step 0
        updateButtons()
    }

    private fun getCurrentFragment(): CalibrationStepFragment? {
        return supportFragmentManager.findFragmentByTag("f$currentStep") as? CalibrationStepFragment
    }

    private fun resetCurrentStepState() {
        getCurrentFragment()?.resetUI()
        // Also reset timer display? The fragment will reset its own timer when start is pressed.
        // But we need to clear any ongoing sensor registration.
    }

    private fun updateButtons() {
        backBtn.isEnabled = currentStep > 0
        nextBtn.isEnabled = false // initially disabled; fragments will enable it when step is complete
        if (currentStep == totalSteps - 1) {
            nextBtn.text = "Finish"
        } else {
            nextBtn.text = "Next"
        }
    }

    fun setTimerText(text: String) {
        runOnUiThread { timerText.text = text }
    }

    fun setStatusHeader(text: String) {
        runOnUiThread { statusHeader.text = text }
    }

    fun onStepComplete() {
        // Called by a fragment when its calibration step is successfully completed and validated
        runOnUiThread {
            nextBtn.isEnabled = true
            updateOverallProgress()
            vibrate(50)
        }
    }

    private fun updateOverallProgress() {
        // Simple progress: steps completed / total steps
        val progress = (currentStep + 1) * 100 / totalSteps
        overallProgress.progress = progress
    }

    private fun vibrate(durationMs: Long) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("Calibration Complete")
            .setMessage("All sensors calibrated successfully! You can now use the Air Mouse.")
            .setPositiveButton("OK") { _, _ -> finish() }
            .show()
    }

    private fun showAbortDialog() {
        AlertDialog.Builder(this)
            .setTitle("Abort Calibration?")
            .setMessage("Progress will be lost. Are you sure?")
            .setPositiveButton("Yes") { _, _ -> finish() }
            .setNegativeButton("No", null)
            .show()
    }
}
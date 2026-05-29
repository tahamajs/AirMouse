package com.airmouse.ui

import android.content.Context
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

        viewPager.adapter = CalibrationPagerAdapter(this)
        viewPager.isUserInputEnabled = false

        timerText.text = "00:00"
        statusHeader.text = getString(R.string.calibration_welcome)

        backBtn.setOnClickListener {
            if (currentStep > 0) {
                currentStep--
                viewPager.currentItem = currentStep
                resetStepState()
            }
        }

        nextBtn.setOnClickListener {
            val fragment = supportFragmentManager.findFragmentByTag("f${currentStep}") as? CalibrationStepFragment
            if (fragment?.isDataValid() != true) {
                Toast.makeText(this, "Please complete this step successfully before proceeding.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (currentStep < totalSteps - 1) {
                currentStep++
                viewPager.currentItem = currentStep
                resetStepState()
            } else {
                // All steps completed – mark calibration as done and finish
                val prefs = com.airmouse.utils.PreferencesManager(this)
                prefs.setCalibrated(true)
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

    /** Called by fragments to update the timer display. */
    fun setTimerText(text: String) {
        runOnUiThread { timerText.text = text }
    }

    /** Called by fragments to update the header. */
    fun setStatusHeader(text: String) {
        runOnUiThread { statusHeader.text = text }
    }

    /** Called by fragments when they complete their step successfully. */
    fun onStepComplete() {
        nextBtn.isEnabled = true
        updateOverallProgress()
        // Short vibration feedback
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    fun resetStepState() {
        val fragment = supportFragmentManager.findFragmentByTag("f${currentStep}") as? CalibrationStepFragment
        fragment?.resetUI()
        nextBtn.isEnabled = fragment?.isStepComplete() ?: false
        updateButtons()
    }

    private fun updateButtons() {
        backBtn.isEnabled = currentStep > 0
        // Change next button text on last step
        if (currentStep == totalSteps - 1) {
            nextBtn.text = "Finish"
        } else {
            nextBtn.text = "Next"
        }
    }

    private fun updateOverallProgress() {
        val progress = (currentStep + 1) * 100 / totalSteps
        overallProgress.progress = progress
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("Calibration Complete")
            .setMessage("All sensors calibrated successfully! You can now start the Air Mouse.")
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
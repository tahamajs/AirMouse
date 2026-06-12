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
import androidx.activity.OnBackPressedCallback
import androidx.viewpager2.widget.ViewPager2
import com.airmouse.R
import com.airmouse.calibration.CalibrationPagerAdapter
import com.airmouse.calibration.CalibrationStepFragment
import com.airmouse.utils.PreferencesManager
import com.google.android.material.tabs.TabLayoutMediator
import kotlin.math.max

/**
 * Host activity for the guided calibration wizard.
 *
 * The calibration flow is intentionally kept inside a dedicated screen so the user always
 * knows what to do next and the app can keep the phone/PC sync deterministic.
 */
class CalibrationActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var timerText: TextView
    private lateinit var backBtn: Button
    private lateinit var nextBtn: Button
    private lateinit var stopBtn: Button
    private lateinit var overallProgress: ProgressBar
    private lateinit var statusHeader: TextView
    private lateinit var taskProgressText: TextView
    private lateinit var gyroCheck: TextView
    private lateinit var accelCheck: TextView
    private lateinit var magCheck: TextView

    private val stepTitles = listOf(
        "Gyroscope calibration",
        "Accelerometer calibration",
        "Magnetometer calibration"
    )

    private var currentStep = 0
    private var highestStepCompleted = -1
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
        taskProgressText = findViewById(R.id.taskProgressText)
        gyroCheck = findViewById(R.id.gyroCheck)
        accelCheck = findViewById(R.id.accelCheck)
        magCheck = findViewById(R.id.magCheck)

        preferences = PreferencesManager(this)

        viewPager.adapter = CalibrationPagerAdapter(this)
        viewPager.isUserInputEnabled = false
        TabLayoutMediator(findViewById(R.id.tabLayout), viewPager) { tab, position ->
            tab.text = stepTitles.getOrElse(position) { "Step ${position + 1}" }
        }.attach()
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentStep = position
                updateHeaderForCurrentStep()
                updateButtons()
            }
        })

        backBtn.setOnClickListener { goToPreviousStepFromUi() }
        nextBtn.setOnClickListener { handleNextStep() }
        stopBtn.setOnClickListener { abortFromUi() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentStep > 0) {
                    goToPreviousStepFromUi()
                } else {
                    abortFromUi()
                }
            }
        })

        updateHeaderForCurrentStep()
        updateButtons()
    }

    private fun updateHeaderForCurrentStep() {
        statusHeader.text = stepTitles.getOrElse(currentStep) { "Calibration" }
        taskProgressText.text = "Step ${currentStep + 1} / $totalSteps"
        overallProgress.progress = ((currentStep + 1) * 100 / totalSteps).coerceIn(0, 100)
        updateChecklist()
    }

    private fun updateButtons() {
        backBtn.isEnabled = currentStep > 0
        nextBtn.isEnabled = false
        nextBtn.text = if (currentStep == totalSteps - 1) "Finish" else "Next"
    }

    private fun updateChecklist() {
        gyroCheck.text = if (highestStepCompleted >= 0) "✓ Gyroscope" else "⬜ Gyroscope"
        accelCheck.text = if (highestStepCompleted >= 1) "✓ Accelerometer" else "⬜ Accelerometer"
        magCheck.text = if (highestStepCompleted >= 2) "✓ Magnetometer" else "⬜ Magnetometer"
    }

    private fun handleBackStep() {
        if (currentStep <= 0) return
        currentStep -= 1
        viewPager.currentItem = currentStep
        resetCurrentStepState()
        updateHeaderForCurrentStep()
        updateButtons()
    }

    private fun handleNextStep() {
        val fragment = getCurrentFragment()
        if (fragment?.isDataValid() != true) {
            Toast.makeText(
                this,
                "Please complete this step successfully before proceeding.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (currentStep < totalSteps - 1) {
            highestStepCompleted = max(highestStepCompleted, currentStep)
            currentStep += 1
            viewPager.currentItem = currentStep
            resetCurrentStepState()
            updateHeaderForCurrentStep()
            updateButtons()
        } else {
            highestStepCompleted = max(highestStepCompleted, currentStep)
            updateChecklist()
            preferences.setCalibrated(true)
            preferences.resetCalibrationAttempts()
            showSuccessDialog()
        }
    }

    private fun getCurrentFragment(): CalibrationStepFragment? {
        return supportFragmentManager.findFragmentByTag("f$currentStep") as? CalibrationStepFragment
    }

    private fun resetCurrentStepState() {
        getCurrentFragment()?.resetUI()
    }

    fun setTimerText(text: String) {
        runOnUiThread { timerText.text = text }
    }

    fun setStatusHeader(text: String) {
        runOnUiThread { statusHeader.text = text }
    }

    fun onStepComplete() {
        runOnUiThread {
            highestStepCompleted = max(highestStepCompleted, currentStep)
            nextBtn.isEnabled = true
            updateHeaderForCurrentStep()
            vibrate(50)
        }
    }

    fun goToPreviousStepFromUi() {
        handleBackStep()
    }

    fun goToNextStepFromUi() {
        handleNextStep()
    }

    fun abortFromUi() {
        showAbortDialog()
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
            .setMessage("All sensors calibrated successfully. You can now use the Air Mouse.")
            .setPositiveButton("OK") { _, _ -> finish() }
            .setCancelable(false)
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

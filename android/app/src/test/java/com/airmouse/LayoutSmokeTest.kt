package com.airmouse

import android.view.LayoutInflater
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LayoutSmokeTest {

    @Test
    fun `home layout exposes the core controls`() {
        val view = LayoutInflater.from(RuntimeEnvironment.getApplication())
            .inflate(R.layout.fragment_home, null, false)

        assertNotNull(view.findViewById(R.id.ip_edit_text))
        assertNotNull(view.findViewById(R.id.port_edit_text))
        assertNotNull(view.findViewById(R.id.connect_btn))
        assertNotNull(view.findViewById(R.id.modeToggle))
        assertNotNull(view.findViewById(R.id.modeToggleTouchpad))
        assertNotNull(view.findViewById(R.id.calibrate_btn))
        assertNotNull(view.findViewById(R.id.calibrate_fab))
    }

    @Test
    fun `gesture training layout exposes recording controls`() {
        val view = LayoutInflater.from(RuntimeEnvironment.getApplication())
            .inflate(R.layout.fragment_gesture_training, null, false)

        assertNotNull(view.findViewById(R.id.training_status))
        assertNotNull(view.findViewById(R.id.train_click_btn))
        assertNotNull(view.findViewById(R.id.train_scroll_btn))
    }

    @Test
    fun `calibration layouts expose progress and status fields`() {
        val accel = LayoutInflater.from(RuntimeEnvironment.getApplication())
            .inflate(R.layout.fragment_accel_step, null, false)
        val mag = LayoutInflater.from(RuntimeEnvironment.getApplication())
            .inflate(R.layout.fragment_mag_step, null, false)

        assertNotNull(accel.findViewById(R.id.positionBadge))
        assertNotNull(accel.findViewById(R.id.progressBar))
        assertNotNull(accel.findViewById(R.id.coachMessage))

        assertNotNull(mag.findViewById(R.id.movementQualityText))
        assertNotNull(mag.findViewById(R.id.progressBar))
        assertNotNull(mag.findViewById(R.id.coachMessage))
    }
}

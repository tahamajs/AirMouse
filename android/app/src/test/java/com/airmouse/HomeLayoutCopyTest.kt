package com.airmouse

import android.view.LayoutInflater
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeLayoutCopyTest {

    @Test
    fun homeLayoutUsesClearerUserFacingCopy() {
        val context = RuntimeEnvironment.getApplication()
        val view = LayoutInflater.from(context).inflate(R.layout.fragment_home, null, false)

        assertNotNull(view.findViewById<android.widget.TextView>(R.id.status_text))
        assertNotNull(view.findViewById<android.widget.TextView>(R.id.calibration_progress_text))
        assertNotNull(view.findViewById<android.widget.TextView>(R.id.live_log_text))
        assertEquals(context.getString(R.string.live_log_empty), view.findViewById<android.widget.TextView>(R.id.live_log_text).text)
    }
}

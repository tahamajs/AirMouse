package com.airmouse

import android.view.LayoutInflater
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StatisticsLayoutTest {

    @Test
    fun `statistics layout exposes chart and summary text`() {
        val view = LayoutInflater.from(RuntimeEnvironment.getApplication())
            .inflate(R.layout.fragment_statistics, null, false)

        assertNotNull(view.findViewById(R.id.gesture_chart))
        assertNotNull(view.findViewById(R.id.gesture_count_text))
        assertNotNull(view.findViewById(R.id.session_time_text))
    }
}

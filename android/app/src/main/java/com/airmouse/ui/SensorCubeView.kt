package com.airmouse.ui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AbstractComposeView
import com.airmouse.presentation.ui.sensor.SensorCubeView

class SensorCubeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    @Composable
    override fun Content() {
        SensorCubeView(roll = 0f, pitch = 0f, yaw = 0f)
    }
}

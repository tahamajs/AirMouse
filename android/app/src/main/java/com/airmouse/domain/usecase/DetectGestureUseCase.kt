// app/src/main/java/com/airmouse/domain/usecase/DetectGestureUseCase.kt
package com.airmouse.domain.usecase

import com.airmouse.domain.model.Gesture
import com.airmouse.domain.model.GestureType
import com.airmouse.domain.repository.IGestureRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.math.abs

class DetectGestureUseCase @Inject constructor(
    private val gestureRepo: IGestureRepository,
    private val settingsRepo: ISettingsRepository
) {

    suspend fun detectClick(gyroY: Float, gyroDelta: Float): Gesture? {
        val threshold = settingsRepo.getPreferences().first().clickThreshold
        if (abs(gyroDelta) > threshold) {
            return Gesture(GestureType.CLICK)
        }
        return null
    }

    suspend fun detectScroll(accelY: Float, accelDelta: Float): Gesture? {
        val threshold = settingsRepo.getPreferences().first().scrollThreshold
        if (abs(accelDelta) > threshold) {
            return if (accelDelta > 0) Gesture(GestureType.SCROLL_UP) else Gesture(GestureType.SCROLL_DOWN)
        }
        return null
    }

    suspend fun detectDoubleClick(lastClickTime: Long, currentTime: Long): Gesture? {
        val interval = settingsRepo.getPreferences().first().doubleClickInterval
        if (currentTime - lastClickTime < interval) {
            return Gesture(GestureType.DOUBLE_CLICK)
        }
        return null
    }

    suspend fun detectRightClick(tiltAngle: Float): Gesture? {
        val threshold = settingsRepo.getPreferences().first().rightClickTilt
        if (abs(tiltAngle) > threshold) {
            return Gesture(GestureType.RIGHT_CLICK)
        }
        return null
    }
}
// app/src/main/java/com/airmouse/domain/usecase/SendMovementUseCase.kt
package com.airmouse.domain.usecase

import com.airmouse.domain.model.MouseEvent
import com.airmouse.domain.repository.IConnectionRepository
import com.airmouse.domain.repository.ISettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SendMovementUseCase @Inject constructor(
    private val connectionRepo: IConnectionRepository,
    private val settingsRepo: ISettingsRepository
) {

    suspend fun sendMove(dx: Float, dy: Float) {
        val sensitivity = settingsRepo.getPreferences().first().cursorSensitivity
        val scaledDx = dx * sensitivity
        val scaledDy = dy * sensitivity
        connectionRepo.sendEvent(MouseEvent.Move(scaledDx, scaledDy))
    }

    suspend fun sendClick() {
        connectionRepo.sendEvent(MouseEvent.Click)
    }

    suspend fun sendDoubleClick() {
        connectionRepo.sendEvent(MouseEvent.DoubleClick)
    }

    suspend fun sendRightClick() {
        connectionRepo.sendEvent(MouseEvent.RightClick)
    }

    suspend fun sendScroll(delta: Int) {
        connectionRepo.sendEvent(MouseEvent.Scroll(delta))
    }
}

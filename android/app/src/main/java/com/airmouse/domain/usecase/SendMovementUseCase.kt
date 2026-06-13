// app/src/main/java/com/airmouse/domain/usecase/SendMovementUseCase.kt
package com.airmouse.domain.usecase

import com.airmouse.domain.model.MouseEvent
import com.airmouse.domain.model.MouseButton
import com.airmouse.domain.repository.IConnectionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SendMovementUseCase @Inject constructor(
    private val connectionRepo: IConnectionRepository,
    private val settingsRepo: ISettingsRepository
) {

    private var lastMoveTime = 0L
    private val moveThrottleMs = 10L // Throttle to ~100Hz

    /**
     * Send cursor movement with sensitivity and acceleration applied
     */
    suspend fun sendMove(dx: Float, dy: Float) {
        val now = System.currentTimeMillis()
        if (now - lastMoveTime < moveThrottleMs) return
        lastMoveTime = now

        val prefs = settingsRepo.getPreferences().first()

        // Apply sensitivity
        var finalDx = dx * prefs.cursorSensitivity
        var finalDy = dy * prefs.cursorSensitivity

        // Apply inversion
        if (prefs.invertX) finalDx = -finalDx
        if (prefs.invertY) finalDy = -finalDy

        // Apply acceleration
        if (prefs.accelerationEnabled) {
            val speed = kotlin.math.sqrt(finalDx * finalDx + finalDy * finalDy)
            val acceleration = 1 + (speed / 100f)
            finalDx *= acceleration
            finalDy *= acceleration
        }

        // Apply smoothing
        if (prefs.smoothingEnabled) {
            finalDx = finalDx * prefs.smoothingFactor + (1 - prefs.smoothingFactor) * lastDx
            finalDy = finalDy * prefs.smoothingFactor + (1 - prefs.smoothingFactor) * lastDy
            lastDx = finalDx
            lastDy = finalDy
        }

        connectionRepo.sendEvent(MouseEvent.Move(finalDx, finalDy))
    }

    /**
     * Send left click
     */
    suspend fun sendClick() {
        connectionRepo.sendEvent(MouseEvent.Click(MouseButton.LEFT))
    }

    /**
     * Send double click
     */
    suspend fun sendDoubleClick() {
        connectionRepo.sendEvent(MouseEvent.DoubleClick)
    }

    /**
     * Send right click
     */
    suspend fun sendRightClick() {
        connectionRepo.sendEvent(MouseEvent.RightClick)
    }

    /**
     * Send scroll with configurable delta
     */
    suspend fun sendScroll(delta: Int) {
        val prefs = settingsRepo.getPreferences().first()
        val scaledDelta = (delta * prefs.cursorSpeed).toInt()
        connectionRepo.sendEvent(MouseEvent.Scroll(scaledDelta))
    }

    /**
     * Send gesture event
     */
    suspend fun sendGesture(name: String, confidence: Float) {
        connectionRepo.sendEvent(MouseEvent.Gesture(name, confidence))
    }

    /**
     * Send proximity event
     */
    suspend fun sendProximity(isNear: Boolean, distance: Float) {
        connectionRepo.sendEvent(MouseEvent.Proximity(isNear, distance))
    }

    /**
     * Send control command
     */
    suspend fun sendControl(command: String) {
        connectionRepo.sendEvent(MouseEvent.Control(command))
    }

    /**
     * Send key press
     */
    suspend fun sendKeyPress(keyCode: Int, keyChar: Char? = null) {
        connectionRepo.sendEvent(MouseEvent.KeyPress(keyCode, keyChar))
    }

    private var lastDx = 0f
    private var lastDy = 0f
}
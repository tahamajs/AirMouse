package com.airmouse.sensors

class GestureDetector(private var sensitivity: Float = 1.0f) {

    private var lastPitch = 0f
    private var lastRoll  = 0f
    private var clickCooldown = 0L
    private var lastClickTime = 0L

    /**
     * Returns [dx, dy] for mouse movement and a boolean for click/scroll.
     * Call this with Euler angles (pitch, roll, yaw) from SensorFusion.
     */
    fun process(pitch: Float, roll: Float, yaw: Float): MotionResult {
        // Dead zone
        val dx = if (abs(pitch - lastPitch) < 0.3f) 0f else (pitch - lastPitch) * sensitivity
        val dy = if (abs(roll - lastRoll) < 0.3f) 0f else (roll - lastRoll) * sensitivity

        lastPitch = pitch; lastRoll = roll

        // Click detection: quick yaw rotation to left (yaw < -20)
        val now = System.currentTimeMillis()
        var click = false
        if (yaw < -20f && now - lastClickTime > 300) {
            click = true
            lastClickTime = now
        }

        return MotionResult(dx, dy, click)
    }

    data class MotionResult(val dx: Float, val dy: Float, val click: Boolean)
    private fun abs(f: Float) = kotlin.math.abs(f)
}
package com.airmouse.domain.model

data class CustomControlMode(
    val id: String,
    val name: String,
    val moveSource: String,      // "gyro", "linear_accel", "touchpad"
    val scrollSource: String,    // "z_axis", "none"
    val clickSource: String,     // "roll_tilt", "none"
    val sensitivity: Float = 1.0f,
    val rollClickThreshold: Float = 45f,
    val zScrollThreshold: Float = 4.0f,
    val isCustom: Boolean = true
) {
    companion object {
        fun getPredefinedModes(): List<CustomControlMode> {
            return listOf(
                CustomControlMode(
                    id = "motion_gyro",
                    name = "Motion (Gyro Tilt)",
                    moveSource = "gyro",
                    scrollSource = "z_axis",
                    clickSource = "roll_tilt",
                    sensitivity = 1.0f,
                    rollClickThreshold = 45f,
                    zScrollThreshold = 4.0f,
                    isCustom = false
                ),
                CustomControlMode(
                    id = "motion_translation",
                    name = "Motion (Translation Move)",
                    moveSource = "linear_accel",
                    scrollSource = "z_axis",
                    clickSource = "roll_tilt",
                    sensitivity = 1.2f,
                    rollClickThreshold = 45f,
                    zScrollThreshold = 4.0f,
                    isCustom = false
                ),
                CustomControlMode(
                    id = "touchpad_mode",
                    name = "Touchpad Mode",
                    moveSource = "touchpad",
                    scrollSource = "none",
                    clickSource = "none",
                    sensitivity = 1.0f,
                    rollClickThreshold = 45f,
                    zScrollThreshold = 4.0f,
                    isCustom = false
                )
            )
        }
    }
}

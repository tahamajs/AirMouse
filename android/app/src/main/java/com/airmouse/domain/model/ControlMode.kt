package com.airmouse.domain.model

/**
 * Control mode for how the phone controls the cursor.
 */
enum class ControlMode(val displayName: String) {
    GYRO("Gyroscope (Tilt)"),
    ACCEL("Accelerometer (Move)"),
    HYBRID("Hybrid");

    /**
     * Get a detailed description of the control mode.
     */
    fun getDescription(): String {
        return when (this) {
            GYRO -> "Control cursor by tilting the phone"
            ACCEL -> "Control cursor by moving the phone in space"
            HYBRID -> "Combine tilt and movement for intuitive control"
        }
    }

    /**
     * Get the icon resource name for this mode.
     */
    fun getIconName(): String {
        return when (this) {
            GYRO -> "ic_gyro_mode"
            ACCEL -> "ic_accel_mode"
            HYBRID -> "ic_hybrid_mode"
        }
    }

    /**
     * Get the settings key for this mode.
     */
    fun getPreferenceKey(): String {
        return "control_mode_${name.lowercase()}"
    }

    companion object {
        /**
         * Get the default control mode.
         */
        fun default(): ControlMode {
            return GYRO
        }

        /**
         * Parse a control mode from a string.
         */
        fun fromString(value: String): ControlMode? {
            return try {
                valueOf(value.uppercase())
            } catch (_: Exception) {
                null
            }
        }

        /**
         * Get the next control mode in sequence.
         */
        fun next(current: ControlMode): ControlMode {
            return when (current) {
                GYRO -> ACCEL
                ACCEL -> HYBRID
                HYBRID -> GYRO
            }
        }

        /**
         * Get all control modes as a list.
         */
        fun allModes(): List<ControlMode> {
            return values().toList()
        }
    }
}
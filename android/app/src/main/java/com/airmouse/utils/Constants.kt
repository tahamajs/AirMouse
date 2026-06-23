
package com.airmouse.utils

object Constants {
    
    const val DEFAULT_PORT = 8080
    const val DEFAULT_WS_PORT = 8081
    const val DEFAULT_UDP_PORT = 8082
    const val CONNECTION_TIMEOUT_MS = 5000
    const val ACK_TIMEOUT_MS = 500L
    const val RECONNECT_DELAY_MS = 5000L
    const val MAX_RECONNECT_ATTEMPTS = 5
    const val KEEP_ALIVE_INTERVAL_MS = 30000L

    
    const val SENSOR_DELAY_US = 20000
    const val GYRO_SAMPLE_COUNT = 500
    const val ACCEL_SAMPLES_PER_POSITION = 100
    const val MAG_SAMPLE_DURATION_MS = 15000L

    
    const val DEFAULT_SENSITIVITY = 0.5f
    const val CLICK_SPEED_THRESHOLD = 8f
    const val SCROLL_SPEED_THRESHOLD = 6f
    const val SCROLL_DEBOUNCE = 2f
    const val DOUBLE_CLICK_INTERVAL_MS = 400L
    const val RIGHT_CLICK_TILT_DEG = 45f
    const val RIGHT_CLICK_DURATION_MS = 500L
    const val SWIPE_THRESHOLD = 15f
    const val GESTURE_COOLDOWN_MS = 500L

    
    const val ANIMATION_DURATION_MS = 300
    const val SNACKBAR_DURATION_MS = 3000
    const val DEBOUNCE_DELAY_MS = 500
    const val TYPING_DELAY_MS = 1000

    
    const val DATABASE_NAME = "airmouse_db"
    const val DATABASE_VERSION = 1

    
    const val MAX_LOG_ENTRIES = 500
    const val LOG_FILE_MAX_SIZE_BYTES = 10 * 1024 * 1024 

    
    const val MOVEMENT_THROTTLE_MS = 10L
    const val SENSOR_FUSION_RATE_HZ = 50
    const val MAX_MESSAGE_QUEUE_SIZE = 1000

    
    const val CALIBRATION_GYRO_SAMPLES = 500
    const val CALIBRATION_ACCEL_SAMPLES = 100
    const val CALIBRATION_MAG_DURATION_MS = 15000L

    
    const val ENABLE_AI_SMOOTHING = false
    const val ENABLE_PREDICTIVE_MOVEMENT = true
    const val ENABLE_BATTERY_SAVER = true
    const val ENABLE_EDGE_GESTURES = true
    const val ENABLE_VOICE_COMMANDS = true
}
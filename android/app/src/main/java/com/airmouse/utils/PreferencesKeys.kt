// app/src/main/java/com/airmouse/utils/PreferencesKeys.kt
package com.airmouse.utils

/**
 * Centralized preferences keys for all app settings.
 * Follows a consistent naming convention for maintainability.
 */
object PreferencesKeys {

    // ==========================================
    // CONNECTION KEYS
    // ==========================================

    const val KEY_LAST_IP = "last_ip"
    const val KEY_LAST_PORT = "last_port"
    const val KEY_CONNECTION_PROTOCOL = "connection_protocol"
    const val KEY_USE_SSL = "use_ssl"
    const val KEY_AUTH_TOKEN = "auth_token"
    const val KEY_AUTO_CONNECT = "auto_connect"
    const val KEY_AUTO_START_SERVER = "auto_start_server"
    const val KEY_RECONNECT_ATTEMPTS = "reconnect_attempts"
    const val KEY_CONNECTION_TIMEOUT = "connection_timeout"
    const val KEY_USE_WEBSOCKET = "use_websocket"
    const val KEY_USE_UDP_DISCOVERY = "use_udp_discovery"
    const val KEY_SERVER_MAC = "server_mac"
    const val KEY_LAST_PROTOCOL = "last_protocol"
    const val KEY_CONNECTION_STATUS = "connection_status"

    // ==========================================
    // CALIBRATION KEYS - GYROSCOPE
    // ==========================================

    const val KEY_GYRO_BIAS_X = "gyro_bias_x"
    const val KEY_GYRO_BIAS_Y = "gyro_bias_y"
    const val KEY_GYRO_BIAS_Z = "gyro_bias_z"
    const val KEY_GYRO_VARIANCE_X = "gyro_variance_x"
    const val KEY_GYRO_VARIANCE_Y = "gyro_variance_y"
    const val KEY_GYRO_VARIANCE_Z = "gyro_variance_z"
    const val KEY_GYRO_SAMPLES = "gyro_samples"
    const val KEY_GYRO_CALIBRATED = "gyro_calibrated"
    const val KEY_GYRO_OFFSET_X = "gyro_offset_x"
    const val KEY_GYRO_OFFSET_Y = "gyro_offset_y"
    const val KEY_GYRO_OFFSET_Z = "gyro_offset_z"

    // ==========================================
    // CALIBRATION KEYS - ACCELEROMETER
    // ==========================================

    const val KEY_ACCEL_OFFSET_X = "accel_offset_x"
    const val KEY_ACCEL_OFFSET_Y = "accel_offset_y"
    const val KEY_ACCEL_OFFSET_Z = "accel_offset_z"
    const val KEY_ACCEL_SCALE_X = "accel_scale_x"
    const val KEY_ACCEL_SCALE_Y = "accel_scale_y"
    const val KEY_ACCEL_SCALE_Z = "accel_scale_z"
    const val KEY_ACCEL_CALIBRATED = "accel_calibrated"
    const val KEY_ACCEL_POSITIONS_COMPLETED = "accel_positions_completed"

    // ==========================================
    // CALIBRATION KEYS - MAGNETOMETER
    // ==========================================

    const val KEY_MAG_OFFSET_X = "mag_offset_x"
    const val KEY_MAG_OFFSET_Y = "mag_offset_y"
    const val KEY_MAG_OFFSET_Z = "mag_offset_z"
    const val KEY_MAG_SCALE_X = "mag_scale_x"
    const val KEY_MAG_SCALE_Y = "mag_scale_y"
    const val KEY_MAG_SCALE_Z = "mag_scale_z"
    const val KEY_MAG_SAMPLES = "mag_samples"
    const val KEY_MAG_CALIBRATED = "mag_calibrated"

    // ==========================================
    // CALIBRATION KEYS - ACCELEROMETER POSITIONS
    // ==========================================

    const val KEY_ACCEL_POS_0_X = "accel_pos_0_x"
    const val KEY_ACCEL_POS_0_Y = "accel_pos_0_y"
    const val KEY_ACCEL_POS_0_Z = "accel_pos_0_z"
    const val KEY_ACCEL_POS_1_X = "accel_pos_1_x"
    const val KEY_ACCEL_POS_1_Y = "accel_pos_1_y"
    const val KEY_ACCEL_POS_1_Z = "accel_pos_1_z"
    const val KEY_ACCEL_POS_2_X = "accel_pos_2_x"
    const val KEY_ACCEL_POS_2_Y = "accel_pos_2_y"
    const val KEY_ACCEL_POS_2_Z = "accel_pos_2_z"
    const val KEY_ACCEL_POS_3_X = "accel_pos_3_x"
    const val KEY_ACCEL_POS_3_Y = "accel_pos_3_y"
    const val KEY_ACCEL_POS_3_Z = "accel_pos_3_z"
    const val KEY_ACCEL_POS_4_X = "accel_pos_4_x"
    const val KEY_ACCEL_POS_4_Y = "accel_pos_4_y"
    const val KEY_ACCEL_POS_4_Z = "accel_pos_4_z"
    const val KEY_ACCEL_POS_5_X = "accel_pos_5_x"
    const val KEY_ACCEL_POS_5_Y = "accel_pos_5_y"
    const val KEY_ACCEL_POS_5_Z = "accel_pos_5_z"

    // ==========================================
    // CALIBRATION KEYS - STATUS & PROGRESS
    // ==========================================

    const val KEY_CALIBRATION_STATUS = "calibration_status"
    const val KEY_CALIBRATION_QUALITY = "calibration_quality"
    const val KEY_CALIBRATION_QUALITY_FLOAT = "calibration_quality_float"
    const val KEY_CALIBRATION_PROGRESS = "calibration_progress"
    const val KEY_CALIBRATION_COMPLETE = "calibration_complete"
    const val KEY_CALIBRATION_TIMESTAMP = "calibration_timestamp"
    const val KEY_CALIBRATION_VERSION = "calibration_version"
    const val KEY_CALIBRATION_APPLIED = "calibration_applied"
    const val KEY_CALIBRATION_IN_PROGRESS = "calibration_in_progress"
    const val KEY_CALIBRATION_CURRENT_STEP = "calibration_current_step"
    const val KEY_CALIBRATION_DEVICE_MODEL = "calibration_device_model"
    const val KEY_CALIBRATION_ANDROID_VERSION = "calibration_android_version"
    const val KEY_CALIBRATION_ATTEMPTS = "calibration_attempts"

    // ==========================================
    // MOUSE SETTINGS KEYS
    // ==========================================

    const val KEY_SENSITIVITY = "sensitivity"
    const val KEY_SMOOTHING_ENABLED = "smoothing_enabled"
    const val KEY_SMOOTHING_FACTOR = "smoothing_factor"
    const val KEY_ACCELERATION_ENABLED = "acceleration_enabled"
    const val KEY_ACCELERATION_FACTOR = "acceleration_factor"
    const val KEY_INVERT_X = "invert_x"
    const val KEY_INVERT_Y = "invert_y"
    const val KEY_CLICK_THRESHOLD = "click_threshold"
    const val KEY_DOUBLE_CLICK_INTERVAL = "double_click_interval"
    const val KEY_SCROLL_THRESHOLD = "scroll_threshold"
    const val KEY_SCROLL_DEBOUNCE = "scroll_debounce"
    const val KEY_RIGHT_CLICK_TILT = "right_click_tilt"
    const val KEY_RIGHT_CLICK_DURATION = "right_click_duration"
    const val KEY_SWIPE_THRESHOLD = "swipe_threshold"
    const val KEY_GESTURE_COOLDOWN = "gesture_cooldown"
    const val KEY_CURSOR_SPEED = "cursor_speed"
    const val KEY_GESTURE_DEBOUNCE = "gesture_debounce"

    // ==========================================
    // AI & PREDICTIVE KEYS
    // ==========================================

    const val KEY_AI_SMOOTHING = "ai_smoothing"
    const val KEY_AI_BLEND_FACTOR = "ai_blend_factor"
    const val KEY_PREDICTIVE_MOVEMENT = "predictive_movement"
    const val KEY_PREDICTION_STRENGTH = "prediction_strength"
    const val KEY_KALMAN_ENABLED = "kalman_enabled"

    // ==========================================
    // HAPTIC & FEEDBACK KEYS
    // ==========================================

    const val KEY_HAPTIC_ENABLED = "haptic_enabled"
    const val KEY_HAPTIC_STRENGTH = "haptic_strength"
    const val KEY_SOUND_ENABLED = "sound_enabled"
    const val KEY_VISUAL_FEEDBACK = "visual_feedback"
    const val KEY_NOTIFICATION_ON_GESTURE = "notification_on_gesture"

    // ==========================================
    // DISPLAY & THEME KEYS
    // ==========================================

    const val KEY_THEME = "theme"
    const val KEY_ACCENT_COLOR = "accent_color"
    const val KEY_LANGUAGE = "language"
    const val KEY_DYNAMIC_COLORS = "dynamic_colors"
    const val KEY_FONT_SIZE = "font_size"
    const val KEY_SHOW_DEBUG_INFO = "show_debug_info"
    const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
    const val KEY_SHOW_FPS = "show_fps"

    // ==========================================
    // TOUCHPAD KEYS
    // ==========================================

    const val KEY_TOUCHPAD_ACTIVE = "touchpad_active"
    const val KEY_TOUCHPAD_SENSITIVITY = "touchpad_sensitivity"
    const val KEY_TOUCHPAD_CURSOR_SPEED = "touchpad_cursor_speed"
    const val KEY_TOUCHPAD_POINTER_SPEED = "touchpad_pointer_speed"
    const val KEY_TOUCHPAD_ACCELERATION = "touchpad_acceleration"
    const val KEY_TOUCHPAD_INVERT_VERTICAL = "touchpad_invert_vertical"
    const val KEY_TOUCHPAD_INVERT_HORIZONTAL = "touchpad_invert_horizontal"
    const val KEY_TOUCHPAD_SCROLL_SPEED = "touchpad_scroll_speed"
    const val KEY_TOUCHPAD_NATURAL_SCROLLING = "touchpad_natural_scrolling"
    const val KEY_TOUCHPAD_TWO_FINGER_SCROLL = "touchpad_two_finger_scroll"
    const val KEY_TOUCHPAD_EDGE_SCROLLING = "touchpad_edge_scrolling"
    const val KEY_TOUCHPAD_SCROLL_INERTIA = "touchpad_scroll_inertia"
    const val KEY_TOUCHPAD_TAP_TO_CLICK = "touchpad_tap_to_click"
    const val KEY_TOUCHPAD_DOUBLE_TAP_DELAY = "touchpad_double_tap_delay"
    const val KEY_TOUCHPAD_THREE_FINGER_SWIPE = "touchpad_three_finger_swipe"
    const val KEY_TOUCHPAD_PINCH_TO_ZOOM = "touchpad_pinch_to_zoom"
    const val KEY_TOUCHPAD_ROTATE_TO_ROTATE = "touchpad_rotate_to_rotate"
    const val KEY_TOUCHPAD_HAPTIC_FEEDBACK = "touchpad_haptic_feedback"
    const val KEY_TOUCHPAD_SHOW_TOUCH_POINTS = "touchpad_show_touch_points"

    // ==========================================
    // PRIVACY & DATA KEYS
    // ==========================================

    const val KEY_ANONYMOUS_STATS = "anonymous_stats"
    const val KEY_CRASH_REPORTING = "crash_reporting"
    const val KEY_CLEAR_DATA_ON_EXIT = "clear_data_on_exit"
    const val KEY_USER_NAME = "user_name"
    const val KEY_FIRST_LAUNCH = "first_launch"
    const val KEY_APP_VERSION = "app_version"

    // ==========================================
    // PRESENTATION KEYS
    // ==========================================

    const val KEY_PRESENTATION_MODE_ENABLED = "presentation_mode_enabled"
    const val KEY_LASER_POINTER_SPEED = "laser_pointer_speed"
    const val KEY_SHOW_PRESENTATION_TIMER = "show_presentation_timer"
    const val KEY_AUTO_HIDE_LASER = "auto_hide_laser"

    // ==========================================
    // ONBOARDING KEYS
    // ==========================================

    const val KEY_ONBOARDING_COMPLETE = "onboarding_completed"
    const val KEY_ONBOARDING_VERSION = "onboarding_version"

    // ==========================================
    // VOICE COMMAND KEYS
    // ==========================================

    const val KEY_VOICE_ENABLED = "voice_enabled"
    const val KEY_VOICE_WAKE_WORD = "wake_word"
    const val KEY_VOICE_WAKE_WORD_ENABLED = "wake_word_enabled"
    const val KEY_VOICE_WAKE_WORD_CONFIDENCE = "voice_wake_word_confidence"
    const val KEY_VOICE_HAPTIC_FEEDBACK = "voice_haptic_feedback"
    const val KEY_VOICE_COMMAND_HISTORY = "voice_command_history"
    const val KEY_VOICE_CONTINUOUS = "voice_continuous"
    const val KEY_VOICE_FEEDBACK = "voice_feedback"
    const val KEY_VOICE_SOUND_EFFECTS = "voice_sound_effects"
    const val KEY_VOICE_LANGUAGE = "voice_language"
    const val KEY_VOICE_SENSITIVITY = "voice_sensitivity"

    // ==========================================
    // PROXIMITY KEYS
    // ==========================================

    const val KEY_PROXIMITY_ENABLED = "proximity_enabled"
    const val KEY_PROXIMITY_DEVICE_MAC = "proximity_device_mac"
    const val KEY_PROXIMITY_NEAR_THRESHOLD = "proximity_near_threshold"
    const val KEY_PROXIMITY_NEAR = "proximity_near"
    const val KEY_PROXIMITY_FAR_THRESHOLD = "proximity_far_threshold"
    const val KEY_PROXIMITY_FAR = "proximity_far"
    const val KEY_PROXIMITY_TX_POWER = "proximity_tx_power"
    const val KEY_PROXIMITY_PATH_LOSS = "proximity_path_loss"
    const val KEY_PROXIMITY_VIBRATION = "proximity_vibration"
    const val KEY_PROXIMITY_NOTIFICATION = "proximity_notification"

    // ==========================================
    // EDGE GESTURES KEYS
    // ==========================================

    const val KEY_EDGE_GESTURES_ENABLED = "edge_gestures_enabled"
    const val KEY_EDGE_GESTURES_VOLUME_UP = "edge_gestures_volume_up"
    const val KEY_EDGE_GESTURES_VOLUME_DOWN = "edge_gestures_volume_down"
    const val KEY_EDGE_GESTURES_LONG_PRESS = "edge_gestures_long_press"
    const val KEY_EDGE_GESTURES_SENSITIVITY = "edge_gestures_sensitivity"

    // ==========================================
    // BATTERY SAVER KEYS
    // ==========================================

    const val KEY_BATTERY_SAVER_ENABLED = "battery_saver_enabled"
    const val KEY_BATTERY_SAVER_IDLE_TIME = "battery_saver_idle_time"

    // ==========================================
    // ACCESSIBILITY KEYS
    // ==========================================

    const val KEY_ANNOUNCE_MOVEMENT = "announce_movement"
    const val KEY_ANNOUNCE_CLICKS = "announce_clicks"
    const val KEY_HIGH_CONTRAST = "high_contrast"
    const val KEY_LARGE_TEXT = "large_text"
    const val KEY_REDUCE_MOTION = "reduce_motion"
    const val KEY_COLOR_BLIND_MODE = "color_blind_mode"

    // ==========================================
    // STATISTICS KEYS
    // ==========================================

    const val KEY_STAT_CLICKS = "stat_clicks"
    const val KEY_STAT_DOUBLE_CLICKS = "stat_double_clicks"
    const val KEY_STAT_RIGHT_CLICKS = "stat_right_clicks"
    const val KEY_STAT_SCROLLS = "stat_scrolls"
    const val KEY_STAT_GESTURES = "stat_gestures"
    const val KEY_STAT_TOTAL_DISTANCE = "stat_total_distance"
    const val KEY_STAT_TOTAL_MOVEMENTS = "stat_total_movements"
    const val KEY_STAT_SESSION_TIME = "stat_session_time"
    const val KEY_SESSION_CLICKS = "session_clicks"
    const val KEY_SESSION_DOUBLE_CLICKS = "session_double_clicks"
    const val KEY_SESSION_RIGHT_CLICKS = "session_right_clicks"
    const val KEY_SESSION_SCROLLS = "session_scrolls"
    const val KEY_SESSION_MOVEMENTS = "session_movements"
    const val KEY_SESSION_DISTANCE = "session_distance"
    const val KEY_SESSION_AVG_SPEED = "session_avg_speed"
    const val KEY_SESSION_MAX_SPEED = "session_max_speed"
    const val KEY_SESSION_START = "session_start"

    // ==========================================
    // PROFILES KEYS
    // ==========================================

    const val KEY_LAST_USED_PROFILE = "last_used_profile"
    const val KEY_PROFILE_PREFIX = "profile_"
    const val KEY_PROFILE_SENSITIVITY_SUFFIX = "_sensitivity"
    const val KEY_PROFILE_CLICK_THRESHOLD_SUFFIX = "_click_threshold"
    const val KEY_PROFILE_SCROLL_THRESHOLD_SUFFIX = "_scroll_threshold"
    const val KEY_PROFILE_LAST_USED_SUFFIX = "_last_used"

    // ==========================================
    // CUSTOM GESTURES KEYS
    // ==========================================

    const val KEY_GESTURE_PREFIX = "gesture_"
    const val KEY_RECORDED_GESTURES = "recorded_gestures"

    // ==========================================
    // DEVELOPER KEYS
    // ==========================================

    const val KEY_DEVELOPER_MODE = "developer_mode"
    const val KEY_EXPERIMENTAL_FEATURES = "experimental_features"
    const val KEY_LOG_LEVEL = "log_level"

    // ==========================================
    // CALIBRATION HELPER FUNCTIONS
    // ==========================================

    /**
     * Gets the preference key for a specific accelerometer position and axis.
     */
    fun getAccelPositionKey(position: Int, axis: String): String {
        return when (axis.lowercase()) {
            "x" -> when (position) {
                0 -> KEY_ACCEL_POS_0_X
                1 -> KEY_ACCEL_POS_1_X
                2 -> KEY_ACCEL_POS_2_X
                3 -> KEY_ACCEL_POS_3_X
                4 -> KEY_ACCEL_POS_4_X
                5 -> KEY_ACCEL_POS_5_X
                else -> "accel_pos_${position}_x"
            }
            "y" -> when (position) {
                0 -> KEY_ACCEL_POS_0_Y
                1 -> KEY_ACCEL_POS_1_Y
                2 -> KEY_ACCEL_POS_2_Y
                3 -> KEY_ACCEL_POS_3_Y
                4 -> KEY_ACCEL_POS_4_Y
                5 -> KEY_ACCEL_POS_5_Y
                else -> "accel_pos_${position}_y"
            }
            "z" -> when (position) {
                0 -> KEY_ACCEL_POS_0_Z
                1 -> KEY_ACCEL_POS_1_Z
                2 -> KEY_ACCEL_POS_2_Z
                3 -> KEY_ACCEL_POS_3_Z
                4 -> KEY_ACCEL_POS_4_Z
                5 -> KEY_ACCEL_POS_5_Z
                else -> "accel_pos_${position}_z"
            }
            else -> "accel_pos_${position}_${axis}"
        }
    }

    /**
     * Checks if a key is related to calibration.
     */
    fun isCalibrationKey(key: String): Boolean {
        return key.startsWith("gyro_") ||
                key.startsWith("accel_") ||
                key.startsWith("mag_") ||
                key.startsWith("calibration_")
    }

    /**
     * Gets all calibration-related keys.
     */
    fun getCalibrationKeys(): List<String> {
        return listOf(
            // Gyroscope
            KEY_GYRO_BIAS_X, KEY_GYRO_BIAS_Y, KEY_GYRO_BIAS_Z,
            KEY_GYRO_VARIANCE_X, KEY_GYRO_VARIANCE_Y, KEY_GYRO_VARIANCE_Z,
            KEY_GYRO_SAMPLES, KEY_GYRO_CALIBRATED,
            KEY_GYRO_OFFSET_X, KEY_GYRO_OFFSET_Y, KEY_GYRO_OFFSET_Z,
            // Accelerometer
            KEY_ACCEL_OFFSET_X, KEY_ACCEL_OFFSET_Y, KEY_ACCEL_OFFSET_Z,
            KEY_ACCEL_SCALE_X, KEY_ACCEL_SCALE_Y, KEY_ACCEL_SCALE_Z,
            KEY_ACCEL_CALIBRATED, KEY_ACCEL_POSITIONS_COMPLETED,
            KEY_ACCEL_POS_0_X, KEY_ACCEL_POS_0_Y, KEY_ACCEL_POS_0_Z,
            KEY_ACCEL_POS_1_X, KEY_ACCEL_POS_1_Y, KEY_ACCEL_POS_1_Z,
            KEY_ACCEL_POS_2_X, KEY_ACCEL_POS_2_Y, KEY_ACCEL_POS_2_Z,
            KEY_ACCEL_POS_3_X, KEY_ACCEL_POS_3_Y, KEY_ACCEL_POS_3_Z,
            KEY_ACCEL_POS_4_X, KEY_ACCEL_POS_4_Y, KEY_ACCEL_POS_4_Z,
            KEY_ACCEL_POS_5_X, KEY_ACCEL_POS_5_Y, KEY_ACCEL_POS_5_Z,
            // Magnetometer
            KEY_MAG_OFFSET_X, KEY_MAG_OFFSET_Y, KEY_MAG_OFFSET_Z,
            KEY_MAG_SCALE_X, KEY_MAG_SCALE_Y, KEY_MAG_SCALE_Z,
            KEY_MAG_SAMPLES, KEY_MAG_CALIBRATED,
            // Status
            KEY_CALIBRATION_STATUS, KEY_CALIBRATION_QUALITY,
            KEY_CALIBRATION_QUALITY_FLOAT,
            KEY_CALIBRATION_PROGRESS, KEY_CALIBRATION_COMPLETE,
            KEY_CALIBRATION_TIMESTAMP, KEY_CALIBRATION_VERSION,
            KEY_CALIBRATION_APPLIED, KEY_CALIBRATION_IN_PROGRESS,
            KEY_CALIBRATION_CURRENT_STEP, KEY_CALIBRATION_DEVICE_MODEL,
            KEY_CALIBRATION_ANDROID_VERSION, KEY_CALIBRATION_ATTEMPTS
        )
    }

    /**
     * Gets all profile-related keys for a specific profile name.
     */
    fun getProfileKeys(profileName: String): List<String> {
        return listOf(
            "$KEY_PROFILE_PREFIX${profileName}$KEY_PROFILE_SENSITIVITY_SUFFIX",
            "$KEY_PROFILE_PREFIX${profileName}$KEY_PROFILE_CLICK_THRESHOLD_SUFFIX",
            "$KEY_PROFILE_PREFIX${profileName}$KEY_PROFILE_SCROLL_THRESHOLD_SUFFIX",
            "$KEY_PROFILE_PREFIX${profileName}$KEY_PROFILE_LAST_USED_SUFFIX"
        )
    }

    /**
     * Gets all gesture-related keys.
     */
    fun getGestureKeys(): List<String> {
        // This would need to be dynamic - returns all keys starting with KEY_GESTURE_PREFIX
        return emptyList()
    }

    /**
     * Gets all statistical keys.
     */
    fun getStatKeys(): List<String> {
        return listOf(
            KEY_STAT_CLICKS, KEY_STAT_DOUBLE_CLICKS, KEY_STAT_RIGHT_CLICKS,
            KEY_STAT_SCROLLS, KEY_STAT_GESTURES, KEY_STAT_TOTAL_DISTANCE,
            KEY_STAT_TOTAL_MOVEMENTS, KEY_STAT_SESSION_TIME
        )
    }

    /**
     * Gets all session keys.
     */
    fun getSessionKeys(): List<String> {
        return listOf(
            KEY_SESSION_CLICKS, KEY_SESSION_DOUBLE_CLICKS, KEY_SESSION_RIGHT_CLICKS,
            KEY_SESSION_SCROLLS, KEY_SESSION_MOVEMENTS, KEY_SESSION_DISTANCE,
            KEY_SESSION_AVG_SPEED, KEY_SESSION_MAX_SPEED, KEY_SESSION_START
        )
    }

    /**
     * Gets all touchpad keys.
     */
    fun getTouchpadKeys(): List<String> {
        return listOf(
            KEY_TOUCHPAD_ACTIVE, KEY_TOUCHPAD_SENSITIVITY, KEY_TOUCHPAD_CURSOR_SPEED,
            KEY_TOUCHPAD_POINTER_SPEED, KEY_TOUCHPAD_ACCELERATION,
            KEY_TOUCHPAD_INVERT_VERTICAL, KEY_TOUCHPAD_INVERT_HORIZONTAL,
            KEY_TOUCHPAD_SCROLL_SPEED, KEY_TOUCHPAD_NATURAL_SCROLLING,
            KEY_TOUCHPAD_TWO_FINGER_SCROLL, KEY_TOUCHPAD_EDGE_SCROLLING,
            KEY_TOUCHPAD_SCROLL_INERTIA, KEY_TOUCHPAD_TAP_TO_CLICK,
            KEY_TOUCHPAD_DOUBLE_TAP_DELAY, KEY_TOUCHPAD_THREE_FINGER_SWIPE,
            KEY_TOUCHPAD_PINCH_TO_ZOOM, KEY_TOUCHPAD_ROTATE_TO_ROTATE,
            KEY_TOUCHPAD_HAPTIC_FEEDBACK, KEY_TOUCHPAD_SHOW_TOUCH_POINTS
        )
    }

    /**
     * Gets all voice command keys.
     */
    fun getVoiceKeys(): List<String> {
        return listOf(
            KEY_VOICE_ENABLED, KEY_VOICE_WAKE_WORD, KEY_VOICE_WAKE_WORD_ENABLED,
            KEY_VOICE_WAKE_WORD_CONFIDENCE, KEY_VOICE_HAPTIC_FEEDBACK,
            KEY_VOICE_COMMAND_HISTORY, KEY_VOICE_CONTINUOUS,
            KEY_VOICE_FEEDBACK, KEY_VOICE_SOUND_EFFECTS,
            KEY_VOICE_LANGUAGE, KEY_VOICE_SENSITIVITY
        )
    }

    /**
     * Gets all proximity keys.
     */
    fun getProximityKeys(): List<String> {
        return listOf(
            KEY_PROXIMITY_ENABLED, KEY_PROXIMITY_DEVICE_MAC,
            KEY_PROXIMITY_NEAR_THRESHOLD, KEY_PROXIMITY_NEAR,
            KEY_PROXIMITY_FAR_THRESHOLD, KEY_PROXIMITY_FAR,
            KEY_PROXIMITY_TX_POWER, KEY_PROXIMITY_PATH_LOSS,
            KEY_PROXIMITY_VIBRATION, KEY_PROXIMITY_NOTIFICATION
        )
    }

    /**
     * Gets all accessibility keys.
     */
    fun getAccessibilityKeys(): List<String> {
        return listOf(
            KEY_ANNOUNCE_MOVEMENT, KEY_ANNOUNCE_CLICKS,
            KEY_HIGH_CONTRAST, KEY_LARGE_TEXT,
            KEY_REDUCE_MOTION, KEY_COLOR_BLIND_MODE
        )
    }

    /**
     * Gets all edge gesture keys.
     */
    fun getEdgeGestureKeys(): List<String> {
        return listOf(
            KEY_EDGE_GESTURES_ENABLED, KEY_EDGE_GESTURES_VOLUME_UP,
            KEY_EDGE_GESTURES_VOLUME_DOWN, KEY_EDGE_GESTURES_LONG_PRESS,
            KEY_EDGE_GESTURES_SENSITIVITY
        )
    }
}
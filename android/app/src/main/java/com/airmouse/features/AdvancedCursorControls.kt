// AdvancedCursorControls.kt
package com.airmouse.features

import android.content.Context
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlin.math.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Advanced cursor controls: speed curves, sensitivity zones, edge scrolling,
 * momentum/inertia, and snap‑to‑grid.
 *
 * All settings are persisted in PreferencesManager and applied in real time.
 */
@Singleton
class AdvancedCursorControls @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager
) {

    // ==================== 1. Speed curves ====================
    enum class SpeedCurve {
        LINEAR,      // Direct 1:1 mapping
        EXPONENTIAL, // Faster as you tilt more
        LOGARITHMIC, // Fine control near center
        CUSTOM       // User-defined curve via points
    }

    var speedCurve: SpeedCurve
        get() = SpeedCurve.valueOf(prefs.getString("cursor_curve", "LINEAR"))
        set(value) { prefs.putString("cursor_curve", value.name) }

    var customCurvePoints: List<Pair<Float, Float>>
        get() = loadCurvePoints()
        set(value) { saveCurvePoints(value) }

    private fun loadCurvePoints(): List<Pair<Float, Float>> {
        val str = prefs.getString("cursor_curve_points", "")
        if (str.isEmpty()) return listOf(0f to 0f, 1f to 1f)
        return str.split(";").mapNotNull {
            val parts = it.split(",")
            if (parts.size == 2) parts[0].toFloat() to parts[1].toFloat() else null
        }
    }

    private fun saveCurvePoints(points: List<Pair<Float, Float>>) {
        val str = points.joinToString(";") { "${it.first},${it.second}" }
        prefs.putString("cursor_curve_points", str)
    }

    // ==================== 2. Sensitivity zones ====================
    data class SensitivityZone(
        val angleStart: Float,   // degrees
        val angleEnd: Float,
        val multiplier: Float,
        val acceleration: Float
    )

    var sensitivityZones: List<SensitivityZone>
        get() = loadZones()
        set(value) { saveZones(value) }

    private fun loadZones(): List<SensitivityZone> {
        val str = prefs.getString("cursor_zones", "")
        if (str.isEmpty()) return defaultZones()
        return str.split(";").mapNotNull {
            val parts = it.split(",")
            if (parts.size == 4) {
                SensitivityZone(
                    parts[0].toFloat(),
                    parts[1].toFloat(),
                    parts[2].toFloat(),
                    parts[3].toFloat()
                )
            } else null
        }.ifEmpty { defaultZones() }
    }

    private fun saveZones(zones: List<SensitivityZone>) {
        val str = zones.joinToString(";") {
            "${it.angleStart},${it.angleEnd},${it.multiplier},${it.acceleration}"
        }
        prefs.putString("cursor_zones", str)
    }

    private fun defaultZones() = listOf(
        SensitivityZone(0f, 15f, 1f, 1f),
        SensitivityZone(15f, 30f, 1.5f, 1.2f),
        SensitivityZone(30f, 45f, 2.5f, 1.5f),
        SensitivityZone(45f, 90f, 4f, 2f)
    )

    // ==================== 3. Edge scrolling (screen edges) ====================
    data class EdgeScrollConfig(
        val enabled: Boolean = true,
        val sensitivity: Float = 1.5f,
        val deadzone: Int = 50,        // pixels from edge
        val maxSpeed: Int = 20
    )

    var edgeScrollConfig: EdgeScrollConfig
        get() = EdgeScrollConfig(
            enabled = prefs.getBoolean("edge_scroll_enabled", true),
            sensitivity = prefs.getFloat("edge_scroll_sensitivity", 1.5f),
            deadzone = prefs.getInt("edge_scroll_deadzone", 50),
            maxSpeed = prefs.getInt("edge_scroll_max_speed", 20)
        )
        set(value) {
            prefs.putBoolean("edge_scroll_enabled", value.enabled)
            prefs.putFloat("edge_scroll_sensitivity", value.sensitivity)
            prefs.putInt("edge_scroll_deadzone", value.deadzone)
            prefs.putInt("edge_scroll_max_speed", value.maxSpeed)
        }

    // ==================== 4. Momentum / inertia ====================
    data class MomentumConfig(
        val enabled: Boolean = true,
        val friction: Float = 0.95f,
        val maxSpeed: Float = 50f,
        val duration: Long = 500L
    )

    var momentumConfig: MomentumConfig
        get() = MomentumConfig(
            enabled = prefs.getBoolean("momentum_enabled", true),
            friction = prefs.getFloat("momentum_friction", 0.95f),
            maxSpeed = prefs.getFloat("momentum_max_speed", 50f),
            duration = prefs.getLong("momentum_duration", 500L)
        )
        set(value) {
            prefs.putBoolean("momentum_enabled", value.enabled)
            prefs.putFloat("momentum_friction", value.friction)
            prefs.putFloat("momentum_max_speed", value.maxSpeed)
            prefs.putLong("momentum_duration", value.duration)
        }

    // ==================== 5. Snap to grid / magnetic edges ====================
    data class SnapConfig(
        val enabled: Boolean = false,
        val gridSize: Int = 10,
        val strength: Float = 0.8f
    )

    var snapConfig: SnapConfig
        get() = SnapConfig(
            enabled = prefs.getBoolean("snap_enabled", false),
            gridSize = prefs.getInt("snap_grid_size", 10),
            strength = prefs.getFloat("snap_strength", 0.8f)
        )
        set(value) {
            prefs.putBoolean("snap_enabled", value.enabled)
            prefs.putInt("snap_grid_size", value.gridSize)
            prefs.putFloat("snap_strength", value.strength)
        }

    // ==================== Core method: apply to raw deltas ====================
    /**
     * Apply all enabled cursor enhancements to raw movement deltas (dx, dy).
     * Returns the final (dx, dy) to be sent over the network.
     */
    fun apply(dx: Float, dy: Float, tiltAngle: Float = 0f, screenX: Int = 0, screenY: Int = 0): Pair<Float, Float> {
        var newDx = dx
        var newDy = dy

        // 1. Speed curve mapping (per‑axis) – if custom curve, interpolate
        newDx = applyCurve(newDx)
        newDy = applyCurve(newDy)

        // 2. Sensitivity zones based on tilt angle (e.g., angle of phone)
        val multiplier = getZoneMultiplier(tiltAngle)
        newDx *= multiplier
        newDy *= multiplier

        // 3. Edge scrolling (if enabled and finger near screen edge – used in touchpad mode)
        if (edgeScrollConfig.enabled && screenX != 0 && screenY != 0) {
            val screenWidth = context.resources.displayMetrics.widthPixels
            val screenHeight = context.resources.displayMetrics.heightPixels
            val edgeDx = edgeScrollDelta(screenX, screenWidth)
            val edgeDy = edgeScrollDelta(screenY, screenHeight)
            newDx += edgeDx
            newDy += edgeDy
        }

        // 4. Snap to grid (if enabled)
        if (snapConfig.enabled) {
            newDx = snapToGrid(newDx, snapConfig.gridSize, snapConfig.strength)
            newDy = snapToGrid(newDy, snapConfig.gridSize, snapConfig.strength)
        }

        // 5. Momentum is applied separately in the gesture handler – we return the instantaneous delta.
        //    The momentum handler (outside) will keep a velocity variable.
        return newDx to newDy
    }

    // Helper: apply curve to a single axis value
    private fun applyCurve(value: Float): Float {
        val sign = sign(value)
        val absVal = abs(value)
        return when (speedCurve) {
            SpeedCurve.LINEAR -> value
            SpeedCurve.EXPONENTIAL -> sign * (absVal.pow(1.5f))
            SpeedCurve.LOGARITHMIC -> sign * (log(1f + absVal * 10) / log(11f)) * (absVal)
            SpeedCurve.CUSTOM -> interpolateCurve(absVal) * sign
        }
    }

    private fun interpolateCurve(input: Float): Float {
        val points = customCurvePoints
        if (points.isEmpty()) return input
        if (input <= points.first().first) return points.first().second
        if (input >= points.last().first) return points.last().second
        for (i in 0 until points.size - 1) {
            val (x1, y1) = points[i]
            val (x2, y2) = points[i + 1]
            if (input in x1..x2) {
                val t = (input - x1) / (x2 - x1)
                return y1 + t * (y2 - y1)
            }
        }
        return input
    }

    private fun getZoneMultiplier(angle: Float): Float {
        val absAngle = abs(angle)
        for (zone in sensitivityZones) {
            if (absAngle in zone.angleStart..zone.angleEnd) {
                return zone.multiplier
            }
        }
        return 1f
    }

    private fun edgeScrollDelta(pos: Int, total: Int): Float {
        val distanceFromEdge = min(pos, total - pos)
        if (distanceFromEdge > edgeScrollConfig.deadzone) return 0f
        val strength = (1f - distanceFromEdge.toFloat() / edgeScrollConfig.deadzone) * edgeScrollConfig.sensitivity
        val direction = if (pos < total / 2) -1f else 1f
        val delta = direction * strength * edgeScrollConfig.maxSpeed
        return delta.coerceIn(-edgeScrollConfig.maxSpeed.toFloat(), edgeScrollConfig.maxSpeed.toFloat())
    }

    private fun snapToGrid(value: Float, gridSize: Int, strength: Float): Float {
        if (abs(value) < 0.5f) return 0f
        val nearestGrid = round(value / gridSize) * gridSize
        val diff = nearestGrid - value
        return value + diff * strength
    }
}
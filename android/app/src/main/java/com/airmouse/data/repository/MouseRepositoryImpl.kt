package com.airmouse.data.repository

import com.airmouse.domain.model.*
import com.airmouse.domain.repository.IMouseRepository
import com.airmouse.network.ConnectionManager
import com.airmouse.sensors.SensorDataProcessor
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.sqrt
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MouseRepositoryImpl @Inject constructor(
    private val connectionManager: ConnectionManager,
    private val prefs: PreferencesManager
) : IMouseRepository {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _profile = MutableStateFlow(MovementProfile())
    override fun observeMovementProfile(): Flow<MovementProfile> = _profile.asStateFlow()

    private val _statistics = MutableStateFlow(MouseStatistics())
    override fun observeStatistics(): Flow<MouseStatistics> = _statistics.asStateFlow()

    private val _events = MutableSharedFlow<MouseEvent>(extraBufferCapacity = 64)
    override fun observeMouseEvents(): Flow<MouseEvent> = _events.asSharedFlow()

    init {
        loadProfile()
        loadStatistics()
    }

    private fun loadProfile() {
        val profile = MovementProfile(
            sensitivity = prefs.getFloat("sensitivity", 1.0f),
            smoothingEnabled = prefs.getBoolean("smoothing_enabled", true),
            accelerationEnabled = prefs.getBoolean("acceleration_enabled", true),
            accelerationFactor = prefs.getFloat("acceleration_factor", 1.5f),
            invertX = prefs.getBoolean("invert_x", false),
            invertY = prefs.getBoolean("invert_y", false),
            deadband = prefs.getFloat("deadband", 0.5f),
            maxSpeed = prefs.getFloat("max_speed", 100f),
            minSpeed = prefs.getFloat("min_speed", 0.5f),
            predictiveBlend = prefs.getFloat("predictive_blend", 0.6f),
            smoothingAlpha = prefs.getFloat("smoothing_alpha", 0.3f)
        )
        _profile.value = profile
    }

    private fun loadStatistics() {
        val stats = MouseStatistics(
            totalClicks = prefs.getInt("click_count", 0),
            totalDoubleClicks = prefs.getInt("double_click_count", 0),
            totalRightClicks = prefs.getInt("right_click_count", 0),
            totalScrolls = prefs.getInt("scroll_count", 0),
            totalMovement = prefs.getFloat("total_movement", 0f),
            movementCount = prefs.getInt("movement_count", 0),
            averageSpeed = prefs.getFloat("avg_speed", 0f),
            lastUpdated = System.currentTimeMillis()
        )
        _statistics.value = stats
    }

    // ============================================================
    // Movement
    // ============================================================

    private var lowPassFilter: SensorDataProcessor.LowPassFilter? = null
    private var lastAlpha: Float = -1f

    private fun getOrCreateFilter(alpha: Float): SensorDataProcessor.LowPassFilter {
        var filter = lowPassFilter
        if (filter == null || lastAlpha != alpha) {
            filter = SensorDataProcessor.LowPassFilter(alpha)
            lowPassFilter = filter
            lastAlpha = alpha
        }
        return filter
    }

    override suspend fun move(dx: Float, dy: Float): Boolean {
        return try {
            val profile = _profile.value

            var filteredX = dx
            var filteredY = dy

            if (profile.smoothingEnabled) {
                val filter = getOrCreateFilter(profile.smoothingAlpha)
                val (fX, fY, _) = filter.filter(dx, dy, 0f)
                filteredX = fX
                filteredY = fY
            } else {
                lowPassFilter = null
                lastAlpha = -1f
            }

            var scaledX = filteredX * profile.sensitivity
            var scaledY = filteredY * profile.sensitivity

            if (abs(scaledX) < profile.deadband) scaledX = 0f
            if (abs(scaledY) < profile.deadband) scaledY = 0f

            if (scaledX == 0f && scaledY == 0f) return false

            if (profile.invertX) scaledX = -scaledX
            if (profile.invertY) scaledY = -scaledY

            if (profile.swapAxes) {
                val temp = scaledX
                scaledX = scaledY
                scaledY = temp
            }

            if (profile.accelerationEnabled) {
                val speed = sqrt(scaledX * scaledX + scaledY * scaledY)
                if (speed > 1.0f) {
                    val factor = 1.0f + profile.accelerationFactor * (speed / 10.0f)
                    val clampedFactor = factor.coerceAtMost(3.0f)
                    scaledX *= clampedFactor
                    scaledY *= clampedFactor
                }
            }

            currentX += scaledX.toInt()
            currentY += scaledY.toInt()

            val result = connectionManager.sendMove(scaledX, scaledY)

            if (result) {
                val distance = sqrt(scaledX * scaledX + scaledY * scaledY)
                updateStatistics(distance)
            }

            result
        } catch (e: Exception) {
            Timber.e(e, "Move failed")
            false
        }
    }

    override suspend fun moveSmooth(points: List<Pair<Float, Float>>, durationMs: Int): Boolean {
        if (points.isEmpty()) return true
        var success = true
        val stepDelay = durationMs / points.size

        points.forEach { (x, y) ->
            if (!move(x, y)) success = false
            if (stepDelay > 0) {
                delay(stepDelay.toLong())
            }
        }

        return success
    }

    override suspend fun stopMovement() {
        try {
            connectionManager.sendPauseMovement()
        } catch (e: Exception) {
            Timber.e(e, "Stop movement failed")
        }
    }

    override suspend fun resumeMovement() {
        try {
            connectionManager.sendResumeMovement()
        } catch (e: Exception) {
            Timber.e(e, "Resume movement failed")
        }
    }

    // ============================================================
    // Clicks
    // ============================================================

    override suspend fun click(button: MouseButton): Boolean {
        return try {
            val buttonName = when (button) {
                MouseButton.LEFT -> "left"
                MouseButton.RIGHT -> "right"
                MouseButton.MIDDLE -> "middle"
                MouseButton.BACK -> "back"
                MouseButton.FORWARD -> "forward"
            }
            val result = connectionManager.sendClick(buttonName)

            if (result) {
                updateClickStats(button)
                addEvent(MouseEvent.Click(button))
            }

            result
        } catch (e: Exception) {
            Timber.e(e, "Click failed")
            false
        }
    }

    override suspend fun doubleClick(): Boolean {
        return try {
            val result = connectionManager.sendDoubleClick()
            if (result) {
                updateClickStats(MouseButton.LEFT, isDouble = true)
                addEvent(MouseEvent.DoubleClick)
            }
            result
        } catch (e: Exception) {
            Timber.e(e, "Double click failed")
            false
        }
    }

    override suspend fun rightClick(): Boolean {
        return try {
            val result = connectionManager.sendRightClick()
            if (result) {
                updateClickStats(MouseButton.RIGHT)
                addEvent(MouseEvent.RightClick)
            }
            result
        } catch (e: Exception) {
            Timber.e(e, "Right click failed")
            false
        }
    }

    override suspend fun middleClick(): Boolean {
        return try {
            val result = connectionManager.sendClick("middle")
            if (result) {
                updateClickStats(MouseButton.MIDDLE)
                addEvent(MouseEvent.Click(MouseButton.MIDDLE))
            }
            result
        } catch (e: Exception) {
            Timber.e(e, "Middle click failed")
            false
        }
    }

    // ============================================================
    // Scroll & Gestures
    // ============================================================

    override suspend fun scroll(delta: Int): Boolean {
        return try {
            val result = connectionManager.sendScroll(delta)
            if (result) {
                val current = _statistics.value
                _statistics.value = current.copy(
                    totalScrolls = current.totalScrolls + 1,
                    lastUpdated = System.currentTimeMillis()
                )
                saveStatistics()
                val direction = if (delta > 0) ScrollDirection.UP else ScrollDirection.DOWN
                addEvent(MouseEvent.Scroll(delta, direction))
            }
            result
        } catch (e: Exception) {
            Timber.e(e, "Scroll failed")
            false
        }
    }

    override suspend fun sendGesture(gesture: String, confidence: Float): Boolean {
        return try {
            connectionManager.sendGesture(gesture, confidence)
        } catch (e: Exception) {
            Timber.e(e, "Send gesture failed")
            false
        }
    }

    // ============================================================
    // Position
    // ============================================================

    private var currentX: Int = 0
    private var currentY: Int = 0

    override suspend fun getPosition(): Pair<Int, Int> {
        return Pair(currentX, currentY)
    }

    override suspend fun setPosition(x: Int, y: Int): Boolean {
        currentX = x
        currentY = y
        return true
    }

    // ============================================================
    // Profile
    // ============================================================

    override suspend fun getMovementProfile(): MovementProfile = _profile.value

    override suspend fun setMovementProfile(profile: MovementProfile) {
        _profile.value = profile
        prefs.putFloat("sensitivity", profile.sensitivity)
        prefs.putBoolean("smoothing_enabled", profile.smoothingEnabled)
        prefs.putBoolean("acceleration_enabled", profile.accelerationEnabled)
        prefs.putFloat("acceleration_factor", profile.accelerationFactor)
        prefs.putBoolean("invert_x", profile.invertX)
        prefs.putBoolean("invert_y", profile.invertY)
        prefs.putFloat("deadband", profile.deadband)
        prefs.putFloat("max_speed", profile.maxSpeed)
        prefs.putFloat("min_speed", profile.minSpeed)
        prefs.putFloat("predictive_blend", profile.predictiveBlend)
        prefs.putFloat("smoothing_alpha", profile.smoothingAlpha)
    }

    // ============================================================
    // Statistics
    // ============================================================

    override suspend fun getStatistics(): MouseStatistics = _statistics.value

    override suspend fun resetStatistics() {
        _statistics.value = MouseStatistics()
        saveStatistics()
        prefs.remove("click_count")
        prefs.remove("double_click_count")
        prefs.remove("right_click_count")
        prefs.remove("scroll_count")
        prefs.remove("total_movement")
        prefs.remove("movement_count")
        prefs.remove("avg_speed")
    }

    override suspend fun clearEvents() {
        // Clear events (implementation depends on event storage)
    }

    // ============================================================
    // Private Helpers
    // ============================================================

    private fun updateStatistics(distance: Float) {
        val current = _statistics.value
        val newMovementCount = current.movementCount + 1
        val newTotalMovement = current.totalMovement + distance
        val newAvgSpeed = if (newMovementCount > 0) newTotalMovement / newMovementCount else 0f

        _statistics.value = current.copy(
            movementCount = newMovementCount,
            totalMovement = newTotalMovement,
            averageSpeed = newAvgSpeed,
            lastUpdated = System.currentTimeMillis()
        )
        saveStatistics()
    }

    private fun updateClickStats(button: MouseButton, isDouble: Boolean = false) {
        val current = _statistics.value
        _statistics.value = when {
            isDouble -> current.copy(
                totalDoubleClicks = current.totalDoubleClicks + 1,
                lastUpdated = System.currentTimeMillis()
            )
            button == MouseButton.LEFT -> current.copy(
                totalClicks = current.totalClicks + 1,
                lastUpdated = System.currentTimeMillis()
            )
            button == MouseButton.RIGHT -> current.copy(
                totalRightClicks = current.totalRightClicks + 1,
                lastUpdated = System.currentTimeMillis()
            )
            else -> current.copy(
                lastUpdated = System.currentTimeMillis()
            )
        }
        saveStatistics()
    }

    private fun saveStatistics() {
        val stats = _statistics.value
        prefs.putInt("click_count", stats.totalClicks)
        prefs.putInt("double_click_count", stats.totalDoubleClicks)
        prefs.putInt("right_click_count", stats.totalRightClicks)
        prefs.putInt("scroll_count", stats.totalScrolls)
        prefs.putFloat("total_movement", stats.totalMovement)
        prefs.putInt("movement_count", stats.movementCount)
        prefs.putFloat("avg_speed", stats.averageSpeed)
    }

    private fun addEvent(event: MouseEvent) {
        _events.tryEmit(event)
    }
}
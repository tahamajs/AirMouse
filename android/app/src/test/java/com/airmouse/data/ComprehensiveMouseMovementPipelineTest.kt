package com.airmouse.data

import com.airmouse.data.repository.MouseRepositoryImpl
import com.airmouse.domain.model.MovementProfile
import com.airmouse.network.ConnectionManager
import com.airmouse.sensors.OneEuroFilter
import com.airmouse.utils.PreferencesManager
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

/**
 * Complete End-to-End Test Suite for Mouse Movement, Smoothing, Deadband, Acceleration, and Protocol Transmission.
 */
class ComprehensiveMouseMovementPipelineTest {

    private val mockConnectionManager = mockk<ConnectionManager>(relaxed = true)
    private val mockPrefs = mockk<PreferencesManager>(relaxed = true)
    private lateinit var mouseRepository: MouseRepositoryImpl

    @Before
    fun setUp() {
        // Setup clean mock preferences without legacy lowPassFilter carrying over between single steps
        every { mockPrefs.getFloat("sensitivity", any()) } returns 1.5f
        every { mockPrefs.getBoolean("smoothing_enabled", any()) } returns false
        every { mockPrefs.getBoolean("acceleration_enabled", any()) } returns true
        every { mockPrefs.getFloat("acceleration_factor", any()) } returns 1.5f
        every { mockPrefs.getBoolean("invert_x", any()) } returns false
        every { mockPrefs.getBoolean("invert_y", any()) } returns false
        every { mockPrefs.getFloat("deadband", any()) } returns 0.2f
        every { mockPrefs.getFloat("max_speed", any()) } returns 200f
        every { mockPrefs.getFloat("min_speed", any()) } returns 0.1f
        every { mockPrefs.getFloat("predictive_blend", any()) } returns 0.6f
        every { mockPrefs.getFloat("smoothing_alpha", any()) } returns 0.3f
        every { mockPrefs.getInt(any(), any()) } returns 0
        every { mockPrefs.getFloat("total_movement", any()) } returns 0f
        every { mockPrefs.getFloat("avg_speed", any()) } returns 0f

        every { mockConnectionManager.sendMove(any(), any()) } returns true
        every { mockConnectionManager.isMouseControlEnabled } returns true

        mouseRepository = MouseRepositoryImpl(mockConnectionManager, mockPrefs)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testSubtleMovementBelowDeadband_isRejectedToPreventJitter() = runBlocking {
        // When movement is within the deadband (< 0.2), sendMove should not be invoked
        val result = mouseRepository.move(0.05f, 0.05f)
        assertFalse("Sub-deadband jitter must be filtered out", result)
        verify(exactly = 0) { mockConnectionManager.sendMove(any(), any()) }
    }

    @Test
    fun testIntentionalMovement_isScaledAndTransmitted() = runBlocking {
        val result = mouseRepository.move(2.0f, -1.5f)
        assertTrue("Legitimate intentional movement must be transmitted", result)

        val slotX = slot<Float>()
        val slotY = slot<Float>()
        verify(atLeast = 1) { mockConnectionManager.sendMove(capture(slotX), capture(slotY)) }

        assertTrue("Transmitted X must be positive", slotX.captured > 0f)
        assertTrue("Transmitted Y must be negative", slotY.captured < 0f)
    }

    @Test
    fun testFourDirectionalCardinalMovement() = runBlocking {
        val directions = listOf(
            Pair(4.0f, 0.0f) to "Right",
            Pair(0.0f, 4.0f) to "Down",
            Pair(-4.0f, 0.0f) to "Left",
            Pair(0.0f, -4.0f) to "Up"
        )

        for ((delta, name) in directions) {
            clearMocks(mockConnectionManager, answers = false)
            val success = mouseRepository.move(delta.first, delta.second)
            assertTrue("Movement in direction $name failed", success)

            val slotX = slot<Float>()
            val slotY = slot<Float>()
            verify(exactly = 1) { mockConnectionManager.sendMove(capture(slotX), capture(slotY)) }

            when (name) {
                "Right" -> {
                    assertTrue("Right: X must be > 0", slotX.captured > 0f)
                    assertEquals("Right: Y must be 0", 0f, slotY.captured, 0.001f)
                }
                "Down" -> {
                    assertEquals("Down: X must be 0", 0f, slotX.captured, 0.001f)
                    assertTrue("Down: Y must be > 0", slotY.captured > 0f)
                }
                "Left" -> {
                    assertTrue("Left: X must be < 0", slotX.captured < 0f)
                    assertEquals("Left: Y must be 0", 0f, slotY.captured, 0.001f)
                }
                "Up" -> {
                    assertEquals("Up: X must be 0", 0f, slotX.captured, 0.001f)
                    assertTrue("Up: Y must be < 0", slotY.captured < 0f)
                }
            }
        }
    }

    @Test
    fun testOneEuroAdaptiveFilterEliminatesTremorWithoutLag() {
        val filter = OneEuroFilter(minCutoff = 1.0, beta = 0.05, dCutoff = 1.0)
        var t = 1000L

        // Baseline stationary state with physiological micro-tremor
        val stationaryFiltered = mutableListOf<Float>()
        for (i in 0 until 40) {
            val microTremor = if (i % 2 == 0) 0.15f else -0.15f
            stationaryFiltered.add(filter.filter(microTremor, t))
            t += 16L
        }

        // Tail 15 samples should be well damped
        val steadyJitter = stationaryFiltered.takeLast(15).maxOf { abs(it) }
        assertTrue("Steady-state resting jitter must be damped below 0.12 by OneEuro filter (got $steadyJitter)", steadyJitter < 0.12f)

        // Rapid deliberate motion burst
        var rapidFiltered = 0f
        for (i in 0 until 10) {
            rapidFiltered = filter.filter(15.0f, t)
            t += 16L
        }
        assertTrue("Filter must follow rapid motion closely (got $rapidFiltered)", rapidFiltered > 10.0f)
    }
}

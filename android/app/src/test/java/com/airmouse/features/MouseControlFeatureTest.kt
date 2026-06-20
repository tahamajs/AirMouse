package com.airmouse.features

import com.airmouse.domain.model.MouseButton
import com.airmouse.domain.model.MouseEvent
import com.airmouse.domain.model.MouseStatistics
import com.airmouse.domain.model.MovementProfile
import com.airmouse.domain.usecase.SendMovementUseCase
import com.airmouse.domain.repository.IMouseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MouseControlFeatureTest {

    private val sendMovementUseCase = mockk<SendMovementUseCase>()
    private val mouseRepository = mockk<IMouseRepository>()
    private val feature = MouseControlFeature(sendMovementUseCase, mouseRepository)

    @Test
    fun `pause blocks motion until resumed`() = runTest {
        coEvery { sendMovementUseCase.pauseMovement() } returns Result.success(true)
        coEvery { sendMovementUseCase.resumeMovement() } returns Result.success(true)

        assertFalse(feature.isPaused())
        assertTrue(feature.pauseMovement().isSuccess)
        assertTrue(feature.isPaused())
        assertTrue(feature.resumeMovement().isSuccess)
        assertFalse(feature.isPaused())
    }

    @Test
    fun `move updates speed and forwards movement`() = runTest {
        coEvery { sendMovementUseCase.invoke(any(), any()) } returns Result.success(true)

        val result = feature.move(3f, 4f)

        assertTrue(result.isSuccess)
        assertEquals(5f, feature.getCurrentSpeed(), 0.0001f)
        coVerify(exactly = 1) { sendMovementUseCase.invoke(3f, 4f) }
    }

    @Test
    fun `setMovementProfile updates exposed state`() = runTest {
        val profile = MovementProfile(
            sensitivity = 1.4f,
            smoothingEnabled = false,
            accelerationEnabled = false
        )

        coEvery { mouseRepository.setMovementProfile(profile) } returns Unit

        feature.setMovementProfile(profile)

        assertEquals(1.4f, feature.state.value.sensitivity, 0.0001f)
        assertFalse(feature.state.value.smoothingEnabled)
        assertFalse(feature.state.value.accelerationEnabled)
    }

    @Test
    fun `observeMouseEvents delegates repository flow`() = runTest {
        val events = MutableSharedFlow<MouseEvent>()
        every { mouseRepository.observeMouseEvents() } returns events

        assertEquals(events, feature.observeMouseEvents())
    }

    @Test
    fun `getStatistics delegates repository`() = runTest {
        val stats = mockk<MouseStatistics>(relaxed = true)
        coEvery { mouseRepository.getStatistics() } returns stats

        assertEquals(stats, feature.getStatistics())
    }
}

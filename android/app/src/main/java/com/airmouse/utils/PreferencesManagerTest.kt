package com.airmouse.utils

import android.content.Context
import android.content.SharedPreferences
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class PreferencesManagerTest {

    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var preferencesManager: PreferencesManager

    @Before
    fun setup() {
        mockPrefs = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.putFloat(any(), any()) } returns mockEditor
        every { mockEditor.putInt(any(), any()) } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.putBoolean(any(), any()) } returns mockEditor
        every { mockEditor.apply() } just Runs

        val mockContext = mockk<Context>()
        every { mockContext.getSharedPreferences(any(), any()) } returns mockPrefs

        preferencesManager = PreferencesManager(mockContext)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun setSensitivity_savesValue() {
        preferencesManager.setSensitivity(0.8f)
        verify { mockEditor.putFloat("sensitivity", 0.8f) }
        verify { mockEditor.apply() }
    }

    @Test
    fun getSensitivity_returnsDefaultWhenNotSet() {
        every { mockPrefs.getFloat("sensitivity", 0.5f) } returns 0.5f
        assertEquals(0.5f, preferencesManager.getSensitivity())
    }

    @Test
    fun incrementClick_incrementsCounter() {
        every { mockPrefs.getInt("click_count", 0) } returns 5
        preferencesManager.incrementClick()
        verify { mockEditor.putInt("click_count", 6) }
    }

    @Test
    fun saveCalibration_storesGyroBias() {
        val bias = floatArrayOf(0.1f, -0.05f, 0.02f)
        preferencesManager.saveGyroBias(bias)
        verify { mockEditor.putFloat("gyro_bias_x", 0.1f) }
        verify { mockEditor.putFloat("gyro_bias_y", -0.05f) }
        verify { mockEditor.putFloat("gyro_bias_z", 0.02f) }
        verify { mockEditor.apply() }
    }
}
package com.airmouse.network

import com.airmouse.data.repository.ConnectionRepositoryImpl
import com.airmouse.domain.model.ConnectionConfig
import com.airmouse.domain.model.ConnectionProtocol
import com.airmouse.domain.model.ConnectionStatus
import com.airmouse.utils.PreferencesManager
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConnectionRepositoryTest {

    private lateinit var mockConnectionManager: ConnectionManager
    private lateinit var mockUdpDiscovery: UdpDiscovery
    private lateinit var mockPrefs: PreferencesManager
    private lateinit var repository: ConnectionRepositoryImpl

    @Before
    fun setUp() {
        mockConnectionManager = mockk(relaxed = true)
        mockUdpDiscovery = mockk(relaxed = true)
        mockPrefs = mockk(relaxed = true)

        every { mockPrefs.getString("last_ip", any()) } returns "192.168.1.50"
        every { mockPrefs.getInt("last_port", any()) } returns 8082
        every { mockPrefs.getString("last_protocol", any()) } returns "WEBSOCKET"
        every { mockPrefs.getBoolean("calibration_complete", false) } returns true

        repository = ConnectionRepositoryImpl(mockConnectionManager, mockUdpDiscovery, mockPrefs)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testConnectWithValidConfig() = runTest {
        coEvery { mockConnectionManager.connect(any(), any()) } returns true

        val config = ConnectionConfig(
            ip = "192.168.1.10",
            port = 8082,
            protocol = ConnectionProtocol.WEBSOCKET
        )

        val result = repository.connect(config)
        assertTrue(result)
        verify { mockConnectionManager.setProtocol(ConnectionManager.ConnectionProtocol.WEBSOCKET) }
        coVerify { mockConnectionManager.connect("192.168.1.10", 8082) }
    }

    @Test
    fun testDisconnect() = runTest {
        coEvery { mockConnectionManager.disconnect() } just runs

        repository.disconnect()
        coVerify { mockConnectionManager.disconnect() }
    }

    @Test
    fun testSendMessage() = runTest {
        every { mockConnectionManager.send(any()) } returns true

        val success = repository.sendMessage("test-message")
        assertTrue(success)
        verify { mockConnectionManager.send("test-message") }
    }
}

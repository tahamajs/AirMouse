package com.airmouse.network

import android.bluetooth.BluetoothAdapter
import android.content.Context
import com.airmouse.domain.repository.IStatisticsRepository
import com.airmouse.utils.PreferencesManager
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConnectionManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: PreferencesManager
    private lateinit var mockBluetoothAdapter: BluetoothAdapter
    private lateinit var mockBluetoothHidHelper: BluetoothHidMouseHelper
    private lateinit var mockStatsRepo: IStatisticsRepository
    private lateinit var mockUsbDataSource: com.airmouse.data.datasource.remote.IUsbDataSource
    private lateinit var connectionManager: ConnectionManager

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockPrefs = mockk(relaxed = true)
        mockBluetoothAdapter = mockk(relaxed = true)
        mockBluetoothHidHelper = mockk(relaxed = true)
        mockStatsRepo = mockk(relaxed = true)
        mockUsbDataSource = mockk(relaxed = true)

        // Mock some basic preferences returns
        every { mockPrefs.getString("last_ip", any()) } returns "192.168.1.100"
        every { mockPrefs.getInt("last_port", any()) } returns 8082
        every { mockPrefs.getString("last_protocol", any()) } returns "UDP"

        connectionManager = ConnectionManager(
            mockContext,
            mockPrefs,
            mockBluetoothAdapter,
            mockBluetoothHidHelper,
            mockStatsRepo,
            mockUsbDataSource
        )
    }

    @After
    fun tearDown() {
        connectionManager.cleanup()
        unmockkAll()
    }

    @Test
    fun testProtocolSettings() {
        connectionManager.setProtocol(ConnectionManager.ConnectionProtocol.TCP)
        assertEquals(ConnectionManager.ConnectionProtocol.TCP, connectionManager.getCurrentProtocol())
        verify { mockPrefs.putString("last_protocol", "TCP") }
        verify { mockPrefs.putString("connection_protocol", "TCP") }
    }

    @Test
    fun testMouseControlToggle() {
        connectionManager.isMouseControlEnabled = true
        assertTrue(connectionManager.isMouseControlEnabled)

        connectionManager.isMouseControlEnabled = false
        assertFalse(connectionManager.isMouseControlEnabled)
    }

    @Test
    fun testSendMoveDisabled() {
        connectionManager.isMouseControlEnabled = false
        val sent = connectionManager.sendMove(10f, -5f)
        assertFalse("Should return false and not send when mouse control is disabled", sent)
    }

    @Test
    fun testSendMoveBluetooth() {
        connectionManager.setProtocol(ConnectionManager.ConnectionProtocol.BLUETOOTH)
        connectionManager.isMouseControlEnabled = true

        // Bluetooth uses integer accumulation and reports back to bluetoothHidMouseHelper
        val sent = connectionManager.sendMove(12.5f, -8.3f)
        assertTrue("Should return true for Bluetooth move simulation", sent)
        verify { mockBluetoothHidHelper.sendMouseReport(any(), 12.toByte(), (-8).toByte(), 0.toByte()) }
    }

    @Test
    fun testSendClickBluetooth() {
        connectionManager.setProtocol(ConnectionManager.ConnectionProtocol.BLUETOOTH)
        connectionManager.isMouseControlEnabled = true

        val sent = connectionManager.sendClick(ConnectionManager.MessageTypes.BUTTON_LEFT)
        assertTrue(sent)
        // Verify click presses button then releases it
        verifyOrder {
            mockBluetoothHidHelper.sendMouseReport(1.toByte(), 0, 0, 0)
            mockBluetoothHidHelper.sendMouseReport(0.toByte(), 0, 0, 0)
        }
    }

    @Test
    fun testGetConnectionStats() {
        connectionManager.setProtocol(ConnectionManager.ConnectionProtocol.WEBSOCKET)
        val stats = connectionManager.getConnectionStats()
        assertEquals("DISCONNECTED", stats["status"])
        assertEquals("WEBSOCKET", stats["protocol"])
        assertEquals(8082, stats["port"])
    }

    @Test
    fun testDisconnect() {
        connectionManager.disconnect()
        assertFalse(connectionManager.isMouseControlEnabled)
        assertEquals(ConnectionManager.ConnectionStatus.DISCONNECTED, connectionManager.connectionStatus.value)
    }
}

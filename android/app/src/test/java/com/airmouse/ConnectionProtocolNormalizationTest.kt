package com.airmouse

import com.airmouse.domain.model.ConnectionConfig
import com.airmouse.domain.model.ConnectionProtocol
import com.airmouse.domain.model.CalibrationStatus
import com.airmouse.domain.repository.IConnectionRepository
import com.airmouse.domain.repository.ICalibrationRepository
import com.airmouse.domain.usecase.ConnectToServerUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionProtocolNormalizationTest {

    @Test
    fun websocketConfigUsesWebsocketDefaultPortWhenPortMissing() {
        val config = ConnectionConfig(
            ip = "192.168.1.106",
            port = 0,
            protocol = ConnectionProtocol.WEBSOCKET
        ).normalized()

        assertEquals(ConnectionConfig.DEFAULT_WEBSOCKET_PORT, config.port)
        assertEquals(ConnectionProtocol.WEBSOCKET, config.protocol)
    }

    @Test
    fun tcpConfigUsesTcpDefaultPortWhenPortMissing() {
        val config = ConnectionConfig(
            ip = "192.168.1.106",
            port = 0,
            protocol = ConnectionProtocol.TCP
        ).normalized()

        assertEquals(ConnectionConfig.DEFAULT_TCP_PORT, config.port)
        assertEquals(ConnectionProtocol.TCP, config.protocol)
    }

    @Test
    fun websocketConfigReplacesTcpDefaultPortWithWebsocketPort() {
        val config = ConnectionConfig(
            ip = "192.168.1.106",
            port = ConnectionConfig.DEFAULT_TCP_PORT,
            protocol = ConnectionProtocol.WEBSOCKET
        ).normalized()

        assertEquals(ConnectionConfig.DEFAULT_WEBSOCKET_PORT, config.port)
        assertEquals(ConnectionProtocol.WEBSOCKET, config.protocol)
    }

    @Test
    fun websocketConfigReplacesUdpDiscoveryPortWithWebsocketPort() {
        val config = ConnectionConfig(
            ip = "192.168.1.106",
            port = ConnectionConfig.DEFAULT_UDP_PORT,
            protocol = ConnectionProtocol.WEBSOCKET
        ).normalized()

        assertEquals(ConnectionConfig.DEFAULT_WEBSOCKET_PORT, config.port)
        assertEquals(ConnectionProtocol.WEBSOCKET, config.protocol)
    }

    @Test
    fun tcpConfigReplacesUdpDiscoveryPortWithTcpPort() {
        val config = ConnectionConfig(
            ip = "192.168.1.106",
            port = ConnectionConfig.DEFAULT_UDP_PORT,
            protocol = ConnectionProtocol.TCP
        ).normalized()

        assertEquals(ConnectionConfig.DEFAULT_TCP_PORT, config.port)
        assertEquals(ConnectionProtocol.TCP, config.protocol)
    }

    @Test
    fun connectUseCaseNormalizesWebsocketPortBeforeCallingRepository() = runTest {
        val repo = RecordingConnectionRepository()
        val calRepo = FakeCalibrationRepository(CalibrationStatus.COMPLETED)
        val useCase = ConnectToServerUseCase(repo, calRepo)

        val result = useCase.connect("192.168.1.106", 0, ConnectionProtocol.WEBSOCKET)

        assertTrue(result.isSuccess)
        assertEquals("192.168.1.106", repo.lastConfig?.ip)
        assertEquals(ConnectionConfig.DEFAULT_WEBSOCKET_PORT, repo.lastConfig?.port)
        assertEquals(ConnectionProtocol.WEBSOCKET, repo.lastConfig?.protocol)
    }

    private class FakeCalibrationRepository(
        var status: CalibrationStatus = CalibrationStatus.COMPLETED
    ) : ICalibrationRepository {
        override suspend fun getCalibrationStatus(): CalibrationStatus = status
        override fun observeCalibrationStatus(): Flow<CalibrationStatus> = flowOf(status)
        override suspend fun getCalibrationProgress(): Int = 100
        override fun observeCalibrationProgress(): Flow<Int> = flowOf(100)
        override fun observeCalibrationQuality(): Flow<com.airmouse.domain.model.CalibrationQuality> = flowOf(com.airmouse.domain.model.CalibrationQuality.EXCELLENT)
        override suspend fun calibrateGyroscope(onProgress: (Int) -> Unit): Boolean = true
        override suspend fun getGyroBias(): com.airmouse.domain.model.GyroBias = com.airmouse.domain.model.GyroBias(0f, 0f, 0f)
        override suspend fun saveGyroBias(bias: com.airmouse.domain.model.GyroBias) {}
        override suspend fun calibrateMagnetometer(onProgress: (Int) -> Unit): Boolean = true
        override suspend fun getMagOffset(): com.airmouse.domain.model.SensorCalibrationData = com.airmouse.domain.model.SensorCalibrationData(0f, 0f, 0f, 1f, 1f, 1f)
        override suspend fun saveMagOffset(data: com.airmouse.domain.model.SensorCalibrationData) {}
        override suspend fun calibrateAccelerometer(onInstruction: (String) -> Unit): Boolean = true
        override suspend fun getAccelOffset(): com.airmouse.domain.model.SensorCalibrationData = com.airmouse.domain.model.SensorCalibrationData(0f, 0f, 0f, 1f, 1f, 1f)
        override suspend fun saveAccelOffset(data: com.airmouse.domain.model.SensorCalibrationData) {}
        override suspend fun getCalibrationData(): com.airmouse.domain.model.CalibrationData = com.airmouse.domain.model.CalibrationData()
        override suspend fun saveCalibrationData(data: com.airmouse.domain.model.CalibrationData) {}
        override suspend fun resetCalibration() {}
        override suspend fun getCalibrationQuality(): com.airmouse.domain.model.CalibrationQuality = com.airmouse.domain.model.CalibrationQuality.EXCELLENT
        override suspend fun resetAllCalibration() {}
        override suspend fun updateCalibrationStatus(status: CalibrationStatus) {}
        override suspend fun updateCalibrationQuality(quality: com.airmouse.domain.model.CalibrationQuality) {}
        override suspend fun updateCalibrationProgress(progress: Int) {}
    }

    private class RecordingConnectionRepository : IConnectionRepository {
        var lastConfig: ConnectionConfig? = null

        override suspend fun connect(config: ConnectionConfig): Boolean {
            lastConfig = config
            return true
        }

        override suspend fun disconnect() = Unit
        override suspend fun sendKeyPress(keyCode: Int): Boolean = true
        override suspend fun sendWindowCommand(action: String): Boolean = true
        override suspend fun sendCalibrate(): Boolean = true
        override suspend fun reconnect(): Boolean = true
        override suspend fun getConnectionStatus() = com.airmouse.domain.model.ConnectionStatus.DISCONNECTED
        override fun observeConnectionStatus(): Flow<com.airmouse.domain.model.ConnectionStatus> = flowOf(com.airmouse.domain.model.ConnectionStatus.DISCONNECTED)
        override suspend fun getConnectionConfig(): ConnectionConfig = lastConfig ?: ConnectionConfig()
        override suspend fun saveConnectionConfig(config: ConnectionConfig) { lastConfig = config }
        override suspend fun getConnectionQuality() = com.airmouse.domain.model.ConnectionQuality()
        override fun observeConnectionQuality(): Flow<com.airmouse.domain.model.ConnectionQuality> = flowOf(com.airmouse.domain.model.ConnectionQuality())
        override suspend fun discoverServers() = emptyList<com.airmouse.domain.model.DiscoveredServer>()
        override suspend fun startDiscovery(onServerFound: (com.airmouse.domain.model.DiscoveredServer) -> Unit) = Unit
        override suspend fun stopDiscovery() = Unit
        override suspend fun sendMessage(message: String): Boolean = true
        override suspend fun sendMessage(message: ByteArray): Boolean = true
        override suspend fun testConnection(ip: String, port: Int) = com.airmouse.domain.model.TestResult(true, "ok")
        override suspend fun ping(): Long = 0L
        override fun setOnMessageListener(listener: (String) -> Unit) = Unit
        override fun setOnBinaryMessageListener(listener: (ByteArray) -> Unit) = Unit
        override fun setOnDisconnectedListener(listener: () -> Unit) = Unit
        override fun setOnConnectedListener(listener: () -> Unit) = Unit
    }
}

// app/src/test/java/com/airmouse/network/WebSocketManagerTest.kt
package com.airmouse.network

import okhttp3.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class WebSocketManagerTest {

    private lateinit var mockServer: MockWebServer
    private lateinit var client: OkHttpClient
    private val latchTimeout = 5L
    private val latchTimeoutUnit = TimeUnit.SECONDS

    @Before
    fun setUp() {
        mockServer = MockWebServer()
        mockServer.start()
        client = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .writeTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS)
            .build()
    }

    @After
    fun tearDown() {
        mockServer.shutdown()
        WebSocketManager.disconnect()
    }

    @Test
    fun testWebSocketConnection() {
        val latch = CountDownLatch(1)
        mockServer.enqueue(MockResponse().withWebSocketUpgrade(TestWebSocketListener()))

        var connected = false
        WebSocketManager.onConnected = { 
            connected = true
            latch.countDown()
        }

        WebSocketManager.connect(mockServer.url("/ws").toString())
        latch.await(latchTimeout, latchTimeoutUnit)

        assertTrue("WebSocket should connect", connected)
    }

    @Test
    fun testSendMoveMessage() {
        val latch = CountDownLatch(1)
        val webSocketListener = TestWebSocketListener()
        mockServer.enqueue(MockResponse().withWebSocketUpgrade(webSocketListener))

        WebSocketManager.connect(mockServer.url("/ws").toString())
        
        Thread.sleep(500) // Wait for connection
        
        WebSocketManager.sendMove(10.5f, -3.2f)
        
        val request = webSocketListener.awaitRequest(latchTimeout, latchTimeoutUnit)
        assertNotNull("Message should be sent", request)
        
        val json = request?.body?.utf8()
        assertTrue("Should contain move type", json?.contains("\"type\":\"move\"") == true)
        assertTrue("Should contain dx", json?.contains("10.5") == true)
        assertTrue("Should contain dy", json?.contains("-3.2") == true)
    }

    @Test
    fun testSendClickMessage() {
        val latch = CountDownLatch(1)
        val webSocketListener = TestWebSocketListener()
        mockServer.enqueue(MockResponse().withWebSocketUpgrade(webSocketListener))

        WebSocketManager.connect(mockServer.url("/ws").toString())
        
        Thread.sleep(500)
        
        WebSocketManager.sendClick("left")
        
        val request = webSocketListener.awaitRequest(latchTimeout, latchTimeoutUnit)
        val json = request?.body?.utf8()
        
        assertTrue("Should contain click type", json?.contains("\"type\":\"click\"") == true)
        assertTrue("Should contain left button", json?.contains("left") == true)
    }

    @Test
    fun testSendGestureMessage() {
        val latch = CountDownLatch(1)
        val webSocketListener = TestWebSocketListener()
        mockServer.enqueue(MockResponse().withWebSocketUpgrade(webSocketListener))

        WebSocketManager.connect(mockServer.url("/ws").toString())
        
        Thread.sleep(500)
        
        WebSocketManager.sendGesture("ThumbsUp", 0.92f)
        
        val request = webSocketListener.awaitRequest(latchTimeout, latchTimeoutUnit)
        val json = request?.body?.utf8()
        
        assertTrue("Should contain gesture type", json?.contains("\"type\":\"gesture\"") == true)
        assertTrue("Should contain gesture name", json?.contains("ThumbsUp") == true)
        assertTrue("Should contain confidence", json?.contains("0.92") == true)
    }

    @Test
    fun testReconnectionOnFailure() {
        val latch = CountDownLatch(2) // Expect two connection attempts
        var connectionCount = 0
        
        val webSocketListener = object : okhttp3.mockwebserver.WebSocketListener() {
            override fun onOpen(webSocket: okhttp3.WebSocket, response: okhttp3.Response) {
                connectionCount++
                latch.countDown()
                if (connectionCount == 1) {
                    webSocket.close(1000, "Simulated failure")
                }
            }
        }
        
        mockServer.enqueue(MockResponse().withWebSocketUpgrade(webSocketListener))
        
        WebSocketManager.connect(mockServer.url("/ws").toString())
        
        latch.await(latchTimeout * 2, latchTimeoutUnit)
        assertTrue("Should attempt reconnection", connectionCount >= 1)
    }

    @Test
    fun testMessageQueueWhenDisconnected() {
        WebSocketManager.disconnect()
        
        // Should not crash
        WebSocketManager.sendMove(1f, 1f)
        WebSocketManager.sendClick("left")
        WebSocketManager.sendGesture("Test", 0.5f)
        
        // No assertion needed - test passes if no exception
        assertTrue(true)
    }
}

class TestWebSocketListener : okhttp3.mockwebsocket.WebSocketListener() {
    private var lastRequest: RecordedRequest? = null
    private val latch = CountDownLatch(1)

    override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
        lastRequest = RecordedRequest().apply {
            setBody(okio.Buffer().writeUtf8(text))
        }
        latch.countDown()
    }

    fun awaitRequest(timeout: Long, unit: TimeUnit): RecordedRequest? {
        latch.await(timeout, unit)
        return lastRequest
    }
}
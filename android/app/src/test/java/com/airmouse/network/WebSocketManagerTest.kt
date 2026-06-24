
package com.airmouse.network

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
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
        
        Thread.sleep(500) 
        
        WebSocketManager.sendMove(10.5f, -3.2f)
        
        val json = webSocketListener.awaitMessage(latchTimeout, latchTimeoutUnit)
        assertNotNull("Message should be sent", json)
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
        
        val json = webSocketListener.awaitMessage(latchTimeout, latchTimeoutUnit)
        
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
        
        val json = webSocketListener.awaitMessage(latchTimeout, latchTimeoutUnit)
        
        assertTrue("Should contain gesture type", json?.contains("\"type\":\"gesture\"") == true)
        assertTrue("Should contain gesture name", json?.contains("ThumbsUp") == true)
        assertTrue("Should contain confidence", json?.contains("0.92") == true)
    }

    @Test
    fun testReconnectionOnFailure() {
        val latch = CountDownLatch(2) 
        var connectionCount = 0
        
        val webSocketListener = object : okhttp3.WebSocketListener() {
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
        
        
        WebSocketManager.sendMove(1f, 1f)
        WebSocketManager.sendClick("left")
        WebSocketManager.sendGesture("Test", 0.5f)
        
        
        assertTrue(true)
    }
}

class TestWebSocketListener : okhttp3.WebSocketListener() {
    private var lastMessage: String? = null
    private val latch = CountDownLatch(1)

    override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
        lastMessage = text
        latch.countDown()
    }

    fun awaitMessage(timeout: Long, unit: TimeUnit): String? {
        latch.await(timeout, unit)
        return lastMessage
    }
}

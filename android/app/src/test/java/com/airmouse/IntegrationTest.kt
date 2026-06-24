
package com.airmouse

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.airmouse.network.WebSocketManager
import com.airmouse.sensors.SensorService
import io.mockk.mockk
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class IntegrationTest {

    private lateinit var mockServer: MockWebServer
    private lateinit var context: Context
    private lateinit var sensorService: SensorService

    @Before
    fun setUp() {
        mockServer = MockWebServer()
        mockServer.start()
        context = ApplicationProvider.getApplicationContext()
        sensorService = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        mockServer.shutdown()
        WebSocketManager.disconnect()
        sensorService.stop()
    }

    @Test
    fun testWebSocketToServerIntegration() {
        val latch = CountDownLatch(1)
        var messageReceived = false
        
        mockServer.enqueue(MockResponse().withWebSocketUpgrade(object : okhttp3.WebSocketListener() {
            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                if (text.contains("hello")) {
                    messageReceived = true
                    latch.countDown()
                }
            }
        }))

        WebSocketManager.connect(mockServer.url("/ws").toString())
        WebSocketManager.onConnected = {
            WebSocketManager.sendHello("TestDevice", "3.0")
        }

        latch.await(5, TimeUnit.SECONDS)
        assertTrue("Server should receive hello message", messageReceived)
    }

    @Test
    fun testFullMessageFlow() {
        val latch = CountDownLatch(3)
        var moveReceived = false
        var clickReceived = false
        var gestureReceived = false
        
        mockServer.enqueue(MockResponse().withWebSocketUpgrade(object : okhttp3.WebSocketListener() {
            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                when {
                    text.contains("move") -> {
                        moveReceived = true
                        latch.countDown()
                    }
                    text.contains("click") -> {
                        clickReceived = true
                        latch.countDown()
                    }
                    text.contains("gesture") -> {
                        gestureReceived = true
                        latch.countDown()
                    }
                }
            }
        }))

        WebSocketManager.connect(mockServer.url("/ws").toString())
        
        Thread.sleep(500)
        
        WebSocketManager.sendMove(10f, 5f)
        WebSocketManager.sendClick("left")
        WebSocketManager.sendGesture("ThumbsUp", 0.9f)

        latch.await(5, TimeUnit.SECONDS)
        
        assertTrue("Move message should be received", moveReceived)
        assertTrue("Click message should be received", clickReceived)
        assertTrue("Gesture message should be received", gestureReceived)
    }
}

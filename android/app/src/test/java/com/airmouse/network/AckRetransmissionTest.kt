package com.airmouse.network

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class AckRetransmissionTest {

    private data class TrackedPacket(
        val id: String,
        val packet: String,
        val timestamp: Long,
        var attempts: Int = 0
    )

    @Test
    fun testRingBuffer32CapacityWraparound() {
        val capacity = 32
        val ring = Array<String?>(capacity) { null }
        var head = 0
        var count = 0

        fun push(item: String) {
            val idx = (head + count) % capacity
            ring[idx] = item
            if (count < capacity) {
                count++
            } else {
                head = (head + 1) % capacity
            }
        }

        // Push 40 items
        for (i in 1..40) {
            push("move_$i")
        }

        // Buffer size should stay capped at 32
        assertEquals(32, count)

        // Oldest item currently in buffer should be move_9
        assertEquals("move_9", ring[head])

        // Last inserted item should be move_40
        val lastIdx = (head + count - 1) % capacity
        assertEquals("move_40", ring[lastIdx])
    }

    @Test
    fun testAckRetransmission500msTimeout() {
        val pendingAcks = ConcurrentHashMap<String, TrackedPacket>()
        val timeoutMs = 500L
        val maxAttempts = 5
        val retransmitCount = AtomicInteger(0)

        val id = "click_uuid_123"
        val packet = """{"v":2,"type":"click","seq":1,"ts":1000,"id":"$id","payload":{"button":"left"}}"""
        pendingAcks[id] = TrackedPacket(id, packet, timestamp = 1000L, attempts = 0)

        // Simulate tick at 1200ms (< 500ms elapsed)
        val t1 = 1200L
        pendingAcks.values.forEach { msg ->
            if (t1 - msg.timestamp >= timeoutMs && msg.attempts < maxAttempts) {
                msg.attempts++
                retransmitCount.incrementAndGet()
            }
        }
        assertEquals(0, retransmitCount.get())

        // Simulate tick at 1550ms (> 500ms elapsed) -> retransmit trigger
        val t2 = 1550L
        pendingAcks.values.forEach { msg ->
            if (t2 - msg.timestamp >= timeoutMs && msg.attempts < maxAttempts) {
                msg.attempts++
                retransmitCount.incrementAndGet()
            }
        }
        assertEquals(1, retransmitCount.get())

        // Simulate ACK arrival removing packet
        pendingAcks.remove(id)
        assertTrue(pendingAcks.isEmpty())

        // Subsequent tick should do nothing
        val t3 = 2100L
        pendingAcks.values.forEach { msg ->
            if (t3 - msg.timestamp >= timeoutMs && msg.attempts < maxAttempts) {
                msg.attempts++
                retransmitCount.incrementAndGet()
            }
        }
        assertEquals(1, retransmitCount.get())
    }

    @Test
    fun testDataSenderProtocolV2Envelope() {
        val moveEnvelope = DataSender.formatMovePacket(5.0f, -2.5f, seq = 42L)
        assertTrue(moveEnvelope.contains("\"v\":2"))
        assertTrue(moveEnvelope.contains("\"type\":\"move\""))
        assertTrue(moveEnvelope.contains("\"seq\":42"))
        assertTrue(moveEnvelope.contains("\"dx\":5"))
        assertTrue(moveEnvelope.contains("\"dy\":-2.5"))

        val clickEnvelope = DataSender.formatClickPacket("uuid_click_1", "right", seq = 43L)
        assertTrue(clickEnvelope.contains("\"v\":2"))
        assertTrue(clickEnvelope.contains("\"type\":\"click\""))
        assertTrue(clickEnvelope.contains("\"id\":\"uuid_click_1\""))
        assertTrue(clickEnvelope.contains("\"button\":\"right\""))

        val scrollEnvelope = DataSender.formatScrollPacket("uuid_scroll_1", -3.0, seq = 44L)
        assertTrue(scrollEnvelope.contains("\"v\":2"))
        assertTrue(scrollEnvelope.contains("\"type\":\"scroll\""))
        assertTrue(scrollEnvelope.contains("\"id\":\"uuid_scroll_1\""))
        assertTrue(scrollEnvelope.contains("\"delta\":-3"))
    }
}

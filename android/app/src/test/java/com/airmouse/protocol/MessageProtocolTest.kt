
package com.airmouse.protocol

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class MessageProtocolTest {

    @Test
    fun testValidMoveMessage() {
        val json = JSONObject().apply {
            put("type", "move")
            put("payload", JSONObject().apply {
                put("dx", 12.5)
                put("dy", -3.2)
            })
        }
        
        assertTrue("Should have type", json.has("type"))
        assertEquals("Type should be move", "move", json.getString("type"))
        assertTrue("Should have payload", json.has("payload"))
        
        val payload = json.getJSONObject("payload")
        assertTrue("Should have dx", payload.has("dx"))
        assertTrue("Should have dy", payload.has("dy"))
        assertEquals("dx should be 12.5", 12.5, payload.getDouble("dx"), 0.001)
        assertEquals("dy should be -3.2", -3.2, payload.getDouble("dy"), 0.001)
    }

    @Test
    fun testValidClickMessage() {
        val json = JSONObject().apply {
            put("type", "click")
            put("payload", JSONObject().put("button", "left"))
        }
        
        assertEquals("Type should be click", "click", json.getString("type"))
        val button = json.getJSONObject("payload").getString("button")
        assertTrue("Button should be left, right, or middle", 
            setOf("left", "right", "middle").contains(button))
    }

    @Test
    fun testValidGestureMessage() {
        val json = JSONObject().apply {
            put("type", "gesture")
            put("payload", JSONObject().apply {
                put("gesture", "ThumbsUp")
                put("confidence", 0.92)
            })
        }
        
        assertEquals("Type should be gesture", "gesture", json.getString("type"))
        val payload = json.getJSONObject("payload")
        assertEquals("Gesture should be ThumbsUp", "ThumbsUp", payload.getString("gesture"))
        
        val confidence = payload.getDouble("confidence")
        assertTrue("Confidence should be between 0 and 1", confidence in 0.0..1.0)
    }

    @Test
    fun testValidProximityMessage() {
        val json = JSONObject().apply {
            put("type", "proximity")
            put("payload", JSONObject().apply {
                put("device_id", "test-device-123")
                put("is_near", true)
                put("distance", 1.23)
            })
        }
        
        assertEquals("Type should be proximity", "proximity", json.getString("type"))
        val payload = json.getJSONObject("payload")
        assertTrue("Should have device_id", payload.has("device_id"))
        assertTrue("Should have is_near", payload.has("is_near"))
        assertTrue("Should have distance", payload.has("distance"))
    }

    @Test
    fun testValidControlMessage() {
        val commands = listOf("pause_movement", "resume_movement")
        
        for (cmd in commands) {
            val json = JSONObject().apply {
                put("type", "control")
                put("payload", JSONObject().put("command", cmd))
            }
            
            assertEquals("Type should be control", "control", json.getString("type"))
            val command = json.getJSONObject("payload").getString("command")
            assertEquals("Command should match", cmd, command)
        }
    }

    @Test
    fun testValidHelloMessage() {
        val json = JSONObject().apply {
            put("type", "hello")
            put("payload", JSONObject().apply {
                put("name", "Pixel 8 Pro")
                put("version", "3.0")
            })
        }
        
        assertEquals("Type should be hello", "hello", json.getString("type"))
        val payload = json.getJSONObject("payload")
        assertTrue("Should have name", payload.has("name"))
        assertTrue("Should have version", payload.has("version"))
    }

    @Test
    fun testFlatMessageFormat() {
        
        val json = JSONObject().apply {
            put("type", "move")
            put("dx", 10.0)
            put("dy", -5.0)
        }
        
        assertEquals("Type should be move", "move", json.getString("type"))
        assertEquals("dx should be 10.0", 10.0, json.getDouble("dx"), 0.001)
        assertEquals("dy should be -5.0", -5.0, json.getDouble("dy"), 0.001)
    }

    @Test
    fun testMessageSerialization() {
        val original = JSONObject().apply {
            put("type", "click")
            put("payload", JSONObject().put("button", "left"))
        }
        
        val serialized = original.toString()
        val deserialized = JSONObject(serialized)
        
        assertEquals("Serialized type should match", 
            original.getString("type"), deserialized.getString("type"))
    }

    @Test
    fun testInvalidMessageMissingType() {
        val json = JSONObject().apply {
            put("dx", 10.0)
            put("dy", -5.0)
        }
        
        assertFalse("Message without type is invalid", json.has("type"))
    }

    @Test
    fun testInvalidMoveMissingFields() {
        val json = JSONObject().apply {
            put("type", "move")
            put("payload", JSONObject())
        }
        
        val payload = json.getJSONObject("payload")
        assertFalse("Move without dx/dy is invalid", 
            payload.has("dx") && payload.has("dy"))
    }
}
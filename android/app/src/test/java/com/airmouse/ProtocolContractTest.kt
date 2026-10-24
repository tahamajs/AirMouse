package com.airmouse

import com.airmouse.network.AirMouseProtocolMessages
import com.airmouse.network.ConnectionManager
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolContractTest {

    @Test
    fun androidOutboundMessagesMatchGoWireContract() {
        assertMove(AirMouseProtocolMessages.move(12.5f, -3.25f))
        assertReliableClick(
            AirMouseProtocolMessages.reliableClick(
                type = ConnectionManager.MessageTypes.TYPE_CLICK,
                id = "msg_7",
                button = ConnectionManager.MessageTypes.BUTTON_LEFT
            )
        )
        assertReliableScroll(AirMouseProtocolMessages.reliableScroll(id = "msg_8", delta = -4))
        assertGesture(AirMouseProtocolMessages.gesture("ThumbsUp", 0.91f))
        assertProximity(AirMouseProtocolMessages.proximity("android-device", true, 1.75f))
        assertControl(AirMouseProtocolMessages.control(ConnectionManager.MessageTypes.COMMAND_STOP))
        assertHello(
            AirMouseProtocolMessages.hello(
                name = "Pixel Test",
                version = "3.0",
                device = "Google Pixel Test",
                androidVersion = "14",
                protocol = "WEBSOCKET",
                transport = "websocket",
                authToken = "secret-token"
            )
        )
    }

    @Test
    fun androidParsesGoServerReplies() {
        val welcome = JSONObject("""{"type":"welcome","payload":{"server":"Air Mouse Go","version":"3.0"}}""")
        assertEquals("welcome", welcome.getString("type"))
        assertEquals("Air Mouse Go", welcome.getJSONObject("payload").getString("server"))
        assertEquals("3.0", welcome.getJSONObject("payload").getString("version"))

        val ack = JSONObject("""{"type":"ack","id":"msg_7"}""")
        assertEquals("ack", ack.getString("type"))
        assertEquals("msg_7", ack.getString("id"))

        val pong = JSONObject("""{"type":"pong"}""")
        assertEquals("pong", pong.getString("type"))

        val status = JSONObject("""{"type":"status","running":true,"clients":2}""")
        assertEquals("status", status.getString("type"))
        assertTrue(status.getBoolean("running"))
        assertEquals(2, status.getInt("clients"))
    }

    private fun assertMove(raw: String) {
        val json = JSONObject(raw)
        assertEquals(2, json.getInt("v"))
        assertEquals("move", json.getString("type"))
        assertTrue(json.has("seq"))
        assertTrue(json.has("ts"))
        val payload = json.getJSONObject("payload")
        assertEquals(12.5, payload.getDouble("dx"), 0.0001)
        assertEquals(-3.25, payload.getDouble("dy"), 0.0001)
    }

    private fun assertReliableClick(raw: String) {
        val json = JSONObject(raw)
        assertEquals(2, json.getInt("v"))
        assertEquals("click", json.getString("type"))
        assertEquals("msg_7", json.getString("id"))
        val payload = json.getJSONObject("payload")
        assertEquals("left", payload.getString("button"))
    }

    private fun assertReliableScroll(raw: String) {
        val json = JSONObject(raw)
        assertEquals(2, json.getInt("v"))
        assertEquals("scroll", json.getString("type"))
        assertEquals("msg_8", json.getString("id"))
        val payload = json.getJSONObject("payload")
        assertEquals(-4.0, payload.getDouble("delta"), 0.0001)
    }

    private fun assertGesture(raw: String) {
        val json = JSONObject(raw)
        assertEquals("gesture", json.getString("type"))
        val payload = json.getJSONObject("payload")
        assertEquals("ThumbsUp", payload.getString("gesture"))
        assertEquals(0.91, payload.getDouble("confidence"), 0.0001)
    }

    private fun assertProximity(raw: String) {
        val json = JSONObject(raw)
        assertEquals("proximity", json.getString("type"))
        val payload = json.getJSONObject("payload")
        assertEquals("android-device", payload.getString("device_id"))
        assertTrue(payload.getBoolean("is_near"))
        assertEquals(1.75, payload.getDouble("distance"), 0.0001)
    }

    private fun assertControl(raw: String) {
        val json = JSONObject(raw)
        assertEquals("control", json.getString("type"))
        assertEquals("stop", json.getJSONObject("payload").getString("command"))
    }

    private fun assertHello(raw: String) {
        val json = JSONObject(raw)
        assertEquals("hello", json.getString("type"))
        val payload = json.getJSONObject("payload")
        assertEquals("Pixel Test", payload.getString("name"))
        assertEquals("3.0", payload.getString("version"))
        assertEquals("Google Pixel Test", payload.getString("device"))
        assertEquals("14", payload.getString("android_version"))
        assertEquals("WEBSOCKET", payload.getString("protocol"))
        assertEquals("websocket", payload.getString("transport"))
        assertEquals("secret-token", payload.getString("token"))
    }
}

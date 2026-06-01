package com.airmouse.utils

import org.junit.Assert.*
import org.junit.Test

class ValidationUtilsTest {

    @Test
    fun isValidIp_validIps_returnsTrue() {
        assertTrue(ValidationUtils.isValidIp("192.168.1.1"))
        assertTrue(ValidationUtils.isValidIp("10.0.0.1"))
        assertTrue(ValidationUtils.isValidIp("255.255.255.255"))
    }

    @Test
    fun isValidIp_invalidIps_returnsFalse() {
        assertFalse(ValidationUtils.isValidIp("999.999.999.999"))
        assertFalse(ValidationUtils.isValidIp("192.168.1"))
        assertFalse(ValidationUtils.isValidIp("abc.def.ghi.jkl"))
    }

    @Test
    fun parseEndpoint_validAirMouseUrl_returnsEndpoint() {
        val result = ValidationUtils.parseEndpoint("airmouse://192.168.1.100:8080")
        assertEquals("192.168.1.100", result?.host)
        assertEquals(8080, result?.port)
    }

    @Test
    fun parseEndpoint_ipOnly_returnsDefaultPort() {
        val result = ValidationUtils.parseEndpoint("192.168.1.100")
        assertEquals("192.168.1.100", result?.host)
        assertEquals(8080, result?.port)
    }

    @Test
    fun extractIpAddress_validIp_returnsIp() {
        assertEquals("192.168.1.100", ValidationUtils.extractIpAddress("192.168.1.100"))
        assertEquals("192.168.1.100", ValidationUtils.extractIpAddress("http://192.168.1.100:8080/ws"))
    }
}
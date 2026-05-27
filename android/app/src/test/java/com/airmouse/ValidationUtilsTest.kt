package com.airmouse

import com.airmouse.utils.ValidationUtils
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationUtilsTest {

    @Test
    fun `valid IPv4 addresses return true`() {
        assertTrue(ValidationUtils.isValidIp("192.168.1.1"))
        assertTrue(ValidationUtils.isValidIp("10.0.0.1"))
        assertTrue(ValidationUtils.isValidIp("127.0.0.1"))
        assertTrue(ValidationUtils.isValidIp("255.255.255.255"))
    }

    @Test
    fun `invalid IPv4 addresses return false`() {
        assertFalse(ValidationUtils.isValidIp("192.168.1"))
        assertFalse(ValidationUtils.isValidIp("256.256.256.256"))
        assertFalse(ValidationUtils.isValidIp("abc.def.ghi.jkl"))
        assertFalse(ValidationUtils.isValidIp("1.2.3.4.5"))
        assertFalse(ValidationUtils.isValidIp(""))
    }
}
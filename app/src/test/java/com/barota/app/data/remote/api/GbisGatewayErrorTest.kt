package com.barota.app.data.remote.api

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GbisGatewayErrorTest {
    @Test fun `403 with authorization code guides to service application`() {
        val body = "<response><returnAuthMsg>SERVICE_ACCESS_DENIED_ERROR</returnAuthMsg></response>"
        val message = GbisGatewayError.message(403, body)
        assertTrue(message.contains("SERVICE_ACCESS_DENIED_ERROR"))
        assertTrue(message.contains("활용신청"))
    }

    @Test fun `unregistered key produces dedicated explanation`() {
        val message = GbisGatewayError.message(403, "SERVICE_KEY_IS_NOT_REGISTERED_ERROR")
        assertTrue(message.contains("등록되지 않은"))
    }

    @Test fun `unknown response does not expose sensitive response contents`() {
        val message = GbisGatewayError.message(403, "requestUrl?serviceKey=SECRET-KEY")
        assertTrue(message.contains("GBIS HTTP 403"))
        assertFalse(message.contains("SECRET-KEY"))
    }
}

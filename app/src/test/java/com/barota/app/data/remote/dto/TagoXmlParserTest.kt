package com.barota.app.data.remote.dto

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TagoXmlParserTest {
    private val parser = TagoXmlParser()

    @Test fun `reads TAGO routes with city-specific ID`() {
        val xml = """<response><header><resultCode>00</resultCode><resultMsg>OK</resultMsg></header>
          <body><items><item><routeid>GGB0009</routeid><routeno>9</routeno></item></items>
          <numOfRows>100</numOfRows><totalCount>1</totalCount><pageNo>1</pageNo></body></response>"""
        val data = parser.parse(xml)
        assertEquals(1, data.totalCount)
        assertEquals("GGB0009", data.rows.single()["routeid"])
        assertEquals("9", data.rows.single()["routeno"])
    }

    @Test fun `reads station order and official location fields`() {
        val xml = """<response><header><resultCode>00</resultCode></header><body><items>
          <item><nodeid>AAA</nodeid><nodeord>5</nodeord><nodenm>구리역</nodenm>
            <gpslati>37.6</gpslati><gpslong>127.1</gpslong></item>
          <item><nodeid>AAA</nodeid><nodeord>60</nodeord><nodenm>구리역</nodenm></item>
          </items><totalCount>2</totalCount></body></response>"""
        val rows = parser.parse(xml).rows
        assertEquals(listOf("5", "60"), rows.map { it["nodeord"] })
        assertEquals("127.1", rows.first()["gpslong"])
    }

    @Test fun `accepts successful empty results`() {
        val xml = """<response><header><resultCode>00</resultCode></header><body>
          <items/><totalCount>0</totalCount></body></response>"""
        assertTrue(parser.parse(xml).rows.isEmpty())
    }

    @Test fun `surfaces authorization errors without exposing credentials`() {
        val xml = """<OpenAPI_ServiceResponse><cmmMsgHeader>
          <returnAuthMsg>SERVICE_ACCESS_DENIED_ERROR</returnAuthMsg>
          </cmmMsgHeader></OpenAPI_ServiceResponse>"""
        assertThrows(IOException::class.java) { parser.parse(xml) }
    }

    @Test fun `rejects external entities`() {
        val xml = """<!DOCTYPE xml [<!ENTITY x SYSTEM "file:///etc/passwd">]>
          <response><header><resultCode>00</resultCode></header></response>"""
        assertThrows(IOException::class.java) { parser.parse(xml) }
    }
}

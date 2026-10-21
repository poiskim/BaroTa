package com.barota.app.data.remote.dto

import com.barota.app.data.remote.api.GbisApiException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GbisXmlParserTest {
    private val parser = GbisXmlParser()

    @Test fun `parses official route id and route list`() {
        val xml = """<response><msgHeader><resultCode>0</resultCode></msgHeader><msgBody>
          <busRouteList><routeId>200000085</routeId><routeName>9</routeName>
            <regionName>구리시</regionName><startStationName>A</startStationName><endStationName>B</endStationName>
          </busRouteList></msgBody></response>"""
        val routes = parser.parseRoutes(xml)
        assertEquals(1, routes.size)
        assertEquals("200000085", routes.first().routeId)
        assertEquals("9", routes.first().routeName)
    }

    @Test fun `parses same station in multiple sequence points`() {
        val xml = """<response><msgHeader><resultCode>0</resultCode></msgHeader><msgBody>
          <busRouteStationList><stationId>100</stationId><stationSeq>3</stationSeq>
            <stationName>구리역</stationName><mobileNo>12345</mobileNo><x>127.1</x><y>37.6</y>
          </busRouteStationList>
          <busRouteStationList><stationId>100</stationId><stationSeq>28</stationSeq>
            <stationName>구리역</stationName><mobileNo>12345</mobileNo><x>127.1</x><y>37.6</y>
          </busRouteStationList></msgBody></response>"""
        val stops = parser.parseStops(xml)
        assertEquals(2, stops.size)
        assertEquals(listOf(3, 28), stops.map { it.stationSeq })
        assertEquals(127.1, stops.first().longitude!!, 0.00001)
    }

    @Test fun `empty official results use status four`() {
        assertTrue(parser.parseRoutes("<response><msgHeader><resultCode>4</resultCode></msgHeader></response>").isEmpty())
    }

    @Test fun `invalid service key does not become empty results`() {
        val xml = """<response><msgHeader><resultCode>1</resultCode>
            <resultMessage>Not authorized</resultMessage></msgHeader></response>"""
        assertThrows(GbisApiException::class.java) { parser.parseRoutes(xml) }
    }

    @Test fun `rejects doctype xml external references`() {
        val xml = """<!DOCTYPE foo [ <!ENTITY xxe SYSTEM "file:///etc/passwd"> ]>
            <response><msgHeader><resultCode>0</resultCode></msgHeader></response>"""
        assertThrows(GbisApiException::class.java) { parser.parseRoutes(xml) }
    }
}

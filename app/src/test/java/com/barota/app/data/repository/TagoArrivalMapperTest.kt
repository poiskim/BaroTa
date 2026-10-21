package com.barota.app.data.repository

import com.barota.app.data.remote.dto.TagoXmlParser
import com.barota.app.domain.model.ArrivalStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class TagoArrivalMapperTest {
    private val mapper = TagoArrivalMapper()
    private fun record(route: String, node: String, seconds: String) =
        mapOf("routeid" to route, "nodeid" to node, "arrtime" to seconds)

    @Test fun `matches official route and stop not just bus number`() {
        val rows = listOf(record("anotherRoute", "stopA", "10"),
            record("route9", "wrongStop", "20"), record("route9", "stopA", "75"),
            record("route9", "stopA", "600"))
        assertEquals(ArrivalStatus.Available(2, 10), mapper.map(rows, "route9", "stopA"))
    }

    @Test fun `filters invalid seconds and sorts arriving buses`() {
        val rows = listOf(record("r", "s", "-1"), record("r", "s", "oops"),
            record("r", "s", "360"), record("r", "s", "0"), record("r", "s", "1"))
        assertEquals(ArrivalStatus.Available(0, 1), mapper.map(rows, "r", "s"))
    }

    @Test fun `empty or unrelated predictions explicitly show no data`() {
        assertSame(ArrivalStatus.NoPrediction, mapper.map(emptyList(), "r", "s"))
        assertSame(ArrivalStatus.NoPrediction, mapper.map(listOf(record("other", "s", "90")), "r", "s"))
    }

    @Test fun `official XML fields parse to correctly rounded arrival time`() {
        val xml = """<response><header><resultCode>00</resultCode></header><body>
           <items><item><nodeid>AAA</nodeid><routeid>BBB</routeid>
           <arrtime>61</arrtime><arrprevstationcnt>2</arrprevstationcnt></item></items>
           <totalCount>1</totalCount></body></response>"""
        val rows = TagoXmlParser().parse(xml).rows
        assertEquals(ArrivalStatus.Available(2, null), mapper.map(rows, "BBB", "AAA"))
    }
}

package com.barota.app.data.remote.dto

import com.barota.app.data.remote.api.GbisApiException
import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.xml.sax.InputSource

/** 응답 모델: 공식 API의 raw XML에만 의존. domain 객체로의 매핑은 repository에서 수행한다. */
data class GbisRouteDto(
    val routeId: String, val routeName: String, val regionName: String,
    val startStationName: String, val endStationName: String
)

data class GbisStopDto(
    val stationId: String, val stationSeq: Int, val stationName: String,
    val mobileNo: String?, val regionName: String,
    val longitude: Double?, val latitude: Double?, val isTurningPoint: Boolean
)

/** XML의 row가 1개 혹은 여러 개일 때 모두 처리. 인증 실패/빈 목록을 구별한다. */
class GbisXmlParser {
    fun parseRoutes(xml: String): List<GbisRouteDto> = parse(xml, "busRouteList") { e ->
        val id = e.field("routeId")
        val name = e.field("routeName")
        if (id.isBlank() || name.isBlank()) null else GbisRouteDto(
            routeId = id, routeName = name, regionName = e.field("regionName"),
            startStationName = e.field("startStationName"), endStationName = e.field("endStationName")
        )
    }

    fun parseStops(xml: String): List<GbisStopDto> = parse(xml, "busRouteStationList") { e ->
        val id = e.field("stationId")
        val seq = e.field("stationSeq").toIntOrNull()
        if (id.isBlank() || seq == null || seq <= 0) null else GbisStopDto(
            stationId = id, stationSeq = seq, stationName = e.field("stationName"),
            mobileNo = e.field("mobileNo").ifBlank { null },
            regionName = e.field("regionName"),
            longitude = e.field("x").toDoubleOrNull(), latitude = e.field("y").toDoubleOrNull(),
            isTurningPoint = e.field("turnYn").equals("Y", ignoreCase = true)
        )
    }

    private fun <T : Any> parse(xml: String, rowTag: String, mapper: (Element) -> T?): List<T> {
        try {
            // Android XML 구현별로 지원 feature가 다를 수 있어, DOCTYPE을 먼저 명시적으로 거부한다.
            if (xml.contains("<!DOCTYPE", ignoreCase = true)) {
                throw GbisApiException("안전하지 않은 XML(DOCTYPE)은 처리하지 않아요.")
            }
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = false
                runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
                runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
                runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
                runCatching { setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) }
                runCatching { isXIncludeAware = false }
                runCatching { isExpandEntityReferences = false }
            }
            val doc = factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))
            val headers = doc.getElementsByTagName("resultCode")
            if (headers.length == 0) {
                throw GbisApiException("GBIS 응답을 확인할 수 없어요. API 키와 서버 응답을 확인해 주세요.")
            }
            val status = headers.item(0).textContent.trim().toIntOrNull()
            if (status == 4) return emptyList()
            if (status != 0) {
                val msg = doc.getElementsByTagName("resultMessage").item(0)?.textContent?.trim()
                throw GbisApiException("GBIS 요청 오류 ($status): ${msg.orEmpty()}")
            }
            val nodes = doc.getElementsByTagName(rowTag)
            return (0 until nodes.length).mapNotNull { idx ->
                val element = nodes.item(idx) as? Element ?: return@mapNotNull null
                mapper(element)
            }
        } catch (e: GbisApiException) {
            throw e
        } catch (e: Exception) {
            throw GbisApiException("GBIS 응답 해석에 실패했어요: ${e.message ?: "XML 오류"}", e)
        }
    }

    private fun Element.field(tag: String): String =
        getElementsByTagName(tag).item(0)?.textContent?.trim().orEmpty()
}

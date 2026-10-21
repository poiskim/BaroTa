package com.barota.app.data.remote.dto

import java.io.IOException
import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.xml.sax.InputSource

/** TAGO의 <response><header/><body><items><item>.. 형태를 안전하게 해석한다. */
class TagoXmlParser {
    data class Page(val rows: List<Map<String, String>>, val totalCount: Int)

    fun parse(xml: String): Page {
        if (xml.contains("<!DOCTYPE", ignoreCase = true)) throw IOException("DOCTYPE XML은 지원하지 않아요.")
        try {
            val factory = DocumentBuilderFactory.newInstance().apply {
                runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
                runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
                runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
                runCatching { setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) }
                runCatching { isXIncludeAware = false }
                runCatching { isExpandEntityReferences = false }
            }
            val doc = factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))
            val auth = doc.getElementsByTagName("returnAuthMsg").item(0)?.textContent?.trim()
            if (!auth.isNullOrBlank()) {
                // API 응답에 키/URL이 포함되어도 통째로 노출하지 않는다.
                throw IOException("TAGO 인증 실패 (" + when {
                    auth.contains("등록되지 않은") -> "등록되지 않은 키"
                    auth.contains("SERVICE_ACCESS_DENIED") -> "서비스 접근 권한 없음"
                    else -> "공공데이터포털에서 인증키·활용승인을 확인해 주세요"
                } + ")")
            }
            val result = doc.getElementsByTagName("resultCode").item(0)?.textContent?.trim()
                ?: throw IOException("TAGO 응답 형식을 인식할 수 없어요.")
            if (result != "00" && result != "0") {
                if (result == "03") return Page(emptyList(), 0)
                throw IOException("TAGO 요청 실패 (resultCode=" + result + "). 인증키·서비스 승인 상태를 확인해 주세요.")
            }
            val total = doc.getElementsByTagName("totalCount").item(0)?.textContent?.trim()?.toIntOrNull() ?: 0
            val nodes = doc.getElementsByTagName("item")
            val rows = (0 until nodes.length).mapNotNull { index ->
                val e = nodes.item(index) as? Element ?: return@mapNotNull null
                val fields = linkedMapOf<String, String>()
                val children = e.childNodes
                for (i in 0 until children.length) {
                    val child = children.item(i)
                    if (child is Element) fields[child.tagName.lowercase()] = child.textContent.trim()
                }
                fields
            }
            return Page(rows, total)
        } catch (ex: IOException) {
            throw ex
        } catch (ex: Exception) {
            throw IOException("TAGO XML 응답 해석 실패", ex)
        }
    }
}

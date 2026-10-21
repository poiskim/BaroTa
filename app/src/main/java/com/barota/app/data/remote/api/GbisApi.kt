package com.barota.app.data.remote.api

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** GBIS v2 공식 REST 요청. 네트워크 요청/타임아웃 책임만 갖는다. */
class GbisApi(private val serviceKey: String) {
    suspend fun findRoutes(keyword: String): String = fetch(
        "getBusRouteListv2", "keyword", keyword
    )

    suspend fun getStops(routeId: String): String = fetch(
        "getBusRouteStationListv2", "routeId", routeId
    )

    private suspend fun fetch(operation: String, param: String, value: String): String =
        withContext(Dispatchers.IO) {
            if (serviceKey.isBlank()) {
                throw GbisApiException("GBIS 서비스키가 없어요. local.properties의 GBIS_SERVICE_KEY를 설정해 주세요.")
            }
            // 공공데이터 포털은 디코딩 키와 URL 인코딩 키를 모두 제공한다.
            // 이미 인코딩된 키는 한 번 디코딩한 뒤 쿼리 값으로 정확히 한 번 인코딩한다.
            val normalizedKey = if ('%' in serviceKey) {
                URLDecoder.decode(serviceKey.trim(), "UTF-8")
            } else serviceKey.trim()
            fun encode(raw: String) = URLEncoder.encode(raw, "UTF-8")
            val url = URL(
                "https://apis.data.go.kr/6410000/busrouteservice/v2/$operation" +
                    "?format=xml&serviceKey=${encode(normalizedKey)}&$param=${encode(value)}"
            )
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 10_000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/xml, text/xml")
            }
            try {
                val code = connection.responseCode
                if (code !in 200..299) {
                    val errorBody = connection.errorStream
                        ?.bufferedReader(Charsets.UTF_8)
                        ?.use { it.readText().take(4_096) }
                    throw GbisApiException(GbisGatewayError.message(code, errorBody))
                }
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } catch (ex: GbisApiException) {
                throw ex
            } catch (ex: IOException) {
                throw GbisApiException("네트워크 오류: ${ex.localizedMessage ?: "연결에 실패했어요."}", ex)
            } finally {
                connection.disconnect()
            }
        }
}

class GbisApiException(message: String, cause: Throwable? = null) : IOException(message, cause)

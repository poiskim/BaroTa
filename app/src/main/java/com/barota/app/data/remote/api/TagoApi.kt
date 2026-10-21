package com.barota.app.data.remote.api

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** TAGO HTTP 전송만 책임진다. URI·서비스키를 로그에 남기지 않는다. */
class TagoApi(private val serviceKey: String) {
    suspend fun getCities(page: Int, size: Int) = fetch(
        "BusRouteInfoInqireService/getCtyCodeList",
        mapOf("pageNo" to page.toString(), "numOfRows" to size.toString())
    )

    suspend fun searchRoutes(cityCode: String, number: String, page: Int, size: Int) = fetch(
        "BusRouteInfoInqireService/getRouteNoList",
        mapOf("cityCode" to cityCode, "routeNo" to number, "pageNo" to page.toString(),
            "numOfRows" to size.toString())
    )

    suspend fun getRouteStops(cityCode: String, routeId: String, page: Int, size: Int) = fetch(
        "BusRouteInfoInqireService/getRouteAcctoThrghSttnList",
        mapOf("cityCode" to cityCode, "routeId" to routeId,
            "pageNo" to page.toString(), "numOfRows" to size.toString())
    )

    suspend fun getNearbyStops(latitude: Double, longitude: Double, page: Int, size: Int) = fetch(
        "BusSttnInfoInqireService/getCrdntPrxmtSttnList",
        mapOf("gpsLati" to latitude.toString(), "gpsLong" to longitude.toString(),
            "pageNo" to page.toString(), "numOfRows" to size.toString())
    )

    private suspend fun fetch(path: String, params: Map<String, String>): String =
        withContext(Dispatchers.IO) {
            if (serviceKey.isBlank()) {
                throw IOException("TAGO 키가 없어요. local.properties에 TAGO_SERVICE_KEY를 설정하고 다시 빌드해 주세요.")
            }
            // 공공데이터포털의 Encoding/Decoding 키 모두 받아 쿼리에서는 한 번만 인코딩.
            val key = if ('%' in serviceKey) URLDecoder.decode(serviceKey.trim(), "UTF-8")
                      else serviceKey.trim()
            fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
            val query = (params + mapOf("serviceKey" to key, "_type" to "xml"))
                .entries.joinToString("&") { enc(it.key) + "=" + enc(it.value) }
            val connection = (URL("https://apis.data.go.kr/1613000/" + path + "?" + query)
                .openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 12_000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/xml, text/xml")
            }
            try {
                val status = connection.responseCode
                if (status !in 200..299) {
                    throw IOException("TAGO 서버 HTTP " + status +
                        " 오류. 인증키·해당 TAGO 서비스 활용승인·요청 제한을 확인해 주세요.")
                }
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } finally {
                connection.disconnect()
            }
        }
}

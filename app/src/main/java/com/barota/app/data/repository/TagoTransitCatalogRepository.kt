package com.barota.app.data.repository

import com.barota.app.data.remote.api.TagoApi
import com.barota.app.data.remote.dto.TagoXmlParser
import com.barota.app.domain.model.BusRoute
import com.barota.app.domain.model.GeoPoint
import com.barota.app.domain.model.NearbyTransitStop
import com.barota.app.domain.model.RouteStop
import com.barota.app.domain.model.TransitCity
import com.barota.app.domain.model.TransitProvider
import com.barota.app.domain.repository.TagoTransitCatalog
import java.io.IOException

/** 공식 TAGO 응답을 앱 공통 도메인 모델로 변환. GPS와 정류장 경유 순번을 보존한다. */
class TagoTransitCatalogRepository(
    private val api: TagoApi,
    private val parser: TagoXmlParser = TagoXmlParser()
) : TagoTransitCatalog {
    private var cityNames: Map<String, String> = emptyMap()

    override suspend fun getCities(): List<TransitCity> {
        val result = fetchPages(100, 15) { page, size -> api.getCities(page, size) }
            .mapNotNull { row ->
                val code = row["citycode"].orEmpty()
                if (code.isBlank()) null else TransitCity(code, row["cityname"].orEmpty())
            }.distinctBy { it.code }.sortedBy { it.name }
        cityNames = result.associate { it.code to it.name }
        return result
    }

    override suspend fun searchBusRoutes(query: String, cityCode: String?): List<BusRoute> {
        val city = requireNotNull(cityCode?.takeIf { it.isNotBlank() }) {
            "TAGO는 도시코드를 먼저 선택해야 해요."
        }
        val number = query.trim()
        require(number.isNotEmpty()) { "버스번호를 입력해 주세요." }
        return fetchPages(100, 15) { page, size -> api.searchRoutes(city, number, page, size) }
            .mapNotNull { row ->
                val routeId = row["routeid"].orEmpty()
                val routeNo = row["routeno"].orEmpty()
                if (routeId.isBlank() || routeNo.isBlank()) null else BusRoute(
                    id = routeId, number = routeNo,
                    regionName = cityNames[city] ?: city,
                    startStationName = row["startnodenm"].orEmpty(),
                    endStationName = row["endnodenm"].orEmpty(),
                    provider = TransitProvider.TAGO,
                    cityCode = city
                )
            }.distinctBy { it.cityCode to it.id }
            .sortedWith(compareByDescending<BusRoute> { it.number == number }.thenBy { it.number })
    }

    override suspend fun getRouteStops(routeId: String, cityCode: String?): List<RouteStop> {
        val city = requireNotNull(cityCode?.takeIf { it.isNotBlank() }) {
            "TAGO 경유 정류장 조회에 도시코드가 필요해요."
        }
        require(routeId.isNotBlank())
        return fetchPages(100, 20) { page, size -> api.getRouteStops(city, routeId, page, size) }
            .mapNotNull { row ->
                val stationId = row["nodeid"].orEmpty()
                val seq = row["nodeord"]?.toIntOrNull()
                if (stationId.isBlank() || seq == null || seq <= 0) null else RouteStop(
                    stationId = stationId,
                    stationSeq = seq,
                    name = row["nodenm"].orEmpty().ifBlank { "이름 없는 정류장" },
                    mobileNo = row["nodeno"]?.ifBlank { null },
                    regionName = cityNames[city] ?: city,
                    point = point(row["gpslati"], row["gpslong"])
                )
            }.distinctBy { it.stationId to it.stationSeq }.sortedBy { it.stationSeq }
    }

    override suspend fun getNearbyStops(location: GeoPoint): List<NearbyTransitStop> =
        fetchPages(100, 10) { page, size ->
            api.getNearbyStops(location.latitude, location.longitude, page, size)
        }.mapNotNull { row ->
            val id = row["nodeid"].orEmpty()
            val city = row["citycode"].orEmpty()
            if (id.isBlank() || city.isBlank()) null else NearbyTransitStop(
                stationId = id,
                name = row["nodenm"].orEmpty(),
                cityCode = city,
                point = point(row["gpslati"], row["gpslong"])
            )
        }.distinctBy { it.cityCode to it.stationId }

    private suspend fun fetchPages(
        size: Int,
        maxPages: Int,
        request: suspend (page: Int, size: Int) -> String
    ): List<Map<String, String>> {
        val result = mutableListOf<Map<String, String>>()
        for (page in 1..maxPages) {
            val batch = parser.parse(request(page, size))
            result.addAll(batch.rows)
            if (batch.rows.isEmpty() || page * size >= batch.totalCount) return result
        }
        throw IOException("검색 결과가 너무 많아요. 도시 또는 노선번호를 더 좁혀서 시도해 주세요.")
    }

    private fun point(latitude: String?, longitude: String?): GeoPoint? {
        val lat = latitude?.toDoubleOrNull() ?: return null
        val lng = longitude?.toDoubleOrNull() ?: return null
        return if (lat in 33.0..40.0 && lng in 124.0..132.0) GeoPoint(lat, lng) else null
    }
}

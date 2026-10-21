package com.barota.app.data.repository

import com.barota.app.data.remote.api.GbisApi
import com.barota.app.data.remote.dto.GbisXmlParser
import com.barota.app.domain.model.BusRoute
import com.barota.app.domain.model.GeoPoint
import com.barota.app.domain.model.RouteStop
import com.barota.app.domain.repository.TransitCatalogRepository

/** 네트워크 DTO를 domain 모델로 변환. 다른 버스 공급자도 동일 interface로 추가 가능. */
class GbisTransitCatalogRepository(
    private val api: GbisApi,
    private val parser: GbisXmlParser = GbisXmlParser()
) : TransitCatalogRepository {
    override suspend fun searchBusRoutes(query: String, cityCode: String?): List<BusRoute> {
        require(query.isNotBlank()) { "버스 번호를 입력해 주세요." }
        return parser.parseRoutes(api.findRoutes(query.trim())).map {
            BusRoute(it.routeId, it.routeName, it.regionName, it.startStationName, it.endStationName)
        }.distinctBy { it.id }.sortedWith(
            compareByDescending<BusRoute> { it.number == query.trim() }
                .thenByDescending { it.regionName.contains("구리") || it.startStationName.contains("구리") || it.endStationName.contains("구리") }
                .thenBy { it.number }
        )
    }

    override suspend fun getRouteStops(routeId: String, cityCode: String?): List<RouteStop> {
        require(routeId.isNotBlank())
        return parser.parseStops(api.getStops(routeId)).map {
            val point = if (it.latitude != null && it.longitude != null &&
                it.latitude in 33.0..40.0 && it.longitude in 124.0..132.0
            ) GeoPoint(it.latitude, it.longitude) else null
            RouteStop(it.stationId, it.stationSeq, it.stationName,
                it.mobileNo, it.regionName, point, it.isTurningPoint)
        }.sortedBy { it.stationSeq }
    }
}

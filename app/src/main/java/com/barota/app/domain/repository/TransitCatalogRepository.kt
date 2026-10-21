package com.barota.app.domain.repository

import com.barota.app.domain.model.BusRoute
import com.barota.app.domain.model.RouteStop

/** 공급자(GBIS 등)를 교체해도 UI/UseCase가 변하지 않도록 하는 계약. */
interface TransitCatalogRepository {
    suspend fun searchBusRoutes(query: String, cityCode: String? = null): List<BusRoute>
    suspend fun getRouteStops(routeId: String, cityCode: String? = null): List<RouteStop>
}

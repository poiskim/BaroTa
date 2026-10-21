package com.barota.app.domain.repository

import com.barota.app.domain.model.GeoPoint
import com.barota.app.domain.model.NearbyTransitStop
import com.barota.app.domain.model.TransitCity

/** TAGO 확장 기능. 기본 TransitCatalogRepository와 별개로 지역 탐색에 사용한다. */
interface TagoTransitCatalog : TransitCatalogRepository {
    suspend fun getCities(): List<TransitCity>
    suspend fun getNearbyStops(location: GeoPoint): List<NearbyTransitStop>
}

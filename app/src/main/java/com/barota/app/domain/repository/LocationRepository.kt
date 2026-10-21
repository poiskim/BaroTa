package com.barota.app.domain.repository

import com.barota.app.domain.model.GeoPoint

interface LocationRepository {
    /** 권한이 없거나 위치 확인에 실패하면 null 반환. */
    suspend fun currentLocation(): GeoPoint?
}

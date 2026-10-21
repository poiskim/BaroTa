package com.barota.app.domain.model

/** '노선 + 정류장 + 방향'을 하나의 즐겨찾기로 관리한다. */
enum class TransitMode(val label: String) {
    BUS("버스"), SUBWAY("지하철")
}

data class GeoPoint(val latitude: Double, val longitude: Double)

data class TransitFavorite(
    val id: String,
    val mode: TransitMode,
    val route: String,
    val stop: String,
    val direction: String,
    val anchor: GeoPoint?,
    val viewCount: Int = 0,
    val lastViewedAt: Long = 0L,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    // null이면 v0.1에서 입력한 미검증 즐겨찾기 (하위 호환)
    val officialRouteId: String? = null,
    val officialStationId: String? = null,
    val officialStationSeq: Int? = null,
    val provider: TransitProvider = TransitProvider.GBIS,
    val officialCityCode: String? = null
) {
    val isOfficiallyVerified: Boolean
        get() = mode == TransitMode.BUS && officialRouteId != null &&
            officialStationId != null && officialStationSeq != null
}

data class RecommendedFavorite(
    val favorite: TransitFavorite,
    val distanceMeters: Int?,
    val reason: String
)

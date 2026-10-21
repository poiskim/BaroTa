package com.barota.app.data.repository

import com.barota.app.domain.model.ArrivalStatus

/** TAGO arrtime은 초. 임의 추정 없이 정확히 같은 노선 ID의 유효값만 사용. */
class TagoArrivalMapper {
    fun map(
        rows: List<Map<String, String>>,
        routeId: String,
        stationId: String
    ): ArrivalStatus {
        val seconds = rows.asSequence()
            .filter { it["routeid"] == routeId }
            .filter { it["nodeid"].isNullOrBlank() || it["nodeid"] == stationId }
            .mapNotNull { it["arrtime"]?.toIntOrNull() }
            .filter { it >= 0 }
            .sorted()
            .take(2)
            .toList()
        if (seconds.isEmpty()) return ArrivalStatus.NoPrediction
        // 1~60초는 1분, 61~120초는 2분으로 표시. 0초는 곧 도착.
        fun minutes(value: Int) = (value + 59) / 60
        return ArrivalStatus.Available(minutes(seconds[0]), seconds.getOrNull(1)?.let(::minutes))
    }
}

package com.barota.app.domain.model

/** 특정 노선의 경유 정류장. 같은 stationId가 노선에 여러 번 나타나도 stationSeq로 구분. */
data class RouteStop(
    val stationId: String,
    val stationSeq: Int,
    val name: String,
    val mobileNo: String?,
    val regionName: String,
    val point: GeoPoint?,
    val isTurningPoint: Boolean = false
)

/** 선택 정류장 이후의 다음 정류장을 사용해 방향을 표현한다. 종점 등은 별도 처리. */
fun directionForStop(stops: List<RouteStop>, selected: RouteStop): String {
    val next = stops.filter { it.stationSeq > selected.stationSeq }
        .minByOrNull { it.stationSeq }
    return if (next == null) "종점 방향 (마지막 정류장)" else "${next.name} 방면"
}

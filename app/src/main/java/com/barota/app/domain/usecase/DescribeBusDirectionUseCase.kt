package com.barota.app.domain.usecase

import com.barota.app.domain.model.BusRoute
import com.barota.app.domain.model.RouteStop

/**
 * 기점/종점과 경유 순서를 바탕으로 방향을 설명한다.
 * 같은 정류장명이 왕복으로 반복되어도 순번을 유지한다.
 * 경유 데이터에서 방향을 확정할 수 없으면 추정해서 표시하지 않는다.
 */
class DescribeBusDirectionUseCase {
    operator fun invoke(route: BusRoute, stops: List<RouteStop>, selected: RouteStop): String {
        val ordered = stops.sortedBy { it.stationSeq }
        val next = ordered.firstOrNull { it.stationSeq > selected.stationSeq }
        val start = route.startStationName.trim()
        val end = route.endStationName.trim()

        // 종점이 경유 목록에 나타나는 첫 지점을 회차 기준으로 사용한다.
        val endStop = ordered.firstOrNull { end.isNotBlank() && it.name.trim() == end }
        val hasReturn = endStop != null && start.isNotBlank() &&
            ordered.any { it.stationSeq > endStop.stationSeq && it.name.trim() == start }
        val headline = when {
            next == null -> "마지막 정류장 · 운행 종료"
            endStop == null || start.isBlank() || end.isBlank() || start == end ->
                "방향 확인 필요"
            selected.stationSeq < endStop.stationSeq -> end + " 방면"
            selected.stationSeq == endStop.stationSeq && hasReturn -> "회차 지점 · " + start + " 방면"
            selected.stationSeq == endStop.stationSeq -> "종점 · " + end
            hasReturn -> start + " 방면"
            else -> "방향 확인 필요"
        }
        return if (next == null) headline else headline + " · 다음 " + next.name
    }
}

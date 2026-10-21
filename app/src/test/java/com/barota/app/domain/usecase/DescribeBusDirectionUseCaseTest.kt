package com.barota.app.domain.usecase

import com.barota.app.domain.model.BusRoute
import com.barota.app.domain.model.RouteStop
import org.junit.Assert.assertTrue
import org.junit.Test

class DescribeBusDirectionUseCaseTest {
    private val useCase = DescribeBusDirectionUseCase()
    private val route = BusRoute("r1", "9", "구리", "차고지", "강변역")
    private val stops = listOf(
        RouteStop("a", 1, "차고지", null, "", null),
        RouteStop("b", 2, "구리역", null, "", null),
        RouteStop("c", 3, "강변역", null, "", null),
        RouteStop("d", 4, "구리역", null, "", null),
        RouteStop("e", 5, "차고지", null, "", null)
    )

    @Test fun `outbound before turnaround heads toward destination`() {
        val direction = useCase(route, stops, stops[1])
        assertTrue(direction.contains("강변역 방면"))
        assertTrue(direction.contains("다음 강변역"))
    }

    @Test fun `return at same named stop heads toward depot`() {
        val direction = useCase(route, stops, stops[3])
        assertTrue(direction.contains("차고지 방면"))
        assertTrue(direction.contains("다음 차고지"))
    }

    @Test fun `turnaround label is not confused with outbound`() {
        val direction = useCase(route, stops, stops[2])
        assertTrue(direction.contains("회차 지점"))
        assertTrue(direction.contains("차고지 방면"))
    }

    @Test fun `insufficient route data never invents a direction`() {
        val unknown = route.copy(endStationName = "경유 목록에 없는 종점")
        val direction = useCase(unknown, stops, stops[1])
        assertTrue(direction.contains("방향 확인 필요"))
        assertTrue(direction.contains("다음 강변역"))
    }

    @Test fun `last station marks completion`() {
        assertTrue(useCase(route, stops, stops.last()).contains("운행 종료"))
    }
}

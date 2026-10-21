package com.barota.app.domain.usecase

import com.barota.app.domain.model.RouteStop
import com.barota.app.domain.repository.TransitCatalogRepository

/** 화면에서 선택한 정류장이 저장 시점에도 노선에 실제 포함되는지 다시 검증한다. */
class ValidateRouteStopUseCase(private val catalog: TransitCatalogRepository) {
    suspend operator fun invoke(routeId: String, stationId: String, stationSeq: Int): RouteStop {
        require(routeId.isNotBlank() && stationId.isNotBlank() && stationSeq > 0) {
            "올바르지 않은 노선/정류장 식별자입니다."
        }
        // 네트워크 오류는 그대로 전파한다. '정류장 없음'과 '통신 실패'를 섞지 않는다.
        val verifiedStop = catalog.getRouteStops(routeId).firstOrNull {
            it.stationId == stationId && it.stationSeq == stationSeq
        }
        return verifiedStop ?: throw StopNotOnRouteException()
    }
}

class StopNotOnRouteException : IllegalStateException(
    "이 노선에서 선택한 정류장을 찾지 못했어요. 노선을 다시 검색해 주세요."
)

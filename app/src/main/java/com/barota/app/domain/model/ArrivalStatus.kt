package com.barota.app.domain.model

/** 실시간 데이터 연결 전에는 절대로 가짜 도착시간을 만들지 않는다. */
sealed interface ArrivalStatus {
    data object NotConnected : ArrivalStatus
    /** 요청은 성공했지만 해당 노선의 실시간 도착예정 차량이 없음. */
    data object NoPrediction : ArrivalStatus
    data object Loading : ArrivalStatus
    data class Available(val minutes: Int, val nextMinutes: Int?) : ArrivalStatus
    data class Failure(val message: String) : ArrivalStatus
}

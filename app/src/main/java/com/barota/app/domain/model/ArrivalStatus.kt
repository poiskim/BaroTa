package com.barota.app.domain.model

/** 실시간 데이터 연결 전에는 절대로 가짜 도착시간을 만들지 않는다. */
sealed interface ArrivalStatus {
    data object NotConnected : ArrivalStatus
    data class Available(val minutes: Int, val nextMinutes: Int?) : ArrivalStatus
    data class Failure(val message: String) : ArrivalStatus
}

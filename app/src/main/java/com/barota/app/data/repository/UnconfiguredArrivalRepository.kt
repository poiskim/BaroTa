package com.barota.app.data.repository

import com.barota.app.domain.model.ArrivalStatus
import com.barota.app.domain.model.TransitFavorite
import com.barota.app.domain.repository.ArrivalRepository

/** 실제 GBIS/지하철 API 연동 시 이 구현체만 교체하면 화면 코드를 그대로 쓸 수 있다. */
class UnconfiguredArrivalRepository : ArrivalRepository {
    override suspend fun getArrival(item: TransitFavorite): ArrivalStatus = ArrivalStatus.NotConnected
}

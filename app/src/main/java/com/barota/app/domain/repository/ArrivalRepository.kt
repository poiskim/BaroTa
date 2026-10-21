package com.barota.app.domain.repository

import com.barota.app.domain.model.ArrivalStatus
import com.barota.app.domain.model.TransitFavorite

interface ArrivalRepository {
    suspend fun getArrival(item: TransitFavorite): ArrivalStatus

    /** 제공처에 따라 같은 정류장의 요청을 합칠 수 있도록 일괄 조회도 제공한다. */
    suspend fun getArrivals(items: List<TransitFavorite>): Map<String, ArrivalStatus> =
        items.associate { it.id to getArrival(it) }
}

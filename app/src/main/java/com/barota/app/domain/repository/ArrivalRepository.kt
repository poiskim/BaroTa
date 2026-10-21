package com.barota.app.domain.repository

import com.barota.app.domain.model.ArrivalStatus
import com.barota.app.domain.model.TransitFavorite

interface ArrivalRepository {
    suspend fun getArrival(item: TransitFavorite): ArrivalStatus
}

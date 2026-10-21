package com.barota.app.domain.repository

import com.barota.app.domain.model.TransitFavorite
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    val favorites: Flow<List<TransitFavorite>>
    suspend fun save(item: TransitFavorite)
    suspend fun remove(id: String)
    suspend fun markViewed(id: String)
    suspend fun togglePinned(id: String)
}

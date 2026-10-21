package com.barota.app.data.repository

import com.barota.app.data.local.FavoritesLocalDataSource
import com.barota.app.domain.model.TransitFavorite
import com.barota.app.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow

class DefaultFavoritesRepository(
    private val local: FavoritesLocalDataSource
) : FavoritesRepository {
    override val favorites: Flow<List<TransitFavorite>> = local.all

    override suspend fun save(item: TransitFavorite) {
        local.update { items ->
            val sameStop = items.firstOrNull { existing ->
                item.isOfficiallyVerified && existing.isOfficiallyVerified &&
                    existing.officialRouteId == item.officialRouteId &&
                    existing.officialStationId == item.officialStationId &&
                    existing.officialStationSeq == item.officialStationSeq
            }
            val saved = if (sameStop == null) item else item.copy(
                id = sameStop.id,
                viewCount = sameStop.viewCount,
                lastViewedAt = sameStop.lastViewedAt,
                isPinned = sameStop.isPinned,
                createdAt = sameStop.createdAt
            )
            items.filterNot { it.id == saved.id } + saved
        }
    }

    override suspend fun remove(id: String) {
        local.update { items -> items.filterNot { it.id == id } }
    }

    override suspend fun markViewed(id: String) {
        local.update { items ->
            items.map { item ->
                if (item.id == id) item.copy(
                    viewCount = item.viewCount + 1,
                    lastViewedAt = System.currentTimeMillis()
                ) else item
            }
        }
    }

    override suspend fun togglePinned(id: String) {
        local.update { items ->
            items.map { item -> if (item.id == id) item.copy(isPinned = !item.isPinned) else item }
        }
    }
}

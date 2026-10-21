package com.barota.app.domain.usecase

import com.barota.app.domain.model.GeoPoint
import com.barota.app.domain.model.TransitFavorite
import com.barota.app.domain.model.TransitMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RankFavoritesUseCaseTest {
    private val useCase = RankFavoritesUseCase()
    private val center = GeoPoint(37.6, 127.1)

    private fun favorite(id: String, location: GeoPoint?, views: Int = 0): TransitFavorite =
        TransitFavorite(id, TransitMode.BUS, "9", "예시 정류장", "예시 방면", location,
            viewCount = views, createdAt = 1L)

    @Test fun `near stop ranks higher when usage equal`() {
        val far = favorite("far", GeoPoint(37.7, 127.2))
        val near = favorite("near", GeoPoint(37.6001, 127.1001))
        val result = useCase.rank(listOf(far, near), center)
        assertEquals("near", result.first().favorite.id)
    }

    @Test fun `frequently opened route rises when location unknown`() {
        val often = favorite("often", null, views = 8)
        val rare = favorite("rare", null)
        val result = useCase.rank(listOf(rare, often), null)
        assertEquals("often", result.first().favorite.id)
        assertNull(result.first().distanceMeters)
    }

    @Test fun `pinned route gets preference`() {
        val pinned = favorite("pinned", null).copy(isPinned = true)
        val ordinary = favorite("ordinary", null, views = 2)
        assertEquals("pinned", useCase.rank(listOf(ordinary, pinned), null).first().favorite.id)
    }
}

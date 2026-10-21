package com.barota.app.domain.usecase

import com.barota.app.domain.model.GeoPoint
import com.barota.app.domain.model.NearbyTransitStop
import com.barota.app.domain.model.TransitProvider
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

    @Test fun `exact matching TAGO city and stop is prioritized`() {
        val matching = favorite("matching", null).copy(
            officialRouteId = "route9", officialStationId = "nodeA",
            officialStationSeq = 2, officialCityCode = "31120", provider = TransitProvider.TAGO
        )
        val frequent = favorite("frequent", null, views = 20)
        val nearby = listOf(NearbyTransitStop("nodeA", "집앞", "31120", center))
        val ranked = useCase.rank(listOf(frequent, matching), center, nearbyStops = nearby)
        assertEquals("matching", ranked.first().favorite.id)
    }

    @Test fun `same station ID under different city is not matched`() {
        val distant = favorite("differentCity", null).copy(
            officialRouteId = "route9", officialStationId = "nodeA",
            officialStationSeq = 2, officialCityCode = "31130", provider = TransitProvider.TAGO
        )
        val frequent = favorite("frequent", null, views = 10)
        val nearby = listOf(NearbyTransitStop("nodeA", "집앞", "31120", center))
        val ranked = useCase.rank(listOf(frequent, distant), center, nearbyStops = nearby)
        assertEquals("frequent", ranked.first().favorite.id)
    }
}

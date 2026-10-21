package com.barota.app.domain.usecase

import com.barota.app.domain.model.GeoPoint
import com.barota.app.domain.model.RecommendedFavorite
import com.barota.app.domain.model.TransitFavorite
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Android 의존성이 없는 순수 비즈니스 로직: 단위 테스트 가능. */
class RankFavoritesUseCase {
    fun rank(
        favorites: List<TransitFavorite>,
        here: GeoPoint?,
        now: Long = System.currentTimeMillis()
    ): List<RecommendedFavorite> {
        return favorites.map { item ->
            val distance = if (here != null && item.anchor != null) {
                distanceMeters(here, item.anchor)
            } else null
            val proximity = when {
                distance == null -> 0.0
                distance <= 300 -> 80.0
                distance <= 800 -> 65.0
                distance <= 2000 -> 35.0
                distance <= 5000 -> 10.0
                else -> 0.0
            }
            val usage = min(item.viewCount, 20) * 2.0
            val freshness = if (item.lastViewedAt > 0 && now >= item.lastViewedAt) {
                val days = (now - item.lastViewedAt) / 86_400_000.0
                (12.0 - days).coerceAtLeast(0.0)
            } else 0.0
            val score = proximity + usage + freshness + if (item.isPinned) 50.0 else 0.0
            val reason = when {
                distance != null && distance <= 800 -> "지금 위치와 가까워요"
                item.isPinned -> "상단 고정"
                item.viewCount >= 3 -> "자주 확인한 경로"
                distance != null -> "저장한 장소에서 ${formatDistance(distance)}"
                else -> "내가 추가한 경로"
            }
            Scored(
                result = RecommendedFavorite(item, distance?.roundToInt(), reason),
                score = score
            )
        }.sortedWith(compareByDescending<Scored> { it.score }
            .thenByDescending { it.result.favorite.lastViewedAt }
            .thenBy { it.result.favorite.createdAt })
            .map { it.result }
    }

    private fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
        val lat = Math.toRadians(b.latitude - a.latitude)
        val lng = Math.toRadians(b.longitude - a.longitude)
        val x = sin(lat / 2).pow(2) + cos(Math.toRadians(a.latitude)) *
            cos(Math.toRadians(b.latitude)) * sin(lng / 2).pow(2)
        return 2 * 6_371_000 * asin(sqrt(x.coerceIn(0.0, 1.0)))
    }

    private fun formatDistance(meters: Double): String =
        if (meters < 1000) "${meters.roundToInt()}m" else "${"%.1f".format(meters / 1000)}km"

    private data class Scored(val result: RecommendedFavorite, val score: Double)
}

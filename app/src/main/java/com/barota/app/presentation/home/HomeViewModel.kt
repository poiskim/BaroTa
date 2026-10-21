package com.barota.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.barota.app.di.AppContainer
import com.barota.app.domain.model.ArrivalStatus
import com.barota.app.domain.model.GeoPoint
import com.barota.app.domain.model.NearbyTransitStop
import com.barota.app.domain.model.RecommendedFavorite
import com.barota.app.domain.model.TransitFavorite
import com.barota.app.domain.model.TransitMode
import com.barota.app.domain.repository.ArrivalRepository
import com.barota.app.domain.repository.FavoritesRepository
import com.barota.app.domain.repository.LocationRepository
import com.barota.app.domain.repository.TagoTransitCatalog
import com.barota.app.domain.usecase.RankFavoritesUseCase
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

/** 도착시간은 수동 갱신·화면 진입 시 조회한다. 실시간 스트림인 것처럼 표시하지 않는다. */
data class HomeUiState(
    val items: List<RecommendedFavorite> = emptyList(),
    val location: GeoPoint? = null,
    val locationMessage: String = "위치 미확인",
    val arrivals: Map<String, ArrivalStatus> = emptyMap(),
    val arrivalRefreshing: Boolean = false,
    val arrivalsCheckedAt: Long? = null
)

data class DetailUiState(val item: TransitFavorite, val arrival: ArrivalStatus)

private data class ArrivalSnapshot(
    val statuses: Map<String, ArrivalStatus> = emptyMap(),
    val loading: Boolean = false,
    val checkedAt: Long? = null
)

class HomeViewModel(
    private val favoritesRepository: FavoritesRepository,
    private val locationRepository: LocationRepository,
    private val arrivalRepository: ArrivalRepository,
    private val tagoCatalog: TagoTransitCatalog,
    private val rankUseCase: RankFavoritesUseCase
) : ViewModel() {
    private val location = MutableStateFlow<GeoPoint?>(null)
    private val locationMessage = MutableStateFlow("위치를 허용하면 가까운 경로부터 추천해요")
    private val nearbyStops = MutableStateFlow<List<NearbyTransitStop>>(emptyList())
    private val arrivalSnapshot = MutableStateFlow(ArrivalSnapshot())
    private val _detail = MutableStateFlow<DetailUiState?>(null)
    val detail: StateFlow<DetailUiState?> = _detail.asStateFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        favoritesRepository.favorites, location, locationMessage, nearbyStops, arrivalSnapshot
    ) { favorites, here, message, nearby, arrivals ->
        val ids = favorites.mapTo(mutableSetOf()) { it.id }
        HomeUiState(
            items = rankUseCase.rank(favorites, here, nearbyStops = nearby),
            location = here,
            locationMessage = message,
            arrivals = arrivals.statuses.filterKeys { it in ids },
            arrivalRefreshing = arrivals.loading,
            arrivalsCheckedAt = arrivals.checkedAt
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun refreshLocation() {
        viewModelScope.launch {
            locationMessage.value = "현재 위치 확인 중…"
            nearbyStops.value = emptyList()
            val point = try { locationRepository.currentLocation() }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { null }
            location.value = point
            if (point == null) {
                locationMessage.value = "위치를 가져오지 못했어요. GPS를 켜고 다시 시도해 주세요."
                return@launch
            }
            locationMessage.value = "저장한 경로를 GPS 거리로 정렬했어요. 주변 정류장 확인 중…"
            try {
                nearbyStops.value = tagoCatalog.getNearbyStops(point)
                locationMessage.value = if (nearbyStops.value.isEmpty())
                    "GPS 거리순 추천 중 · 주변 TAGO 정류장은 찾지 못했어요."
                else "GPS 거리·주변 TAGO 정류장 일치를 추천에 반영했어요."
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                // TAGO 조회 실패가 GPS 기반 즐겨찾기 정렬을 막지는 않는다.
                locationMessage.value = "GPS 거리순 추천 중 · 주변 TAGO 정류장 조회 실패"
            }
        }
    }

    /** 같은 정류장에 저장한 여러 노선은 Repository 내부에서 요청을 공유한다. */
    fun refreshArrivals() {
        if (arrivalSnapshot.value.loading) return
        arrivalSnapshot.value = arrivalSnapshot.value.copy(loading = true)
        viewModelScope.launch {
            try {
                // 과도한 대량 요청과 호출 한도 소진을 피한다. 우선순위 상위 15개만 갱신.
                val favorites = favoritesRepository.favorites.first()
                val chosen = rankUseCase.rank(favorites, location.value,
                    nearbyStops = nearbyStops.value).take(15).map { it.favorite }
                val statuses = arrivalRepository.getArrivals(chosen)
                arrivalSnapshot.value = ArrivalSnapshot(
                    statuses = statuses, loading = false, checkedAt = System.currentTimeMillis())
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                arrivalSnapshot.value = arrivalSnapshot.value.copy(loading = false)
            }
        }
    }

    fun addFavorite(mode: TransitMode, route: String, stop: String, direction: String, bindHere: Boolean) {
        val cleanedRoute = route.trim()
        val cleanedStop = stop.trim()
        val cleanedDirection = direction.trim()
        if (cleanedRoute.isEmpty() || cleanedStop.isEmpty() || cleanedDirection.isEmpty()) return
        viewModelScope.launch {
            favoritesRepository.save(
                TransitFavorite(
                    id = UUID.randomUUID().toString(),
                    mode = mode,
                    route = cleanedRoute,
                    stop = cleanedStop,
                    direction = cleanedDirection,
                    anchor = if (bindHere) location.value else null
                )
            )
        }
    }

    fun togglePinned(id: String) = viewModelScope.launch { favoritesRepository.togglePinned(id) }
    fun remove(id: String) = viewModelScope.launch { favoritesRepository.remove(id) }

    fun openDetail(item: TransitFavorite) {
        _detail.value = DetailUiState(item, ArrivalStatus.Loading)
        viewModelScope.launch {
            favoritesRepository.markViewed(item.id)
            val result = try {
                arrivalRepository.getArrival(item)
            } catch (e: CancellationException) { throw e }
              catch (_: Exception) { ArrivalStatus.Failure("도착정보 조회에 실패했어요.") }
            if (_detail.value?.item?.id == item.id) {
                _detail.value = DetailUiState(item, result)
            }
            arrivalSnapshot.value = arrivalSnapshot.value.copy(
                statuses = arrivalSnapshot.value.statuses + (item.id to result),
                checkedAt = System.currentTimeMillis()
            )
        }
    }
    fun closeDetail() { _detail.value = null }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(HomeViewModel::class.java))
                    return HomeViewModel(
                        container.favoritesRepository,
                        container.locationRepository,
                        container.arrivalRepository,
                        container.tagoCatalog,
                        container.rankFavorites
                    ) as T
                }
            }
    }
}

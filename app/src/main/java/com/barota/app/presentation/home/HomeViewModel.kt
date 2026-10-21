package com.barota.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.barota.app.di.AppContainer
import com.barota.app.domain.model.ArrivalStatus
import com.barota.app.domain.model.GeoPoint
import com.barota.app.domain.model.RecommendedFavorite
import com.barota.app.domain.model.TransitFavorite
import com.barota.app.domain.model.TransitMode
import com.barota.app.domain.repository.ArrivalRepository
import com.barota.app.domain.repository.FavoritesRepository
import com.barota.app.domain.repository.LocationRepository
import com.barota.app.domain.usecase.RankFavoritesUseCase
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

/** UI에 필요한 읽기 전용 상태. Compose에 DataStore/LocationManager를 직접 노출하지 않는다. */
data class HomeUiState(
    val items: List<RecommendedFavorite> = emptyList(),
    val location: GeoPoint? = null,
    val locationMessage: String = "위치 미확인"
)

data class DetailUiState(val item: TransitFavorite, val arrival: ArrivalStatus)

class HomeViewModel(
    private val favoritesRepository: FavoritesRepository,
    private val locationRepository: LocationRepository,
    private val arrivalRepository: ArrivalRepository,
    private val rankUseCase: RankFavoritesUseCase
) : ViewModel() {
    private val location = MutableStateFlow<GeoPoint?>(null)
    private val locationMessage = MutableStateFlow("위치를 허용하면 가까운 경로부터 추천해요")
    private val _detail = MutableStateFlow<DetailUiState?>(null)
    val detail: StateFlow<DetailUiState?> = _detail.asStateFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        favoritesRepository.favorites, location, locationMessage
    ) { favorites, here, message ->
        HomeUiState(rankUseCase.rank(favorites, here), here, message)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun refreshLocation() {
        viewModelScope.launch {
            locationMessage.value = "현재 위치 확인 중…"
            val point = locationRepository.currentLocation()
            location.value = point
            locationMessage.value = if (point == null) {
                "위치를 가져오지 못했어요. GPS를 켜고 다시 시도해 주세요."
            } else {
                "현재 위치로 추천 순서를 정렬했어요"
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
        viewModelScope.launch {
            // 상세를 열 때 실제 사용 횟수를 기록한다.
            favoritesRepository.markViewed(item.id)
            _detail.value = DetailUiState(item, arrivalRepository.getArrival(item))
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
                        container.rankFavorites
                    ) as T
                }
            }
    }
}

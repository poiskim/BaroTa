package com.barota.app.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.barota.app.di.AppContainer
import com.barota.app.domain.model.BusRoute
import com.barota.app.domain.model.GeoPoint
import com.barota.app.domain.model.RouteStop
import com.barota.app.domain.model.TransitFavorite
import com.barota.app.domain.model.TransitMode
import com.barota.app.domain.model.directionForStop
import com.barota.app.domain.repository.FavoritesRepository
import com.barota.app.domain.repository.TransitCatalogRepository
import com.barota.app.domain.usecase.ValidateRouteStopUseCase
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val routes: List<BusRoute> = emptyList(),
    val selectedRoute: BusRoute? = null,
    val stops: List<RouteStop> = emptyList(),
    val stopFilter: String = "",
    val selectedStop: RouteStop? = null,
    val searching: Boolean = false,
    val loadingStops: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val notice: String? = null
)

class SearchViewModel(
    private val catalog: TransitCatalogRepository,
    private val favorites: FavoritesRepository,
    private val validateStop: ValidateRouteStopUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    private var fetchJob: Job? = null

    fun updateQuery(query: String) {
        if (query == _uiState.value.query) return
        fetchJob?.cancel()
        _uiState.value = SearchUiState(query = query)
    }
    fun updateStopFilter(value: String) { _uiState.update { it.copy(stopFilter = value) } }

    fun reset() {
        fetchJob?.cancel()
        _uiState.value = SearchUiState()
    }

    fun searchRoutes(number: String = _uiState.value.query) {
        fetchJob?.cancel()
        val query = number.trim()
        if (query.isEmpty()) {
            _uiState.update { it.copy(error = "버스 번호를 입력해 주세요.") }
            return
        }
        _uiState.value = SearchUiState(query = query, searching = true)
        fetchJob = viewModelScope.launch {
            try {
                val routes = catalog.searchBusRoutes(query)
                _uiState.update { it.copy(routes = routes, searching = false,
                    notice = if (routes.isEmpty()) "검색된 노선이 없어요." else null) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _uiState.update { it.copy(searching = false, error = friendlyMessage(e)) }
            }
        }
    }

    fun selectRoute(route: BusRoute) {
        fetchJob?.cancel()
        _uiState.update { it.copy(selectedRoute = route, stops = emptyList(),
            selectedStop = null, stopFilter = "", loadingStops = true,
            error = null, notice = null) }
        fetchJob = viewModelScope.launch {
            try {
                val stops = catalog.getRouteStops(route.id)
                _uiState.update { it.copy(stops = stops, loadingStops = false,
                    notice = if (stops.isEmpty()) "이 노선의 경유 정류장을 찾지 못했어요." else null) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _uiState.update { it.copy(loadingStops = false, error = friendlyMessage(e)) }
            }
        }
    }

    fun selectStop(stop: RouteStop) {
        _uiState.update { it.copy(selectedStop = stop, error = null, notice = null) }
    }

    fun saveVerifiedBusStop() {
        val snapshot = _uiState.value
        val route = snapshot.selectedRoute ?: return
        val selected = snapshot.selectedStop ?: return
        if (snapshot.saving) return
        _uiState.update { it.copy(saving = true, error = null, notice = null) }
        viewModelScope.launch {
            try {
                val verified = validateStop(route.id, selected.stationId, selected.stationSeq)
                val direction = directionForStop(snapshot.stops, verified)
                favorites.save(TransitFavorite(
                    id = UUID.randomUUID().toString(), mode = TransitMode.BUS,
                    route = route.number, stop = verified.name, direction = direction,
                    anchor = verified.point, officialRouteId = route.id,
                    officialStationId = verified.stationId,
                    officialStationSeq = verified.stationSeq
                ))
                _uiState.update { it.copy(saving = false, selectedStop = null,
                    notice = "공식 정류장 '${verified.name}'을(를) 즐겨찾기에 저장했어요.") }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _uiState.update { it.copy(saving = false, error = friendlyMessage(e)) }
            }
        }
    }

    fun saveSubwayManual(line: String, stop: String, direction: String, here: GeoPoint?) {
        if (line.isBlank() || stop.isBlank() || direction.isBlank()) {
            _uiState.update { it.copy(error = "노선·역·방향을 모두 입력해 주세요.") }; return
        }
        viewModelScope.launch {
            try {
                favorites.save(TransitFavorite(UUID.randomUUID().toString(), TransitMode.SUBWAY,
                    line.trim(), stop.trim(), direction.trim(), here))
                _uiState.update { it.copy(notice = "지하철 경로를 저장했어요. 현재는 공식 검증을 지원하지 않아요.", error = null) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _uiState.update { it.copy(error = friendlyMessage(e)) } }
        }
    }

    private fun friendlyMessage(error: Throwable): String =
        error.message?.takeIf { it.isNotBlank() } ?: "요청 처리에 실패했어요. 다시 시도해 주세요."

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(SearchViewModel::class.java))
                    return SearchViewModel(container.transitCatalog, container.favoritesRepository,
                        container.validateRouteStop) as T
                }
            }
    }
}

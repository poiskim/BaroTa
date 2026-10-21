package com.barota.app.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.barota.app.di.AppContainer
import com.barota.app.domain.model.BusRoute
import com.barota.app.domain.model.GeoPoint
import com.barota.app.domain.model.NearbyTransitStop
import com.barota.app.domain.model.RouteStop
import com.barota.app.domain.model.TransitCity
import com.barota.app.domain.model.TransitFavorite
import com.barota.app.domain.model.TransitMode
import com.barota.app.domain.model.TransitProvider
import com.barota.app.domain.usecase.DescribeBusDirectionUseCase
import com.barota.app.domain.repository.FavoritesRepository
import com.barota.app.domain.repository.TagoTransitCatalog
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
    val provider: TransitProvider = TransitProvider.TAGO,
    val cityCode: String = "31120", // TAGO 확인된 구리 코드. 수정/주변 도시 선택 가능
    val cities: List<TransitCity> = emptyList(),
    val nearbyStops: List<NearbyTransitStop> = emptyList(),
    val loadingCities: Boolean = false,
    val loadingNearby: Boolean = false,
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
    private val gbis: TransitCatalogRepository,
    private val tago: TagoTransitCatalog,
    private val favorites: FavoritesRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    private var fetchJob: Job? = null

    init { loadCities() }

    private fun catalog(provider: TransitProvider): TransitCatalogRepository =
        if (provider == TransitProvider.TAGO) tago else gbis

    fun setProvider(provider: TransitProvider) {
        fetchJob?.cancel()
        _uiState.update {
            it.copy(provider = provider, routes = emptyList(), selectedRoute = null,
                stops = emptyList(), selectedStop = null, stopFilter = "",
                searching = false, loadingStops = false, saving = false,
                error = null, notice = null)
        }
    }

    fun setCityCode(value: String) {
        fetchJob?.cancel()
        _uiState.update { it.copy(cityCode = value.trim(), routes = emptyList(),
            selectedRoute = null, stops = emptyList(), selectedStop = null,
            searching = false, loadingStops = false, error = null, notice = null) }
    }

    fun loadCities() {
        viewModelScope.launch {
            _uiState.update { it.copy(loadingCities = true) }
            try {
                val cities = tago.getCities()
                _uiState.update { state ->
                    val current = if (cities.any { it.code == state.cityCode }) state.cityCode
                        else cities.firstOrNull { it.name.contains("구리") }?.code
                        ?: cities.firstOrNull()?.code ?: state.cityCode
                    state.copy(cities = cities, cityCode = current, loadingCities = false)
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _uiState.update { it.copy(loadingCities = false,
                    error = if (it.provider == TransitProvider.TAGO) friendlyMessage(e) else it.error) }
            }
        }
    }

    /** 현재 GPS 반경 500m의 실제 정류장을 통해 도시 후보를 고른다. */
    fun findNearbyCities(here: GeoPoint?) {
        if (here == null) {
            _uiState.update { it.copy(error = "먼저 홈 화면에서 현재 위치를 새로고침해 주세요.") }
            return
        }
        fetchJob?.cancel()
        _uiState.update { it.copy(loadingNearby = true, error = null) }
        fetchJob = viewModelScope.launch {
            try {
                val stops = tago.getNearbyStops(here)
                val closest = stops.filter { it.point != null }.minByOrNull {
                    val latitude = it.point!!.latitude - here.latitude
                    val longitude = it.point.longitude - here.longitude
                    latitude * latitude + longitude * longitude
                } ?: stops.firstOrNull()
                _uiState.update { state ->
                    state.copy(loadingNearby = false, nearbyStops = stops,
                        cityCode = closest?.cityCode ?: state.cityCode,
                        routes = emptyList(), selectedRoute = null,
                        stops = emptyList(), selectedStop = null,
                        notice = if (stops.isEmpty()) "주변 500m에 TAGO 정류장이 없어요. 도시를 직접 선택해 주세요."
                            else "주변 정류장의 도시코드를 선택했어요. 다른 지역 노선이면 도시를 변경해 보세요.")
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _uiState.update { it.copy(loadingNearby = false, error = friendlyMessage(e)) }
            }
        }
    }

    fun updateQuery(query: String) {
        fetchJob?.cancel()
        _uiState.update { it.copy(query = query, routes = emptyList(),
            selectedRoute = null, stops = emptyList(), selectedStop = null,
            stopFilter = "", searching = false, error = null, notice = null) }
    }
    fun updateStopFilter(value: String) {
        _uiState.update { it.copy(stopFilter = value) }
    }

    fun reset() {
        fetchJob?.cancel()
        _uiState.update { it.copy(query = "", routes = emptyList(), selectedRoute = null,
            stops = emptyList(), selectedStop = null, stopFilter = "",
            searching = false, loadingStops = false, saving = false,
            error = null, notice = null) }
    }

    fun searchRoutes(number: String = _uiState.value.query) {
        fetchJob?.cancel()
        val query = number.trim()
        if (query.isBlank()) {
            _uiState.update { it.copy(error = "버스 번호를 입력해 주세요.") }
            return
        }
        val snapshot = _uiState.value
        if (snapshot.provider == TransitProvider.TAGO && snapshot.cityCode.isBlank()) {
            _uiState.update { it.copy(error = "TAGO 도시코드를 먼저 선택해 주세요.") }
            return
        }
        _uiState.update { it.copy(query = query, routes = emptyList(), selectedRoute = null,
            stops = emptyList(), selectedStop = null, searching = true, error = null, notice = null) }
        fetchJob = viewModelScope.launch {
            try {
                val routes = catalog(snapshot.provider).searchBusRoutes(query,
                    if (snapshot.provider == TransitProvider.TAGO) snapshot.cityCode else null)
                _uiState.update { it.copy(routes = routes, searching = false,
                    notice = if (routes.isEmpty()) "이 도시에서 검색된 노선이 없어요. 인접 도시코드도 확인해 주세요."
                        else null) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _uiState.update { it.copy(searching = false, error = friendlyMessage(e)) }
            }
        }
    }

    fun selectRoute(route: BusRoute) {
        fetchJob?.cancel()
        _uiState.update { it.copy(selectedRoute = route, stops = emptyList(), selectedStop = null,
            stopFilter = "", loadingStops = true, error = null, notice = null) }
        fetchJob = viewModelScope.launch {
            try {
                val stops = catalog(route.provider).getRouteStops(route.id, route.cityCode)
                _uiState.update { it.copy(stops = stops, loadingStops = false,
                    notice = if (stops.isEmpty()) "경유 정류소 목록이 비어 있어요." else null) }
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
                val verified = ValidateRouteStopUseCase(catalog(route.provider))(
                    route.id, selected.stationId, selected.stationSeq, route.cityCode)
                favorites.save(TransitFavorite(
                    id = UUID.randomUUID().toString(), mode = TransitMode.BUS,
                    route = route.number, stop = verified.name,
                    direction = DescribeBusDirectionUseCase()(route, snapshot.stops, verified),
                    anchor = verified.point,
                    officialRouteId = route.id, officialStationId = verified.stationId,
                    officialStationSeq = verified.stationSeq,
                    provider = route.provider, officialCityCode = route.cityCode
                ))
                _uiState.update { it.copy(saving = false, selectedStop = null,
                    notice = verified.name + " 정류장을 즐겨찾기에 저장했어요.") }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _uiState.update { it.copy(saving = false, error = friendlyMessage(e)) }
            }
        }
    }

    fun saveSubwayManual(line: String, stop: String, direction: String, here: GeoPoint?) {
        if (line.isBlank() || stop.isBlank() || direction.isBlank()) {
            _uiState.update { it.copy(error = "노선·역·방향을 모두 입력해 주세요.") }
            return
        }
        viewModelScope.launch {
            try {
                favorites.save(TransitFavorite(UUID.randomUUID().toString(), TransitMode.SUBWAY,
                    line.trim(), stop.trim(), direction.trim(), here))
                _uiState.update { it.copy(notice = "지하철 경로를 저장했어요. 공식 검증은 추후 지원해요.",
                    error = null) }
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
                    return SearchViewModel(container.transitCatalog, container.tagoCatalog,
                        container.favoritesRepository) as T
                }
            }
    }
}

package com.barota.app.presentation.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.barota.app.domain.model.BusRoute
import com.barota.app.domain.model.GeoPoint
import com.barota.app.domain.model.RouteStop
import com.barota.app.domain.model.TransitMode
import com.barota.app.domain.model.TransitProvider
import com.barota.app.domain.model.directionForStop

@Composable
fun SearchScreen(
    state: SearchUiState,
    currentLocation: GeoPoint?,
    onProviderChange: (TransitProvider) -> Unit,
    onCityCodeChange: (String) -> Unit,
    onRefreshCities: () -> Unit,
    onFindNearbyCities: () -> Unit,
    onUpdateQuery: (String) -> Unit,
    onSearchRoutes: (String) -> Unit,
    onSelectRoute: (BusRoute) -> Unit,
    onUpdateStopFilter: (String) -> Unit,
    onSelectStop: (RouteStop) -> Unit,
    onSaveSelectedStop: () -> Unit,
    onSaveSubway: (String, String, String, GeoPoint?) -> Unit,
    modifier: Modifier = Modifier
) {
    var mode by rememberSaveable { mutableStateOf(TransitMode.BUS) }
    var cityMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var subwayLine by rememberSaveable { mutableStateOf("") }
    var subwayStop by rememberSaveable { mutableStateOf("") }
    var subwayDirection by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("경로 검색·추가", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("버스는 TAGO 또는 GBIS에서 공식 노선·정류장을 선택해 저장해요.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TransitMode.entries.forEach { candidate ->
                FilterChip(
                    selected = mode == candidate,
                    onClick = { mode = candidate }, label = { Text(candidate.label) }
                )
            }
        }

        if (mode == TransitMode.BUS) {
            Text("버스정보 제공처", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TransitProvider.entries.forEach { provider ->
                    FilterChip(selected = state.provider == provider,
                        onClick = { onProviderChange(provider) },
                        label = { Text(provider.label) })
                }
            }
            if (state.provider == TransitProvider.TAGO) {
                Text("TAGO 노선 검색은 도시코드가 필요해요. 다른 지역에서 출발한 버스는 해당 도시도 확인하세요.",
                    style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box {
                        TextButton(onClick = { cityMenuExpanded = true }) {
                            val name = state.cities.firstOrNull { it.code == state.cityCode }?.name
                                ?: "직접 지정"
                            Text("도시 선택: " + name + " (" + state.cityCode + ") ▾")
                        }
                        DropdownMenu(expanded = cityMenuExpanded,
                            onDismissRequest = { cityMenuExpanded = false },
                            modifier = Modifier.heightIn(max = 320.dp)) {
                            state.cities.forEach { city ->
                                DropdownMenuItem(
                                    text = { Text(city.name + " (" + city.code + ")") },
                                    onClick = {
                                        onCityCodeChange(city.code)
                                        cityMenuExpanded = false
                                    })
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = state.cityCode, onValueChange = onCityCodeChange,
                    modifier = Modifier.fillMaxWidth(), label = { Text("도시코드 (직접 입력 가능)") },
                    singleLine = true, placeholder = { Text("예: 구리 31120") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onRefreshCities, enabled = !state.loadingCities) {
                        Text(if (state.loadingCities) "도시 목록 조회 중…" else "도시 목록 새로고침")
                    }
                    TextButton(onClick = onFindNearbyCities, enabled = !state.loadingNearby) {
                        Text(if (state.loadingNearby) "주변 확인 중…" else "GPS 근처 도시 찾기")
                    }
                }
                if (state.nearbyStops.isNotEmpty()) {
                    Text("내 주변 TAGO 정류장 (500m, 일부)", fontWeight = FontWeight.SemiBold)
                    state.nearbyStops.take(6).forEach { nearby ->
                        Text("• " + nearby.name + " · 도시코드 " + nearby.cityCode,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Text("이 정류장을 경유하는 노선인지 저장 전에 다시 확인해요.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("자주 검색하는 버스", fontWeight = FontWeight.SemiBold)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("9", "51", "1-1").forEach { number ->
                    FilterChip(selected = state.query == number, onClick = { onSearchRoutes(number) },
                        label = { Text("${number}번") })
                }
            }
            OutlinedTextField(
                value = state.query,
                onValueChange = onUpdateQuery,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("버스 노선번호") },
                placeholder = { Text("예: 9, 51, 1-1") },
                singleLine = true
            )
            Button(
                onClick = { onSearchRoutes(state.query) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.query.isNotBlank() && !state.searching && !state.loadingStops
            ) { Text("공식 노선 검색") }

            if (state.searching || state.loadingStops || state.saving) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator()
                    Text(when {
                        state.saving -> "공식 노선·정류장을 다시 검증하고 저장 중…"
                        state.loadingStops -> "노선의 실제 정류장을 불러오는 중…"
                        else -> "버스 노선 검색 중…"
                    })
                }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            val selectedRoute = state.selectedRoute
            if (selectedRoute == null) {
                if (state.routes.isNotEmpty()) {
                    Text("검색 결과 ${state.routes.size}개 — 지역·기점·종점을 확인하고 선택해 주세요.",
                        style = MaterialTheme.typography.titleSmall)
                    state.routes.take(40).forEach { route ->
                        OutlinedCard(onClick = { onSelectRoute(route) }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${route.number}번 · ${route.regionName.ifBlank { "지역 정보 없음" }}",
                                    fontWeight = FontWeight.Bold)
                                Text("${route.startStationName} → ${route.endStationName}",
                                    style = MaterialTheme.typography.bodySmall)
                                Text("제공처: " + route.provider.label + " · 노선 ID: " + route.id, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    if (state.routes.size > 40) Text("결과가 많아요. 버스 번호를 더 정확히 입력해 주세요.")
                }
            } else {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("선택한 노선: ${selectedRoute.number}번", fontWeight = FontWeight.Bold)
                        Text("${selectedRoute.regionName} · ${selectedRoute.startStationName} → ${selectedRoute.endStationName}")
                        TextButton(onClick = { onSearchRoutes(state.query) }) { Text("다른 노선 선택") }
                    }
                }
                if (state.stops.isNotEmpty()) {
                    OutlinedTextField(
                        value = state.stopFilter,
                        onValueChange = onUpdateStopFilter,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("이 노선의 경유 정류장 이름·번호 검색") },
                        placeholder = { Text("예: 구리역, 22247") }, singleLine = true
                    )
                    val matched = state.stops.filter {
                        state.stopFilter.isBlank() || it.name.contains(state.stopFilter, true) ||
                            (it.mobileNo?.contains(state.stopFilter, true) == true)
                    }
                    Text("실제 경유 ${state.stops.size}개 중 ${matched.size}개 표시 후보",
                        style = MaterialTheme.typography.titleSmall)
                    matched.take(40).forEach { stop ->
                        val selected = state.selectedStop?.let {
                            it.stationId == stop.stationId && it.stationSeq == stop.stationSeq
                        } ?: false
                        OutlinedCard(onClick = { onSelectStop(stop) }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${if (selected) "✓ " else ""}${stop.name}", fontWeight = FontWeight.SemiBold)
                                Text("정류장번호 ${stop.mobileNo ?: "-"} · 순번 ${stop.stationSeq}" +
                                    " · ${stop.regionName}", style = MaterialTheme.typography.bodySmall)
                                Text("${directionForStop(state.stops, stop)}", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    if (matched.size > 40) Text("전체 결과 중 처음 40개만 표시해요. 정류장 이름으로 좁혀 주세요.")
                    state.selectedStop?.let { selected ->
                        Text("선택: ${selected.name} (${directionForStop(state.stops, selected)})",
                            fontWeight = FontWeight.SemiBold)
                        Button(onClick = onSaveSelectedStop, enabled = !state.saving,
                            modifier = Modifier.fillMaxWidth()) {
                            Text("공식 정류장 검증 후 즐겨찾기에 추가")
                        }
                    }
                }
            }
            Text("정식 노선·정류장 ID와 순번을 검증해 저장해요. " +
                "도착 예정시간은 다음 버전에서 연결됩니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text("지하철 수동 등록 (아직 공식 검증 전)", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(subwayLine, { subwayLine = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text("노선명") }, placeholder = { Text("경의중앙선") })
            OutlinedTextField(subwayStop, { subwayStop = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text("승차역") })
            OutlinedTextField(subwayDirection, { subwayDirection = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text("방향") })
            Button(onClick = { onSaveSubway(subwayLine, subwayStop, subwayDirection, currentLocation) },
                modifier = Modifier.fillMaxWidth(),
                enabled = subwayLine.isNotBlank() && subwayStop.isNotBlank() && subwayDirection.isNotBlank()) {
                Text("지하철 경로 수동 저장")
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            Text("지하철 경로는 미검증으로 표시됩니다.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

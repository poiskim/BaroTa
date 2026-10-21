package com.barota.app.presentation.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.barota.app.domain.model.BusRoute
import com.barota.app.domain.model.GeoPoint
import com.barota.app.domain.model.RouteStop
import com.barota.app.domain.model.TransitMode
import com.barota.app.domain.model.directionForStop

@Composable
fun SearchScreen(
    state: SearchUiState,
    currentLocation: GeoPoint?,
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
    var subwayLine by rememberSaveable { mutableStateOf("") }
    var subwayStop by rememberSaveable { mutableStateOf("") }
    var subwayDirection by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("경로 검색·추가", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("버스는 공식 GBIS 노선과 경유 정류장을 선택해야 저장돼요.",
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
                                Text("노선 ID: ${route.id}", style = MaterialTheme.typography.labelSmall)
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
            Text("버스는 이름을 임의로 입력해 저장하지 않아요. 공식 ID와 정류장 순번을 검증해요. " +
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

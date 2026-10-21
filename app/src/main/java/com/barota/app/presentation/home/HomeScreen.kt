package com.barota.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.barota.app.domain.model.RecommendedFavorite
import com.barota.app.domain.model.TransitProvider
import com.barota.app.presentation.components.FavoriteCard

@Composable
fun HomeScreen(
    state: HomeUiState,
    onLocate: () -> Unit,
    onRefreshArrivals: () -> Unit,
    onSearch: (String?) -> Unit,
    onOpenDetail: (RecommendedFavorite) -> Unit,
    onPin: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 화면 진입이나 저장한 목록 변경 시 한 번 자동 조회; 반복 폴링은 하지 않는다.
    val savedIds = state.items.map { it.favorite.id }
    LaunchedEffect(savedIds) {
        if (state.items.any { it.favorite.provider == TransitProvider.TAGO &&
                it.favorite.isOfficiallyVerified }) onRefreshArrivals()
    }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("바로타", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold)
        Text("지금 탈 버스,\n바로 찾아보세요.", style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold)

        OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("◎ 저장 경로 · 위치 기반 우선순위", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Text(state.locationMessage, style = MaterialTheme.typography.bodyMedium)
                if (state.location != null) {
                    Text("좌표: %.4f, %.4f".format(state.location.latitude, state.location.longitude),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onLocate) { Text("현재 위치 새로고침 →") }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("자주 찾는 번호", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("9", "51", "1-1").forEach { number ->
                    AssistChip(onClick = { onSearch(number) }, label = { Text("${number}번") })
                }
            }
            Text("번호를 누르면 공식 노선을 검색해요. 경유 정류장을 선택해 저장하세요.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("나의 추천 경로", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            TextButton(onClick = { onSearch(null) }) { Text("+ 추가") }
        }
        if (state.items.isNotEmpty()) {
            TextButton(onClick = onRefreshArrivals, enabled = !state.arrivalRefreshing) {
                Text(if (state.arrivalRefreshing) "실시간 도착정보 조회 중…" else "↻ 실시간 도착정보 새로고침")
            }
        }
        if (state.arrivalsCheckedAt != null) {
            val refreshed = java.text.SimpleDateFormat("HH:mm", java.util.Locale.KOREA)
                .format(java.util.Date(state.arrivalsCheckedAt))
            Text("도착정보 " + refreshed + " 기준 · 실시간 값은 변동될 수 있어요 (TAGO 최대 15개 갱신)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.items.isEmpty()) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("아직 저장한 경로가 없어요", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Text("예: 9번 · 내가 타는 정류장 · 원하는 방향\n\n저장할 때 현재 위치도 함께 등록하면, 그 장소에 왔을 때 위로 추천할 수 있어요.")
                    Button(onClick = { onSearch(null) }) { Text("첫 경로 추가하기") }
                }
            }
        } else {
            state.items.forEach { item ->
                FavoriteCard(
                    recommended = item,
                    arrival = state.arrivals[item.favorite.id],
                    onDetail = { onOpenDetail(item) },
                    onPin = { onPin(item.favorite.id) },
                    onDelete = { onDelete(item.favorite.id) }
                )
            }
        }
        Text(
            "TAGO에서 저장한 버스는 실제 도착예정 시간을 조회해요. GBIS·지하철 도착정보는 아직 미연결이며, TAGO에서도 지역에 따라 실시간 정보가 없을 수 있어요.",
            modifier = Modifier.padding(bottom = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

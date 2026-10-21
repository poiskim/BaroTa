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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.barota.app.domain.model.RecommendedFavorite
import com.barota.app.presentation.components.FavoriteCard

@Composable
fun HomeScreen(
    state: HomeUiState,
    onLocate: () -> Unit,
    onSearch: (String?) -> Unit,
    onOpenDetail: (RecommendedFavorite) -> Unit,
    onPin: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
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
                Text("◎ 내 위치 기반 추천", style = MaterialTheme.typography.titleMedium,
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
                    onDetail = { onOpenDetail(item) },
                    onPin = { onPin(item.favorite.id) },
                    onDelete = { onDelete(item.favorite.id) }
                )
            }
        }
        Text(
            "버스는 TAGO 또는 GBIS의 공식 노선·정류장을 검색해 저장할 수 있어요. 실제 도착시간은 아직 연동하지 않았어요.",
            modifier = Modifier.padding(bottom = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

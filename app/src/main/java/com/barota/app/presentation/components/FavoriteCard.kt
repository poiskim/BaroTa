package com.barota.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.barota.app.domain.model.RecommendedFavorite
import com.barota.app.domain.model.TransitMode
import kotlin.math.roundToInt

@Composable
fun FavoriteCard(
    recommended: RecommendedFavorite,
    onDetail: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit
) {
    val item = recommended.favorite
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = onDetail,
                    label = { Text(if (item.mode == TransitMode.BUS) "버스" else "지하철") }
                )
                Spacer(Modifier.width(8.dp))
                Text(item.route, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onPin) { Text(if (item.isPinned) "★ 고정됨" else "☆ 고정") }
            }
            Text(item.stop, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(if (item.isOfficiallyVerified) "✓ " + item.provider.label + " 공식 정류장 확인" else "미검증 경로 · 이전 수동 입력",
                style = MaterialTheme.typography.labelMedium,
                color = if (item.isOfficiallyVerified) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant)
            Text("방향: ${item.direction}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                buildString {
                    append(recommended.reason)
                    if (recommended.distanceMeters != null) {
                        append(" · 약 ")
                        val d = recommended.distanceMeters ?: 0
                        append(if (d < 1000) "${d}m" else "${(d / 100.0).roundToInt() / 10.0}km")
                    }
                    append(" · 조회 ${item.viewCount}회")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "실시간 도착정보 연결 전",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onDetail) { Text("경로 상세") }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDelete) { Text("삭제") }
            }
        }
    }
}

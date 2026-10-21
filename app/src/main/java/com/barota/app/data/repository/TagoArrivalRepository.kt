package com.barota.app.data.repository

import com.barota.app.data.remote.api.TagoApi
import com.barota.app.data.remote.dto.TagoXmlParser
import com.barota.app.domain.model.ArrivalStatus
import com.barota.app.domain.model.TransitFavorite
import com.barota.app.domain.model.TransitMode
import com.barota.app.domain.model.TransitProvider
import com.barota.app.domain.repository.ArrivalRepository
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * 같은 TAGO 정류장(도시코드 + nodeId)을 공유하는 즐겨찾기는 API를 한 번만 호출한다.
 * GBIS, 지하철, 공식 ID 없는 옛 즐겨찾기는 실제 도착정보를 지원한다고 가장하지 않는다.
 */
class TagoArrivalRepository(
    private val api: TagoApi,
    private val parser: TagoXmlParser = TagoXmlParser(),
    private val mapper: TagoArrivalMapper = TagoArrivalMapper()
) : ArrivalRepository {
    private data class StopKey(val cityCode: String, val nodeId: String)

    override suspend fun getArrival(item: TransitFavorite): ArrivalStatus =
        getArrivals(listOf(item))[item.id] ?: ArrivalStatus.NotConnected

    override suspend fun getArrivals(items: List<TransitFavorite>): Map<String, ArrivalStatus> =
        coroutineScope {
            val supported = items.filter {
                it.mode == TransitMode.BUS && it.provider == TransitProvider.TAGO &&
                    it.isOfficiallyVerified &&
                    !it.officialCityCode.isNullOrBlank() && !it.officialStationId.isNullOrBlank()
            }
            val groups = supported.groupBy {
                StopKey(requireNotNull(it.officialCityCode), requireNotNull(it.officialStationId))
            }
            val limit = Semaphore(3) // 공공 API의 초당 호출 제한을 고려해 동시 요청 수 제한
            val perStop = groups.map { (stop, saved) ->
                async {
                    val statuses = try {
                        val rows = limit.withPermit { getRows(stop) }
                        saved.associate { item ->
                            item.id to mapper.map(rows, requireNotNull(item.officialRouteId), stop.nodeId)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // 민감할 수 있는 서버 응답 본문은 사용자 UI에 표시하지 않는다.
                        val message = when {
                            e.message?.contains("TAGO") == true -> e.message.orEmpty()
                            else -> "도착정보 조회 실패. 연결 상태·TAGO 버스도착정보 활용승인을 확인해 주세요."
                        }
                        saved.associate { it.id to ArrivalStatus.Failure(message) }
                    }
                    statuses
                }
            }.awaitAll().fold(emptyMap<String, ArrivalStatus>()) { acc, map -> acc + map }
            items.associate { item ->
                item.id to (perStop[item.id] ?: ArrivalStatus.NotConnected)
            }
        }

    private suspend fun getRows(key: StopKey): List<Map<String, String>> {
        val rows = mutableListOf<Map<String, String>>()
        val pageSize = 100
        for (page in 1..10) {
            val data = parser.parse(api.getStationArrivals(key.cityCode, key.nodeId, page, pageSize))
            rows.addAll(data.rows)
            if (data.rows.isEmpty() || page * pageSize >= data.totalCount) return rows
        }
        throw IOException("TAGO 도착정보가 너무 많아 전체 응답을 확인하지 못했어요.")
    }
}

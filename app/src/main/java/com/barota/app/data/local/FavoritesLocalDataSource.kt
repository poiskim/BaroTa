package com.barota.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.barota.app.domain.model.GeoPoint
import com.barota.app.domain.model.TransitFavorite
import com.barota.app.domain.model.TransitMode
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.favoritesDataStore by preferencesDataStore(name = "saved_transit")

/** 작은 MVP에는 DataStore + JSON. 데이터 증가 시 Room DAO로 교체하기 쉽도록 분리. */
class FavoritesLocalDataSource(context: Context) {
    private val store = context.applicationContext.favoritesDataStore
    private val key = stringPreferencesKey("favorites_v1")

    val all: Flow<List<TransitFavorite>> = store.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> decode(preferences[key]) }

    suspend fun update(change: (List<TransitFavorite>) -> List<TransitFavorite>) {
        store.edit { preferences ->
            preferences[key] = encode(change(decode(preferences[key])))
        }
    }

    private fun decode(raw: String?): List<TransitFavorite> = try {
        if (raw.isNullOrBlank()) emptyList() else {
            val array = JSONArray(raw)
            (0 until array.length()).map { index ->
                val obj = array.getJSONObject(index)
                val hasAnchor = !obj.isNull("lat") && !obj.isNull("lng")
                TransitFavorite(
                    id = obj.getString("id"),
                    mode = TransitMode.valueOf(obj.getString("mode")),
                    route = obj.getString("route"),
                    stop = obj.getString("stop"),
                    direction = obj.getString("direction"),
                    anchor = if (hasAnchor) GeoPoint(obj.getDouble("lat"), obj.getDouble("lng")) else null,
                    viewCount = obj.optInt("viewCount", 0),
                    lastViewedAt = obj.optLong("lastViewedAt", 0),
                    isPinned = obj.optBoolean("isPinned", false),
                    createdAt = obj.optLong("createdAt", 0),
                    officialRouteId = if (obj.has("officialRouteId") && !obj.isNull("officialRouteId"))
                        obj.optString("officialRouteId").ifBlank { null } else null,
                    officialStationId = if (obj.has("officialStationId") && !obj.isNull("officialStationId"))
                        obj.optString("officialStationId").ifBlank { null } else null,
                    officialStationSeq = if (obj.has("officialStationSeq") && !obj.isNull("officialStationSeq"))
                        obj.optInt("officialStationSeq") else null
                )
            }
        }
    } catch (_: Exception) {
        // 손상된 로컬 데이터로 앱이 크래시하지 않게 처리. 실서비스에서는 로깅/복구 보완 필요.
        emptyList()
    }

    private fun encode(items: List<TransitFavorite>): String {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id)
                put("mode", item.mode.name)
                put("route", item.route)
                put("stop", item.stop)
                put("direction", item.direction)
                put("lat", item.anchor?.latitude ?: JSONObject.NULL)
                put("lng", item.anchor?.longitude ?: JSONObject.NULL)
                put("viewCount", item.viewCount)
                put("lastViewedAt", item.lastViewedAt)
                put("isPinned", item.isPinned)
                put("createdAt", item.createdAt)
                put("officialRouteId", item.officialRouteId ?: JSONObject.NULL)
                put("officialStationId", item.officialStationId ?: JSONObject.NULL)
                put("officialStationSeq", item.officialStationSeq ?: JSONObject.NULL)
            })
        }
        return array.toString()
    }
}

package com.barota.app.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import com.barota.app.domain.model.GeoPoint
import com.barota.app.domain.repository.LocationRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** 앱이 열려 있을 때만 요청. 백그라운드 위치 수집 없음. */
class AndroidLocationRepository(private val context: Context) : LocationRepository {
    private val manager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @Suppress("MissingPermission")
    override suspend fun currentLocation(): GeoPoint? {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return null

        val providers = buildList {
            if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) add(LocationManager.NETWORK_PROVIDER)
            if (fine && manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) add(LocationManager.GPS_PROVIDER)
        }
        if (providers.isEmpty()) return null

        try {
            // 10분보다 오래된 마지막 좌표를 현재 위치로 취급하지 않는다.
            val bestCached = providers.mapNotNull { manager.getLastKnownLocation(it) }
                .filter { System.currentTimeMillis() - it.time in 0..600_000 }
                .maxByOrNull { it.time }
            if (bestCached != null) return bestCached.toPoint()

            for (provider in providers) {
                val found = withTimeoutOrNull(8_000) {
                    suspendCancellableCoroutine<Location?> { continuation ->
                        val cancellation = CancellationSignal()
                        continuation.invokeOnCancellation { cancellation.cancel() }
                        manager.getCurrentLocation(provider, cancellation, context.mainExecutor) { location ->
                            if (continuation.isActive) continuation.resume(location)
                        }
                    }
                }
                if (found != null) return found.toPoint()
            }
        } catch (_: SecurityException) {
            return null
        } catch (_: IllegalArgumentException) {
            return null
        }
        return null
    }

    private fun Location.toPoint(): GeoPoint = GeoPoint(latitude, longitude)
}

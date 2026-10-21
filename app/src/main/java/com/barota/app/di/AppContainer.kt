package com.barota.app.di

import android.content.Context
import com.barota.app.BuildConfig
import com.barota.app.data.local.FavoritesLocalDataSource
import com.barota.app.data.location.AndroidLocationRepository
import com.barota.app.data.remote.api.GbisApi
import com.barota.app.data.repository.DefaultFavoritesRepository
import com.barota.app.data.repository.GbisTransitCatalogRepository
import com.barota.app.data.repository.UnconfiguredArrivalRepository
import com.barota.app.domain.repository.ArrivalRepository
import com.barota.app.domain.repository.FavoritesRepository
import com.barota.app.domain.repository.LocationRepository
import com.barota.app.domain.repository.TransitCatalogRepository
import com.barota.app.domain.usecase.RankFavoritesUseCase
import com.barota.app.domain.usecase.ValidateRouteStopUseCase

/** 작은 프로젝트라 수동 DI. 필요할 때 Hilt로 옮겨도 Repository 계약은 유지된다. */
class AppContainer(context: Context) {
    val favoritesRepository: FavoritesRepository = DefaultFavoritesRepository(FavoritesLocalDataSource(context))
    val locationRepository: LocationRepository = AndroidLocationRepository(context)
    val arrivalRepository: ArrivalRepository = UnconfiguredArrivalRepository()
    val rankFavorites = RankFavoritesUseCase()
    val transitCatalog: TransitCatalogRepository = GbisTransitCatalogRepository(GbisApi(BuildConfig.GBIS_SERVICE_KEY))
    val validateRouteStop = ValidateRouteStopUseCase(transitCatalog)
}

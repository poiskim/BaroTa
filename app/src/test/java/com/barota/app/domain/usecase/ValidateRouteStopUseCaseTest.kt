package com.barota.app.domain.usecase

import com.barota.app.domain.model.BusRoute
import com.barota.app.domain.model.RouteStop
import com.barota.app.domain.repository.TransitCatalogRepository
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ValidateRouteStopUseCaseTest {
    private class FakeCatalog(val stops: List<RouteStop> = emptyList(), val failure: Boolean = false) :
        TransitCatalogRepository {
        override suspend fun searchBusRoutes(query: String): List<BusRoute> = emptyList()
        override suspend fun getRouteStops(routeId: String): List<RouteStop> {
            if (failure) throw IOException("network unavailable")
            return stops
        }
    }

    @Test fun `matching station id and seq passes`() = runBlocking {
        val selected = RouteStop("2001", 15, "구리역", "22148", "구리", null)
        val result = ValidateRouteStopUseCase(FakeCatalog(listOf(selected)))("routeA", "2001", 15)
        assertEquals(selected, result)
    }

    @Test fun `same stop id at different seq must not pass`() {
        val selected = RouteStop("2001", 15, "구리역", "22148", "구리", null)
        assertThrows(StopNotOnRouteException::class.java) {
            runBlocking { ValidateRouteStopUseCase(FakeCatalog(listOf(selected)))("routeA", "2001", 55) }
        }
    }

    @Test fun `station not on route is rejected`() {
        assertThrows(StopNotOnRouteException::class.java) {
            runBlocking { ValidateRouteStopUseCase(FakeCatalog())("routeA", "missing", 2) }
        }
    }

    @Test fun `network errors are not treated as no station`() {
        assertThrows(IOException::class.java) {
            runBlocking { ValidateRouteStopUseCase(FakeCatalog(failure = true))("routeA", "2001", 1) }
        }
    }
}

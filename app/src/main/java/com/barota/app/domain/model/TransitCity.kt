package com.barota.app.domain.model

data class TransitCity(val code: String, val name: String)

data class NearbyTransitStop(
    val stationId: String,
    val name: String,
    val cityCode: String,
    val point: GeoPoint?
)

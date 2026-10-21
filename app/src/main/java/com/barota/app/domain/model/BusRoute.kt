package com.barota.app.domain.model

/** API 제공처에 따라 구분되는 공식 노선 식별자. 노선번호만으로는 노선을 특정할 수 없다. */
data class BusRoute(
    val id: String,
    val number: String,
    val regionName: String,
    val startStationName: String,
    val endStationName: String,
    val provider: TransitProvider = TransitProvider.GBIS,
    val cityCode: String? = null
)

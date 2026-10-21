package com.barota.app.domain.model

/** 공식 노선 ID는 제공처마다 중복될 수 있으므로 반드시 제공처와 함께 저장한다. */
enum class TransitProvider(val label: String) {
    TAGO("TAGO 전국"), GBIS("GBIS 경기")
}

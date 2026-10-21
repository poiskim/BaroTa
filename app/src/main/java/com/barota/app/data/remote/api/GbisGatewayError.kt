package com.barota.app.data.remote.api

/**
 * 공공데이터포털 게이트웨이의 오류 코드를 사용자 친화적인 문장으로 변환한다.
 * 오류 응답 전체를 화면/로그에 노출하지 않아 인증키나 요청 URL이 새어 나가지 않도록 한다.
 */
internal object GbisGatewayError {
    private val knownCodes = listOf(
        "SERVICE_KEY_IS_NOT_REGISTERED_ERROR",
        "SERVICE_ACCESS_DENIED_ERROR",
        "PERMISSION_DENIED",
        "SERVICE_KEY_IS_NULL",
        "DEADLINE_HAS_EXPIRED_ERROR",
        "LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR",
        "LIMITED_NUMBER_OF_SERVICE_REQUESTS_PER_SECOND_EXCEEDS_ERROR",
        "BLACKLIST_IP_ACCESS_ERROR",
        "NO_OPENAPI_SERVICE_ERROR"
    )

    fun message(httpCode: Int, errorBody: String?): String {
        val reason = knownCodes.firstOrNull { code ->
            errorBody?.contains(code, ignoreCase = true) == true
        }
        val advice = when (reason) {
            "SERVICE_KEY_IS_NOT_REGISTERED_ERROR" ->
                "등록되지 않은 서비스키예요. 공공데이터포털의 키와 활용신청 내역을 확인해 주세요."
            "SERVICE_ACCESS_DENIED_ERROR", "PERMISSION_DENIED" ->
                "이 API에 대한 사용 권한이 없어요. '경기도_버스노선 조회' 활용신청 및 승인 상태를 확인해 주세요."
            "SERVICE_KEY_IS_NULL" ->
                "인증키가 요청에 포함되지 않았어요. local.properties의 GBIS_SERVICE_KEY를 확인하고 Rebuild 해 주세요."
            "DEADLINE_HAS_EXPIRED_ERROR" ->
                "인증키 사용 기간이 만료되었어요. 공공데이터포털에서 갱신 상태를 확인해 주세요."
            "LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR" ->
                "일일 API 호출 허용량을 초과했어요."
            "LIMITED_NUMBER_OF_SERVICE_REQUESTS_PER_SECOND_EXCEEDS_ERROR" ->
                "짧은 시간에 요청이 너무 많아요. 잠시 후 다시 시도해 주세요."
            "BLACKLIST_IP_ACCESS_ERROR" ->
                "요청한 네트워크의 IP가 차단되어 있어요. 공공데이터포털에 문의해 주세요."
            "NO_OPENAPI_SERVICE_ERROR" ->
                "요청한 API 주소 또는 서비스가 유효하지 않아요."
            else -> when (httpCode) {
                401, 403 -> "접근이 거부됐어요. API 인증키 및 '경기도_버스노선 조회' 활용신청 상태를 확인해 주세요."
                429 -> "API 호출 제한에 도달했어요. 잠시 후 다시 시도해 주세요."
                in 500..599 -> "공공데이터 서버에 문제가 발생했어요. 잠시 후 다시 시도해 주세요."
                else -> "서버 요청이 실패했어요. 네트워크와 API 설정을 확인해 주세요."
            }
        }
        return "GBIS HTTP $httpCode" + (reason?.let { " ($it)" } ?: "") + ": $advice"
    }
}

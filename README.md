# 바로타 Android v0.3 — TAGO 버스 검색 + 기존 GBIS 유지

Kotlin / Jetpack Compose / MVVM + Repository. v0.2의 GBIS 기능을 삭제하지 않고 TAGO 버스노선·경유 정류장 검색을 추가한 버전입니다. **지하철 API와 실시간 도착정보는 이번 버전에서 변경하지 않았습니다.**

## 1. API 인증키 설정

프로젝트 루트의 `local.properties` (Android Studio가 만든 `sdk.dir` 줄은 그대로 유지):

```properties
TAGO_SERVICE_KEY=TAGO_일반_인증키_값
GBIS_SERVICE_KEY=GBIS_인증키_값
```

- **TAGO_SERVICE_KEY 필수**: `국토교통부_(TAGO)_버스노선정보` 및 `버스정류소정보` 활용승인과 연동되는 공공데이터포털 서비스키를 입력합니다. TAGO 인증키는 인코딩/디코딩 어느 쪽이든 URL에서 한 번만 인코딩하도록 처리합니다.
- **GBIS_SERVICE_KEY 선택**: GBIS 조회도 쓸 때만 설정합니다. 키가 없으면 GBIS 조회 시 안내가 나옵니다.
- `local.properties`는 `.gitignore`로 제외되어 있습니다. 공개 GitHub에 인증키를 올리지 마세요.
- 키를 수정했다면 **Sync Project with Gradle Files → Build → Rebuild Project → Run**.
- 개발 단계에서는 APK의 BuildConfig를 통해 호출하지만, 공개 배포에서는 서비스키를 백엔드 프록시에서 관리해야 합니다. 앱 APK의 키는 추출 가능하기 때문입니다.

## 2. 사용 순서

1. **홈 → 현재 위치 새로고침**을 눌러 GPS 위치를 가져옵니다 (선택 사항).
2. 홈의 `9`, `51`, `1-1` 또는 `검색·추가`를 누릅니다.
3. 버스정보 제공처에서 기본 선택된 **TAGO 전국** 또는 **GBIS 경기**를 선택합니다.
4. TAGO 사용 시 도시코드를 선택하거나 입력합니다. 구리(31120)를 기본값으로 시도하되 **남양주 등 다른 도시로 바꿀 수 있습니다**. 서버에서 가져온 도시 목록에서 선택할 수 있습니다.
5. `GPS 근처 도시 찾기`는 주변 약 500m 정류장을 조회하고 가까운 정류장의 도시코드를 선택합니다. 이는 **주변에 실제 있는 모든 버스노선을 자동 추천하는 기능이 아니라 도시 선택 보조**입니다.
6. 노선번호 검색 → 공식 노선 선택 → 공식 경유 정류장 선택 → 저장.
7. 저장 시 서버에서 같은 `노선 ID + 정류장 ID + 정류장 순번`을 다시 검사합니다.
8. 홈에서 제공처별 검증 표시와 저장 항목을 확인합니다.

### 중요한 검색 범위 주의사항

TAGO의 `getRouteNoList`는 도시코드를 요구합니다. **실제 승차 위치와 노선 등록 지역이 다를 수 있습니다.** 구리에서 노선이 안 나오면 남양주 같은 다른 도시로 전환해 다시 검색하세요. GPS 주변 정류소 API 역시 전국 모든 노선번호 검색이나 해당 정류소의 전 노선 목록을 보장하지 않습니다. v0.3에서는 이를 혼동하지 않도록 도시를 직접 바꿀 수 있습니다.

## 3. 사용 API

- TAGO 도시 목록: `BusRouteInfoInqireService/getCtyCodeList`
- TAGO 노선번호 검색: `BusRouteInfoInqireService/getRouteNoList`
- TAGO 경유 정류장: `BusRouteInfoInqireService/getRouteAcctoThrghSttnList`
- TAGO 주변 정류장: `BusSttnInfoInqireService/getCrdntPrxmtSttnList`
- 기존 GBIS `GbisApi`의 노선번호·경유 정류장 조회는 변경하지 않음
- 응답은 XML로 파싱하고 `totalCount`에 따라 페이지를 조회합니다. 서비스 오류는 빈 목록으로 바꾸지 않습니다.

참고: [국토교통부 TAGO API 소개](https://tago.go.kr/v5/use/openapi.jsp), [공공데이터포털](https://www.data.go.kr/)

## 4. 기존 데이터 호환

- `DataStore`의 기존 `favorites_v1` 키/JSON 구조를 그대로 사용합니다.
- v0.2에서 GBIS로 저장한 항목은 `provider=GBIS`가 기본으로 복원됩니다.
- v0.3부터 `provider`, `officialCityCode`가 추가 저장됩니다.
- 동일 노선 ID여도 `provider + cityCode + routeId + stationId + stationSeq`가 다르면 별도 항목으로 저장합니다.
- 같은 applicationId 및 서명키로 업데이트 설치 시 즐겨찾기를 유지하도록 설계했습니다. 앱을 제거하면 데이터가 사라질 수 있습니다.

## 5. 아직 미지원

- 자동으로 주변 정류장에 정차하는 모든 노선을 추천하는 기능
- 버스 실시간 도착시간 및 지하철 실시간 도착시간
- 사용자 위치 자동 새로고침
- 서울 742번 등 지역별 제공 범위가 다를 수 있으므로 실제 검색 결과 확인 필요
- API 키 없이 실제 TAGO 서버를 호출한 실기기 검증

## 6. 테스트

`app/src/test/.../TagoXmlParserTest.kt`는 TAGO XML 정상/빈 응답/인증 오류를 테스트하고, 기존 `ValidateRouteStopUseCaseTest`는 정류장 순번과 오류 처리를 검증합니다.

실제 앱: Android Studio → **Sync → Run** 후 9/51/1-1 버스번호로 조회하세요. 서버 오류 발생 시 키를 공유하지 말고 **오류 코드/메시지**만 전달하면 됩니다.

※ 전체 Android APK 빌드 및 인증키로 실서버 호출은 개발 환경에서 검증하지 못했습니다.

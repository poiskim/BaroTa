# 바로타 v0.2 아키텍처

**Single Activity + Jetpack Compose + MVVM + Repository + 얇은 Domain UseCase + 수동 DI** (단일 :app Gradle 모듈)

```text
MainActivity (HomeViewModel + SearchViewModel, 화면 라우팅)
├─ HomeScreen/FavoriteCard -> HomeViewModel -> FavoritesRepository
│                                         -> RankFavoritesUseCase
│                                         -> LocationRepository
│                                         -> ArrivalRepository (아직 NotConnected)
└─ SearchScreen -> SearchViewModel -> TransitCatalogRepository (interface)
                                      └─ GbisTransitCatalogRepository (implementation)
                                          ├─ GbisApi (HTTP)
                                          └─ GbisXmlParser (XML -> DTO)
                                 -> ValidateRouteStopUseCase
                                 -> FavoritesRepository -> DataStore
```

**의존성 방향:** presentation → domain interfaces/usecase ← data implementations. AppContainer가 연결.

## 저장 흐름

1. SearchScreen에서 번호 검색 → SearchViewModel.searchRoutes → TransitCatalogRepository.searchBusRoutes
2. GbisApi에서 공식 REST URL 호출. Parser가 DTO 변환. Repo가 domain.BusRoute로 매핑
3. 노선 선택 → 경유정류장 리스트 수신 → 공식 ID/순번/지역/좌표 표시
4. 정류장 선택 후 저장 → ValidateRouteStopUseCase가 **새 목록 조회**하여 `(routeId, stationId, stationSeq)` 검증
5. 검증 통과 시 SearchViewModel이 TransitFavorite 생성 → FavoritesRepository.save → DataStore.
6. HomeViewModel은 DataStore Flow로 자동 업데이트 → RankFavoritesUseCase로 가까운 경로 추천

## 책임별 파일

| 위치 | 역할 |
|---|---|
| `domain/model/BusRoute.kt`, `RouteStop.kt` | provider에 독립적인 도메인 모델 |
| `domain/repository/TransitCatalogRepository.kt` | 검색·경유조회 계약 |
| `domain/usecase/ValidateRouteStopUseCase.kt` | 저장 직전 실제 정차 검증 |
| `data/remote/api/GbisApi.kt` | 인증키·URL·HTTP timeout·에러 처리 |
| `data/remote/dto/GbisXmlParser.kt` | GBIS XML 응답/오류 해석 |
| `data/repository/GbisTransitCatalogRepository.kt` | DTO → Domain 변환, 결과 정렬 |
| `presentation/search/SearchViewModel.kt` | 검색, 선택, 저장 상태 (StateFlow) |
| `presentation/search/SearchScreen.kt` | 순수 화면, 이벤트 콜백 |
| `data/local/FavoritesLocalDataSource.kt` | v0.1 JSON 기반 호환 저장/로딩 |
| `di/AppContainer.kt` | 의존성 조립 |

## 추후 확장

- 실시간 도착정보: `ArrivalRepository`의 인터페이스 유지하며 `GbisArrivalRepository` 구현체 연결
- 전체 정류장 키워드 검색: TransitCatalogRepository에 `searchBusStops` 계약 추가
- 서울/타지역: provider 별로 Repository 구현 추가, 선택 우선순위는 별도 UseCase 분리
- 저장 데이터 증가 시 DataStore → Room 변경 (Repository 계약 보존)
- 서버 프록시: GbisApi의 실제 네트워크 구현만 교체, UI/Domain 그대로 유지

## 중요한 검증 한계

- **API 응답이 도착한 시점의 정류장 경유 목록**을 기준으로 검증. 미래에 노선 경유 변경 가능.
- 방향은 다음 정류장 이름 기반; 같은 정류장명이 반복될 수 있어 `(stationId, stationSeq)` 조합 저장.
- v0.1 즐겨찾기에는 공식 ID가 없으므로 자동으로 `검증됨` 표시하지 않음.
- 코드는 샘플 XML 기반 단위 테스트로 구조를 검증할 수 있지만, 라이브 API와 기기 동작은 개인 키가 있는 실제 환경에서 점검 필요.


## v0.3 — TAGO / GBIS 병행 아키텍처

**사용자 화면 → SearchViewModel → TransitCatalogRepository / TagoTransitCatalog → API + Parser** 구조는 유지합니다.

- `domain/model/TransitProvider.kt`: `TAGO` 및 `GBIS`를 식별합니다.
- `domain/model/BusRoute.kt`: 제공처와 도시코드를 추가합니다. 동일 노선번호를 고유 노선으로 잘못 취급하지 않습니다.
- `domain/repository/TransitCatalogRepository.kt`: 두 공급자가 구현하는 노선/경유정류장 공통 계약입니다. 선택적 도시코드를 지원합니다.
- `domain/repository/TagoTransitCatalog.kt`: TAGO만 제공하는 도시목록/GPS 근처 정류장 조회를 추가한 계약입니다.
- `data/remote/api/TagoApi.kt`: TAGO HTTP 통신·키 인코딩·타임아웃.
- `data/remote/dto/TagoXmlParser.kt`: XML 정규화·페이징 헤더·인증 오류 검사.
- `data/repository/TagoTransitCatalogRepository.kt`: TAGO 행을 `BusRoute`, `RouteStop`, `TransitCity`, `NearbyTransitStop`으로 변환.
- `presentation/search/SearchViewModel.kt`: 선택된 제공처를 분기하고 도시 및 GPS 주변 정류장으로 도시코드를 고릅니다.
- `domain/usecase/ValidateRouteStopUseCase.kt`: 제공처별 Repository로 노선+정류장+순번을 다시 검증합니다.
- `data/local/FavoritesLocalDataSource.kt`: 기존 JSON에 `provider`, `officialCityCode` 추가; 이전 레코드 기본은 GBIS.
- `data/repository/DefaultFavoritesRepository.kt`: 중복 판단에 제공처+도시코드도 포함.
- `di/AppContainer.kt`: Hilt 도입 없이 TAGO/GBIS 두 Repo를 생성하여 주입합니다.

주의: TAGO는 **노선 조회 시 도시코드 필수**입니다. GPS 주변 정류소 API는 도시 후보 선택에만 쓰고, 실제 노선 경유 및 방향은 선택한 노선의 경유정류장 목록으로 검증합니다. 사용자가 인접 도시 코드를 선택할 수 있습니다.

본 버전은 검색·등록용이고, 실시간 도착정보와 모든 노선 자동 추천은 범위 밖입니다.

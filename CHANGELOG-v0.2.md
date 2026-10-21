# BaroTa v0.2 변경 사항

## 추가

- `domain/model/BusRoute.kt` — 공식 노선 모델
- `domain/model/RouteStop.kt` — 경유 정류장, 방향 계산
- `domain/repository/TransitCatalogRepository.kt` — 카탈로그 계약
- `domain/usecase/ValidateRouteStopUseCase.kt` — 공식 경유 검증
- `data/remote/api/GbisApi.kt` — HTTPS GBIS v2 API
- `data/remote/dto/GbisXmlParser.kt` — GBIS XML → DTO
- `data/repository/GbisTransitCatalogRepository.kt` — DTO → Domain
- `presentation/search/SearchViewModel.kt` — 화면 상태 및 검색·저장
- `app/src/test/.../ValidateRouteStopUseCaseTest.kt`, `GbisXmlParserTest.kt` — 단위 테스트
- `local.properties.example` — 키 설정 양식 (개인 키는 포함하지 않음)

## 수정

- `app/build.gradle.kts` — BuildConfig 키 주입 / version 0.2.0
- `AndroidManifest.xml` — 인터넷 권한
- `MainActivity.kt` — SearchViewModel 의존성 주입 연결
- `presentation/search/SearchScreen.kt` — 공식 노선·경유 정류장 UI
- `presentation/home/HomeScreen.kt`, `presentation/components/FavoriteCard.kt` — 사용 문구/검증 상태
- `domain/model/TransitFavorite.kt` — 공식 식별자 필드 추가
- `data/local/FavoritesLocalDataSource.kt` — 새 저장 필드, 구버전 JSON 호환
- `data/repository/DefaultFavoritesRepository.kt` — 공식 경유정류장 중복 방지
- `di/AppContainer.kt` — GbisApi/Repo/UseCase 연결
- `.gitignore` — API 키 보호

**미구현:** 실시간 도착, 지하철 공식 역 검증, 로컬 키 없는 라이브 조회.

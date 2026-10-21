# 바로타 Android v0.2 — GBIS 실제 노선·정류장 검증

기존 v0.1을 기반으로 **경기버스정보(GBIS) 공식 노선·정류장 검색과 검증**을 구현한 Android Studio Kotlin + Jetpack Compose 프로젝트입니다.

## 가장 먼저: 개인 API 키 설정 (필수)

별도 인증키가 없으면 실제 노선/정류장을 조회할 수 없습니다. 테스트용 가짜 정류장이나 도착시간을 대신 띄우지 않습니다.

1. [경기버스정보 공개키 발급 안내](https://www.gbis.go.kr/gbis2014/publicService.action?cmd=publickKey)에서 공공데이터포털의 **경기도 버스노선 조회 서비스** 활용신청/키 발급 절차를 확인합니다.
2. Android Studio에서 프로젝트 루트 `BaroTaAndroid/local.properties` 파일을 엽니다. 보통 Android Studio가 `sdk.dir=...` 줄이 포함된 파일을 자동 생성합니다.
3. 맨 아래에 아래 한 줄을 추가합니다. **`sdk.dir`을 지우지 마세요.**

   ```properties
   GBIS_SERVICE_KEY=발급받은_일반_인증키
   ```

   - 포털이 제공하는 **일반(Decoding) 인증키** 사용을 권장합니다. 인코딩 키도 지원하도록 처리했습니다.
   - 개인 API 키는 타인에게 공유하거나 공개 저장소에 업로드하지 마세요. `local.properties`는 `.gitignore`에 포함했습니다.
   - 이미 키가 반영된 APK는 앱을 뜯으면 키가 추출될 수 있습니다. **이 방식은 개인 개발용**이며, 공개 배포 시 서버 프록시를 별도로 구축하세요.

4. **Sync Project with Gradle Files → Run** (키 추가 후에는 다시 빌드해야 합니다).
5. 홈 화면에서 `9번` 또는 `51번` 등을 누르거나 검색 탭에서 번호를 입력해 공식 노선을 검색합니다.
6. 검색 결과에서 **지역·기점·종점**을 확인해 맞는 노선을 선택합니다.
7. 해당 노선의 경유 정류장을 이름/정류장번호로 검색해 터치 → **공식 정류장 검증 후 즐겨찾기에 추가**.
8. 홈에서 `✓ GBIS 공식 정류장 확인`이 표시되는지 확인합니다.

### Android Studio에서 처음 열 때

- ZIP 압축을 풀어 `BaroTaAndroid` 폴더를 **Open**으로 엽니다.
- 기존 프로젝트를 열어 실행했다면 **동일한 applicationId `com.barota.app`**를 유지했기 때문에 업데이트 설치 시 DataStore 즐겨찾기를 유지하도록 설계했습니다. APK 서명키가 달라 업데이트되지 않는 경우 재설치가 필요할 수 있고, 재설치 시 데이터가 사라질 수 있습니다.
- 압축본에는 `gradle-wrapper.jar`가 들어있지 않습니다. Windows: `setup-wrapper.bat`를 실행해 검증된 Wrapper JAR 설치. Mac/Linux: `./setup-wrapper.sh`. 해당 스크립트는 공식 SHA-256을 검증합니다. 기존 v0.1에서 이미 설정되어 있으면 다시 설치할 필요는 없습니다.
- JDK 17, Android SDK 36 필요할 수 있음. Gradle 8.13, AGP 8.13.2.
- `local.properties`가 없다면 Android Studio가 만든 파일에 키만 추가합니다.

## 이번 버전 동작

- **실제 노선 검색:** GBIS 공식 `getBusRouteListv2?keyword=<번호>&format=xml` 호출
- **경유 정류장 검색:** 공식 `getBusRouteStationListv2?routeId=<ID>&format=xml` 호출
- **저장 시 재검증:** 서버 목록에서 `(routeId, stationId, stationSeq)` 일치 확인; 서버 오류는 미존재와 구분
- **고유 저장:** 공식 노선 ID, 정류장 ID, 정류장 순번, 이름, 지역, 공식 좌표 보관
- **방향 표기:** 정류장 **다음 경유 정류장 이름**으로 `OO 방면` 표시. 노선의 최종 행선지로 오해하지 말 것
- **위치 추천:** 공식 좌표가 있는 버스는 휴대폰의 저장 당시 GPS가 아니라 **공식 정류장 위치**와 현재 GPS의 거리를 계산
- **기존 데이터:** v0.1 DataStore `favorites_v1` 포맷을 추가 필드로 확장. 이전 기록은 삭제/강제 검증 없이 `미검증`으로 보존
- **지하철:** 기존처럼 수동 입력 가능. 아직 미검증 상태
- **중복 저장:** 동일 `(routeId, stationId, stationSeq)`는 중복 항목 대신 기존 고정/조회수 보존하면서 갱신

## 아직 안 되는 것

- **실시간 버스·지하철 도착시간** (다음 단계)
- 정류장 이름만으로 전체 지역을 검색하는 독립 검색 (현재는 **버스 노선을 먼저 선택 → 그 노선 경유 정류장 필터링**)
- 실시간 GPS 자동 갱신 (홈에서 권한 허용 후 새로고침)
- 공식 API 접근 승인/인증키 발급을 앱이 대신 수행하지 않음
- 실제 GBIS 라이브 서버 호출 및 갤럭시 APK 빌드는 이 작업 환경에서 검증하지 못함. **키를 설정한 실기기에서 네트워크 실험 필요**

## 문제 해결

| 메시지 | 점검 |
|---|---|
| `GBIS 서비스키가 없어요` | local.properties에 `GBIS_SERVICE_KEY` 추가 후 **Rebuild** |
| `HTTP 401 / 403` | 키 오타, 활용신청/승인 여부, 포털 서비스 종류 확인 |
| `GBIS 요청 오류` | resultCode 및 서버 메시지를 확인. 키/할당량/서버 상태 검토 |
| `네트워크 오류` | 갤럭시 인터넷 연결과 데이터 제공 서버 확인 |
| `검색된 노선이 없어요` | 같은 번호가 없을 수도 있음. 9/51/1-1처럼 번호만 입력 |
| 위치 추천이 비정상 | 권한 허용 및 홈 새로고침. 공식 응답 좌표가 없으면 거리 추천에서 제외 |

## 관련 공식 명세

- [노선번호 목록조회](https://www.gbis.go.kr/gbis2014/publicService.action?cmd=mBusRoute)
- [경유정류소 목록조회](https://www.gbis.go.kr/gbis2014/publicService.action?cmd=mBusRouteStation)
- [GBIS 공개키 발급](https://www.gbis.go.kr/gbis2014/publicService.action?cmd=publickKey)

구조/변경 파일은 `ARCHITECTURE.md`, `CHANGELOG-v0.2.md` 참고.

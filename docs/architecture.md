# 아키텍처

## 전체 그림
```
┌──────────── Galaxy Fold ────────────┐        ┌──── Galaxy Watch ────┐
│ feature:* (Compose 화면)             │        │ wear (Wear Compose)  │
│   ↓ ViewModel / StateFlow           │ Data   │  상태 보기 + 완료 탭  │
│ core:data  Room ─ Repository ─ Sync │◀─Layer─▶│                      │
│   ↓ GameEngine (core:game)          │        └──────────────────────┘
└──────────────┬──────────────────────┘
               │ HTTPS (Tailscale serve)   X-Api-Key
┌──────────────▼──────────────────────┐
│ Windows PC: Ktor 서버 + PostgreSQL   │
│   같은 core:game 으로 검산 (/profile) │
└─────────────────────────────────────┘
```

## 모듈과 의존 방향
```
app ─┬─ feature:* ─┬─ core:designsystem ── core:game
     │             └─ core:data ─────────── core:protocol ── core:game
     └─ core:data
wear ── core:protocol ── core:game
server ── core:protocol ── core:game
```
| 모듈 | 종류 | 책임 |
|---|---|---|
| core:game | 순수 Kotlin | 규칙·엔진. 안드로이드 의존 0 → JVM 테스트, 서버에서도 그대로 사용 |
| core:protocol | 순수 Kotlin | 폰↔서버, 폰↔워치 데이터 형식 (kotlinx.serialization) |
| core:data | Android 라이브러리 | Room, Repository, 동기화(WorkManager + OkHttp) |
| core:designsystem | Android 라이브러리 | 테마·컴포넌트·연출·효과음 |
| feature:* | Android 라이브러리 | 화면 1개 = 모듈 1개, 서로 모름 |
| app | Android 앱 | 조립(DI), 내비게이션, 폴드 레이아웃, 워치 송수신 |
| wear | Wear OS 앱 | 폰이 보낸 요약 표시 + 완료 요청 |
| server | Ktor (JVM) | 동기화 저장소 |

빌드 설정 중복은 `build-logic` 의 convention plugin 5개로 없앴다
(`lifereset.android.application / library / compose / feature`, `lifereset.jvm.library`).
라이브러리 버전은 `gradle/libs.versions.toml` 한 곳.

---

## 설계 결정 (ADR)

### ADR-1. 멀티모듈
- **결정**: 기능·계층별로 모듈을 나눈다.
- **이유**: 오래 쓰면서 계속 고칠 앱. 게임 규칙을 화면과 분리해야 테스트·서버 재사용이 가능하고, 화면끼리 엉키지 않는다.
- **대가**: 처음 보는 사람에겐 폴더가 많아 보인다 (iOS 의 로컬 Swift Package 여러 개와 같은 구조).

### ADR-2. 이벤트 소싱 — 완료 기록만 저장하고 상태는 재계산
- **결정**: DB 에는 퀘스트 정의와 완료 기록만. 레벨·스탯·클래스·연속은 `GameEngine` 이 매번 재생해서 계산.
- **이유**
  - 밸런스(EXP 곡선 등)를 바꿔도 과거 기록과 모순이 생기지 않는다.
  - 폰·서버가 같은 엔진을 쓰므로 계산 결과가 항상 같다 (`/profile` 로 검산).
  - 동기화 대상이 단순해진다 (추가만 되는 기록 + 몇 개의 정의).
- **대가**: 매번 처음부터 계산. 66일 × 하루 10건 수준이라 수 ms → 문제없음. 몇 년 쌓이면 시즌 단위 스냅샷으로 최적화 예정.

### ADR-3. 연출은 상태 비교로 만든다 (FxDiff)
- **결정**: 완료 전 상태와 후 상태를 비교해 `LevelUp`, `RankUp`, `ClassChange`, `BossDefeated` … 사건을 만든다.
- **이유**: 엔진은 순수 함수로 두고, "무슨 일이 일어났는지"는 화면 쪽 관심사로 분리. 새 연출 = 사건 하나 + `FxHost` 에 한 줄.

### ADR-4. 오프라인 우선 + 최종 수정 우선(LWW) 동기화
- **결정**: 모든 쓰기는 Room 에 먼저, `dirty` 표시. WorkManager 가 5초 뒤(+15분 주기) push → pull.
  충돌은 `updatedAt` 이 큰 쪽이 이긴다. 삭제는 `voided` 표시(soft delete).
- **이유**: 혼자 쓰는 앱이라 동시 편집 충돌이 거의 없고, 네트워크가 없어도 앱은 항상 동작해야 한다.
- **pull 커서**: 서버가 받은 시각 `serverUpdatedAt >= since`. `>` 가 아니라 `>=` 인 이유는 같은 밀리초에 들어온 행을 놓치지 않기 위해 (중복 수신은 LWW 라 무해).

### ADR-5. 서버는 내 PC + Tailscale
- **결정**: 클라우드 대신 24시간 켜진 Windows PC. 외부 접속은 Tailscale 사설망, `tailscale serve` 로 HTTPS.
- **이유**: 비용 0, 데이터가 내 손에. 인터넷에 포트를 열지 않는다.
- **인증**: v1 은 API 키 (`X-Api-Key`, 상수 시간 비교). → v1.1 에서 Google 로그인으로 교체 예정.

### ADR-6. 수동 DI (DataContainer)
- **결정**: Hilt 없이 `DataContainer` 한 파일에서 객체를 만든다.
- **이유**: 객체가 10개 남짓. 무엇이 무엇에 연결되는지 한 파일에서 보이는 게 유지보수에 더 쉽다.

### ADR-7. 워치는 "화면 + 리모컨"
- **결정**: 워치는 계산·저장을 하지 않는다. 폰이 `DataItem` 으로 요약을 보내고, 워치는 `MessageClient` 로 완료 요청만.
- **이유**: 규칙이 한 곳(폰)에만 있으면 버그가 한 곳에만 생긴다. 워치가 꺼져 있어도 DataItem 은 다음에 전달된다.
- **조건**: 폰·워치 `applicationId` 와 서명 키가 같아야 통신된다.

### ADR-8. 폴드 대응은 폭 기준 (600dp)
- **결정**: 폭 < 600dp → 아래 원형 메뉴 1화면, ≥ 600dp → 왼쪽 세로 메뉴 + 2화면 (오늘|상태, 상태|기록, 기록|상태, 설정|퀘스트).
- **이유**: 접힘/펼침 상태 API 대신 실제 사용 가능한 폭만 보면 커버 화면, 펼친 화면, 분할 화면 모두 자연스럽게 대응.

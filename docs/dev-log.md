# 개발 기록 · 트러블슈팅

실제로 부딪힌 문제와 원인, 해결, 배운 점. (관련 이슈 번호는 GitHub Issues 참고)

---

## 1. Gradle 동기화 실패: Incompatible Gradle JVM version
**증상**
```
The project's Gradle version 8.11.1 is incompatible with the Gradle JVM version 25
currently selected to run Gradle build. Gradle 8.11.1 supports Java versions between 1.8 and 23.
```
**원인** Android Studio 에 들어 있는 JDK 가 25 였고, Gradle 8.11 은 Java 23 까지만 지원한다.
"Gradle 을 실행하는 JDK"(Gradle JDK)와 "코드를 컴파일하는 대상 버전"(jvmTarget 17)은 별개다.

**해결** Settings → Build Tools → Gradle → **Gradle JDK** 에서 Download JDK → 21 (JetBrains Runtime) 선택.
이 설정은 프로젝트별(`.idea/gradle.xml`)이라 다른 프로젝트에 영향 없음.

**배운 점** 버전 호환표 확인 순서: AGP ↔ Gradle ↔ JDK. AGP 8.7 은 Gradle 8.9+ 필요, Gradle 8.11 은 JDK ≤ 23.

---

## 2. 컴파일 에러: Unresolved reference 'RectangleShape'
**증상** `core:designsystem:compileDebugKotlin` 에서 `Inputs.kt:15`
**원인** `RectangleShape` 를 `androidx.compose.foundation.shape` 에서 import 했지만 실제 위치는 `androidx.compose.ui.graphics`.
(`CircleShape`, `CutCornerShape` 는 foundation.shape 에 있어서 같은 곳이라고 착각)

**해결**
```diff
- import androidx.compose.foundation.shape.RectangleShape
+ import androidx.compose.ui.graphics.RectangleShape
```
**배운 점** Shape 인터페이스와 가장 기본인 RectangleShape 는 ui-graphics, 모양 구현체(Rounded/Cut/Circle)는 foundation.

---

## 3. 컴파일 에러: AnimatedVisibility 를 이 위치에서 호출할 수 없음
**증상** `LevelUpOverlay.kt` 에서 에러 2개.
**원인** `Column { Row { Box { AnimatedVisibility(...) } } }` 구조였는데,
`AnimatedVisibility` 는 `ColumnScope.AnimatedVisibility`, `RowScope.AnimatedVisibility`, 일반 함수 버전이 따로 있다.
Box 안에서는 바깥 Row·Column 스코프가 둘 다 암시적 수신자로 잡혀서 **어느 확장 함수를 쓸지 모호**해진다.

**해결** 이 경우는 단순히 "나타나기"만 필요했기 때문에 `animateFloatAsState` + `graphicsLayer { alpha; translationY }` 로 바꿨다.
(다른 방법: `androidx.compose.animation.AnimatedVisibility(...)` 처럼 패키지 전체 이름으로 일반 함수를 호출)

**배운 점** Compose 의 스코프 확장 함수(`RowScope.weight`, `ColumnScope.AnimatedVisibility` …)는 레이아웃이 중첩될 때 모호해질 수 있다.

---

## 4. 폰에 설치 차단: "보안 위험 자동 차단에서 차단됨"
**원인** 삼성 One UI 의 **보안 위험 자동 차단(Auto Blocker)** 이 USB 를 통한 앱 설치·명령을 막는다.
**해결** 설정 → 보안 및 개인정보 보호 → 보안 위험 자동 차단 → 끄기 → USB 재연결 → 디버깅 허용.
설치 후 다시 켜도 이미 설치된 앱은 정상 동작. 단, 새로 빌드해서 설치할 때마다 다시 꺼야 한다.

---

## 5. 서버 실행: 'java' is not recognized
**증상** `start-server.bat` 실행 → `'java' is not recognized as an internal or external command` 후 10초마다 재시도
**원인** PC 에 있던 Java 는 Android Studio 안의 JBR 뿐이었고, 시스템 PATH 에 등록돼 있지 않았다.
(Android Studio 는 자기 JDK 를 직접 지정해서 쓰기 때문에 IDE 에서는 문제가 드러나지 않음)
**해결** `winget install EclipseAdoptium.Temurin.21.JDK` → 새 cmd 에서 `java -version` 확인 → bat 재실행
**결과** `Responding at http://127.0.0.1:8080` → Tailscale serve → 폰에서 `/health`, `/sync/push`, `/sync/pull` 모두 200 OK
**배운 점** 24시간 돌릴 서버는 IDE 에 딸린 런타임이 아니라 따로 설치한 LTS 런타임으로 돌린다 (IDE 업데이트에 끌려가지 않게).

---

## 6. Google 로그인: "서버에 허용되지 않은 계정입니다"
**상황** 폰에서 계정 선택 → 서버 로그인 단계에서 403
**원인** Google 쪽(Android 클라이언트 SHA-1, 웹 클라이언트 ID, 토큰 검증)은 모두 통과했고,
마지막 허용 이메일 목록(`LIFE_RESET_ALLOWED_EMAILS`)에 실제로 고른 계정이 들어 있지 않았다.
**해결** bat 의 허용 이메일을 앱에 표시된 계정으로 고치고 **서버를 재시작** (환경 변수는 시작할 때 한 번만 읽음)
**배운 점** 오류 메시지를 단계별로 나눠 둔 덕분에(401 토큰 문제 / 403 허용 목록) 어디서 막혔는지 바로 알 수 있었다.
설정값을 바꾼 뒤에는 그 값을 읽는 프로세스를 다시 띄워야 한다.

---

## 7. 설계·구현 중 고친 것들
| 버그 | 원인 | 해결 |
|---|---|---|
| 운동이 적은 주에 주간 보스 HP 가 맞지 않음 | HP 를 항상 "운동 횟수 − 1" 로 계산 | 운동 3회 이상일 때만 −1 (여유 1회), 그보다 적으면 횟수 그대로 |
| 같은 밀리초에 들어온 행을 pull 에서 놓침 | 커서를 `>` 로 비교 | `>=` 로 바꾸고 중복은 LWW 로 무해하게 |
| 서버에서 복원해도 폰에서 새로 만든 기본 퀘스트가 이김 | 복원도 LWW 로 합쳤기 때문 | 복원은 강제 덮어쓰기(force) |
| `Duration.toMinutesPart()` 크래시 가능 | API 31+ 전용 (minSdk 26) | 초 단위로 직접 계산 |
| 레벨 1~4 에도 클래스가 붙음 | 최고 스탯으로만 판정 | 레벨 5 미만은 견습생, 올라운더는 최소값 + 편차 조건 |

`core:game` 테스트 22개가 이 규칙들을 지킨다: `./gradlew :core:game:test`

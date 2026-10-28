# Google 로그인 설정 (한 번만)

앱은 Google 계정으로 로그인하고, 서버는 그 계정별로 기록을 저장한다.
Google 에 "이 앱이 로그인 버튼을 써도 된다"고 등록하는 과정이 필요하다. 15분 정도.

```
[폰 앱] --계정 선택--> Google --ID 토큰--> [폰 앱] --토큰--> [PC 서버] --검증--> Google 공개키
                                                    <--세션 토큰--
```

## 1. Google Cloud 프로젝트 만들기
1. https://console.cloud.google.com 접속 (본인 Gmail 로 로그인).
2. 위쪽 프로젝트 선택 → **새 프로젝트** → 이름 `Life Reset` → 만들기.

## 2. 동의 화면 (Google 인증 플랫폼)
1. 왼쪽 메뉴 **API 및 서비스 → OAuth 동의 화면** (또는 "Google 인증 플랫폼").
2. **시작하기** →
   - 앱 이름: `Life Reset`
   - 사용자 지원 이메일: 본인 Gmail
   - 대상: **외부**
   - 연락처 이메일: 본인 Gmail → 만들기
3. **대상(Audience)** 메뉴 → **테스트 사용자 → Add users** → 본인 Gmail 추가.
   (게시하지 않은 "테스트" 상태로 두면 여기 넣은 계정만 로그인된다. 혼자 쓰기엔 이게 가장 안전.)

## 3. 웹 클라이언트 만들기 → 서버에 넣을 값
1. **클라이언트(사용자 인증 정보) → 클라이언트 만들기**.
2. 애플리케이션 유형: **웹 애플리케이션**, 이름 `Life Reset Server`. 리디렉션 URI 는 비워 둔다.
3. 만들어진 **클라이언트 ID** (`123456-abc….apps.googleusercontent.com`) 복사.
4. PC 의 `C:\life-reset\start-server.bat` 에:
   ```
   set LIFE_RESET_GOOGLE_CLIENT_ID=복사한-클라이언트-ID
   set LIFE_RESET_ALLOWED_EMAILS=본인@gmail.com
   ```
   서버를 다시 켠다. 앱은 이 ID 를 서버에서 받아가므로 앱에는 따로 넣지 않는다.

## 4. Android 클라이언트 만들기 → 앱을 Google 에 등록
1. 다시 **클라이언트 만들기** → 유형 **Android**.
2. 패키지 이름: `com.lifereset.app`
3. SHA-1 인증서 지문: Android Studio 아래 **Terminal** 에서
   ```
   gradlew signingReport
   ```
   출력에서 `Variant: debug` 의 `SHA1: AA:BB:…` 를 복사해 붙여넣는다.
   (이 프로젝트는 release 도 debug 키로 서명하므로 하나만 등록하면 된다.
   다른 PC 에서 빌드하면 SHA-1 이 달라지니 그 PC 것도 추가로 등록.)
4. 만들기. 이 클라이언트 ID 는 어디에도 넣지 않는다 (등록만 돼 있으면 됨).

## 5. 앱에서 로그인
1. 앱 → 설정 → ACCOUNT · SERVER
2. 서버 주소 입력 → **주소 저장하고 연결 확인** → "연결됨"
3. **Google 계정으로 로그인** → 계정 선택
4. "…로 로그인했습니다" 가 나오면 끝. 이 폰의 기록이 서버의 내 계정에 올라간다.

새 폰에서는 같은 계정으로 로그인만 하면 서버 기록과 합쳐진다.
(게임 시작일은 둘 중 더 이른 날을 쓰고, 같은 기록은 마지막에 고친 쪽이 남는다.)

## 문제 해결
| 메시지 | 원인 / 해결 |
|---|---|
| `Developer console is not set up correctly` / `[28444]` / `[10]` | 4단계 패키지 이름 또는 SHA-1 이 다름. `gradlew signingReport` 값을 다시 확인 |
| `No credentials available` | 폰에 Google 계정이 없거나, 2-3 의 테스트 사용자에 그 계정이 없음 |
| 서버 로그인 실패: 허용되지 않은 계정 | `LIFE_RESET_ALLOWED_EMAILS` 에 그 Gmail 추가 후 서버 재시작 |
| 서버 로그인 실패: Google 토큰을 확인할 수 없습니다 | 서버의 `LIFE_RESET_GOOGLE_CLIENT_ID` 가 3단계 **웹** 클라이언트 ID 인지 확인 (Android 것 아님). PC 가 인터넷에 연결돼 있어야 함 |
| 로그인이 만료되었습니다 | 서버 DB 를 지웠거나 로그아웃됨. 다시 로그인 |

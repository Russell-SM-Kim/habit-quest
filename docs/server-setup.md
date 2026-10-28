# Windows PC 서버 설치

PC 는 24시간 켜져 있다고 가정. 순서대로 한 번만 하면 된다.

## 1. Java 17
- [Adoptium Temurin 17](https://adoptium.net) 설치 (MSI, "Set JAVA_HOME" 체크).
- 확인: `java -version`

## 2. PostgreSQL
- [PostgreSQL 16 Windows 설치 파일](https://www.postgresql.org/download/windows/) 실행. 슈퍼유저(postgres) 비밀번호를 기억.
- 포트 5432 기본값 그대로.

## 3. DB 와 사용자 만들기
시작 메뉴 → **SQL Shell (psql)** → postgres 로 로그인 후:

```sql
CREATE USER lifereset WITH PASSWORD '여기에-DB-비밀번호';
CREATE DATABASE lifereset OWNER lifereset;
```

테이블은 서버가 처음 켜질 때 자동으로 만든다.

## 4. 서버 jar 만들기
Android Studio 터미널(또는 cmd)에서 프로젝트 폴더로 이동:

```
gradlew.bat -PserverOnly=true :server:buildFatJar
```

결과: `server\build\libs\life-reset-server.jar`

## 5. 실행 폴더 만들기
```
C:\life-reset\
  life-reset-server.jar
  start-server.bat     (server\scripts\windows 에서 복사)
  backup-db.ps1        (server\scripts\windows 에서 복사)
```

`start-server.bat` 을 메모장으로 열어 채운다:
- `LIFE_RESET_GOOGLE_CLIENT_ID` : Google 웹 클라이언트 ID → [google-login-setup.md](google-login-setup.md) 1~3단계
- `LIFE_RESET_ALLOWED_EMAILS` : 로그인 허용할 Gmail (쉼표로 여러 개)
- `LIFE_RESET_DB_PASSWORD` : 3단계 비밀번호
- `LIFE_RESET_HOST` : 기본 `127.0.0.1` (7단계 tailscale serve 사용 시). 직접 접속하려면 `0.0.0.0`

더블클릭 → 브라우저에서 `http://localhost:8080/health` 가 `{"ok":true,...}` 면 성공.
서버가 죽으면 bat 이 10초 뒤 다시 켠다.

## 6. PC 켜질 때 자동 실행
작업 스케줄러 → 기본 작업 만들기
- 트리거: **로그온할 때**
- 동작: 프로그램 시작 → `C:\life-reset\start-server.bat`, 시작 위치 `C:\life-reset`
- 속성 → "가장 높은 수준의 권한으로 실행" 체크

## 7. Tailscale (밖에서 접속)
1. PC 와 폰 둘 다 [Tailscale](https://tailscale.com) 설치 → 같은 계정 로그인.
2. 관리 콘솔 → DNS → **MagicDNS** 와 **HTTPS Certificates** 켜기.
3. PC 의 관리자 cmd:
   ```
   tailscale serve --bg 8080
   ```
   출력되는 `https://PC이름.tailnet이름.ts.net` 이 서버 주소.
4. 폰 앱 → 설정 → ACCOUNT · SERVER → 주소 입력 → **주소 저장하고 연결 확인** → **Google 계정으로 로그인**.

Tailscale 망 안에서만 열리므로 인터넷에 공개되지 않는다. Google 로그인 + 허용 이메일이 한 번 더 막는다.

## 8. 백업
`backup-db.ps1` 의 `$PgBin`(PostgreSQL 설치 경로), 비밀번호를 고친 뒤 관리자 cmd:

```
schtasks /Create /SC DAILY /ST 03:00 /TN "LifeReset Backup" /TR "powershell -ExecutionPolicy Bypass -File C:\life-reset\backup-db.ps1"
```

`C:\life-reset\backups\` 에 매일 덤프, 30일 지난 것은 자동 삭제.
복원: `pg_restore -U lifereset -d lifereset --clean 파일.dump`

## 문제 해결
| 증상 | 확인 |
|---|---|
| 앱에서 "연결 실패" | PC 와 폰 Tailscale 둘 다 켜짐? `tailscale serve status` 에 8080 이 보이나? |
| 로그인 관련 오류 | [google-login-setup.md](google-login-setup.md) 의 문제 해결 표 |
| 서버 시작 시 "LIFE_RESET_GOOGLE_CLIENT_ID 에…" | bat 에 웹 클라이언트 ID 를 넣지 않음 |
| 서버가 바로 꺼짐 | bat 창의 오류: 대부분 DB 비밀번호 또는 PostgreSQL 서비스 꺼짐 |
| 새 폰으로 옮김 | 앱 설정 → 서버 주소 → 같은 Google 계정으로 로그인 (자동으로 합쳐짐) |
| 예전 버전(API 키 방식) 서버로 테이블을 이미 만들었음 | psql 에서 `DROP TABLE quests, completions, settings;` 후 서버 재시작 (사용자별 구조로 다시 만들어짐) |

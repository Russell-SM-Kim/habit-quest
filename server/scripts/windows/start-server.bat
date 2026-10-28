@echo off
REM ============================================================
REM  Life Reset 서버 실행 스크립트 (Windows)
REM  1) 아래 값들을 채운다
REM  2) 이 파일을 더블클릭하거나, 작업 스케줄러에 "로그온 시 실행"으로 등록한다
REM ============================================================
chcp 65001 > nul
cd /d %~dp0

REM Google 로그인: Google Cloud 콘솔에서 만든 "웹 애플리케이션" 클라이언트 ID (docs/google-login-setup.md)
set LIFE_RESET_GOOGLE_CLIENT_ID=여기에-숫자-xxxx.apps.googleusercontent.com

REM 로그인을 허용할 Gmail (쉼표로 여러 개). 친구를 추가하면 서버를 다시 켜야 적용된다.
REM 비우면 Google 계정이면 누구나 가입 (공개 가입 - 서버 시작 시 경고가 뜬다).
set LIFE_RESET_ALLOWED_EMAILS=본인@gmail.com

REM PostgreSQL 접속 정보 (docs/server-setup.md 의 3단계에서 만든 값)
set LIFE_RESET_DB_URL=jdbc:postgresql://localhost:5432/lifereset
set LIFE_RESET_DB_USER=lifereset
set LIFE_RESET_DB_PASSWORD=여기에-DB-비밀번호

REM 127.0.0.1 = 이 PC 안에서만 접속 가능. 밖(폰)에서는 tailscale serve 가 HTTPS 로 대신 받아 넘겨준다.
REM tailscale serve 를 안 쓰고 http://100.x.x.x:8080 으로 직접 붙을 때만 0.0.0.0 으로 바꾼다.
set LIFE_RESET_HOST=127.0.0.1
set LIFE_RESET_PORT=8080
set LIFE_RESET_ZONE=Asia/Seoul

REM life-reset-server.jar 를 이 폴더에 같이 둔다
:loop
java -jar life-reset-server.jar
echo 서버가 종료되었습니다. 10초 뒤 다시 시작합니다...
timeout /t 10 /nobreak > nul
goto loop

@echo off
REM ============================================================
REM  Life Reset 서버 실행 스크립트 (Windows)
REM  1) 아래 값들을 채운다
REM  2) 이 파일을 더블클릭하거나, 작업 스케줄러에 "로그온 시 실행"으로 등록한다
REM ============================================================
chcp 65001 > nul
cd /d %~dp0

REM 앱의 설정 화면에도 같은 키를 넣는다. 16자 이상 아무 문자열.
set LIFE_RESET_API_KEY=여기에-긴-비밀키를-넣으세요

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

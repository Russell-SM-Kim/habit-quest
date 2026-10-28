# Life Reset DB 매일 백업 (PowerShell)
# 작업 스케줄러에 "매일 03:00" 으로 등록한다. 30일 지난 백업은 지운다.
# 등록 예:
#   schtasks /Create /SC DAILY /ST 03:00 /TN "LifeReset Backup" /TR "powershell -ExecutionPolicy Bypass -File C:\life-reset\backup-db.ps1"

$ErrorActionPreference = "Stop"

$PgBin     = "C:\Program Files\PostgreSQL\18\bin"     # PostgreSQL 설치 경로에 맞게
$BackupDir = "C:\life-reset\backups"
$DbName    = "lifereset"
$DbUser    = "lifereset"
$env:PGPASSWORD = "여기에-DB-비밀번호"

New-Item -ItemType Directory -Force -Path $BackupDir | Out-Null
$stamp = Get-Date -Format "yyyyMMdd-HHmm"
$file  = Join-Path $BackupDir "lifereset-$stamp.dump"

& "$PgBin\pg_dump.exe" -U $DbUser -h localhost -F c -f $file $DbName
Write-Output "백업 완료: $file"

Get-ChildItem $BackupDir -Filter "lifereset-*.dump" |
    Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-30) } |
    Remove-Item

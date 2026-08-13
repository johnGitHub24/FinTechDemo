# FinTechDemo 一鍵啟動說明（可執行）
# 最小可跑（無 Docker）：只起 order-service + frontend
# 完整 Demo：compose（Kafka）+ risk + order(demo) + gateway + job + frontend

param(
  [switch]$WithInfra,
  [switch]$WithKafkaDemo
)

$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
Set-Location $Root

if ($WithInfra -or $WithKafkaDemo) {
  Write-Host "== docker compose up ==" -ForegroundColor Cyan
  docker compose up -d
}

Write-Host "== Build check ==" -ForegroundColor Cyan
.\gradlew.bat check
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host @"

啟動服務（請開多個終端）：

1) risk-service
   .\gradlew.bat :risk-service:bootRun

2) order-service
   預設（同步／可手動成交）:
   .\gradlew.bat :order-service:bootRun
   Kafka Demo:
   .\gradlew.bat :order-service:bootRun --args='--spring.profiles.active=demo'

3) gateway（可選）
   .\gradlew.bat :gateway:bootRun

4) job-service（可選）
   .\gradlew.bat :job-service:bootRun

5) frontend
   cd frontend; npm install; npm run dev

帳號: trader1 / admin
密碼: password
前端: http://localhost:5173

"@ -ForegroundColor Green

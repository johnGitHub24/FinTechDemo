@echo off
REM Loop Engineering 一鍵：補齊業務服務 + Vite + docs + Grafana/Prometheus + Locust。
REM 建議日常：IntelliJ 跑 OrderServiceApplication（就緒後自動 ensure，含觀測／壓測）。
REM 省 RAM：加參數 -SkipDocker -SkipLocust
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\ensure-demo-links.ps1" %*
if errorlevel 1 (
  echo.
  echo LOOP 未全綠。請看上方 FAIL，或跑: .\scripts\doctor-demo.ps1
  pause
)

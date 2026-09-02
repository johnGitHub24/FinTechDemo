@echo off
REM Stop local Demo (bootRun / Vite / docs / monitoring / Locust). Pair: 開啟Demo.cmd
REM Optional: 關閉Demo.cmd -StopDocker  |  關閉Demo.cmd -StopDockerDown
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0demo\stop-demo-services.ps1" %*
if errorlevel 1 (
  echo.
  echo Stop local Demo failed. See messages above.
  pause
) else (
  echo.
  echo Done. Ports freed. Re-start: 開啟Demo.cmd
  pause
)

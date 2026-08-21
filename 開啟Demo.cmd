@echo off
REM 本機 Demo 一鍵（bootRun／Vite／Gateway）。K8s 請用 開啟K8sDemo.cmd（擇一，勿雙棧）
REM Low RAM: add -SkipDocker -SkipLocust
cd /d "%~dp0"
REM ForceRestart＝重啟 Order／Account（清 H2、載入最新設定）
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0demo\ensure-demo-links.ps1" -ForceRestart %*
if errorlevel 1 (
  echo.
  echo LOOP not green. See FAIL above, or run: .\demo\doctor-demo.ps1
  pause
)

@echo off
REM Demo LOOP: ensure services + Vite（Gateway UP 後 /api 預設經 :8080）+ docs.
REM Daily pure run: .\gradlew.bat :order-service:bootRun  (or IntelliJ Gradle bootRun)
REM Low RAM: add -SkipDocker -SkipLocust
cd /d "%~dp0"
REM ForceRestart＝重啟 Order／Account，載入 feign-sync 與清 H2（只「ensure UP」不夠）
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0demo\ensure-demo-links.ps1" -ForceRestart %*
if errorlevel 1 (
  echo.
  echo LOOP not green. See FAIL above, or run: .\demo\doctor-demo.ps1
  pause
)

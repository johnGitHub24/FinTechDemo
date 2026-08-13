@echo off
REM Demo LOOP: ensure services + Vite + docs.
REM Daily pure run: .\gradlew.bat :order-service:bootRun  (or IntelliJ Gradle bootRun)
REM Low RAM: add -SkipDocker -SkipLocust
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0demo\ensure-demo-links.ps1" %*
if errorlevel 1 (
  echo.
  echo LOOP not green. See FAIL above, or run: .\demo\doctor-demo.ps1
  pause
)

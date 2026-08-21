@echo off
REM Local Demo one-click (= demo\ensure-demo-links.ps1 -ForceRestart). Script catalog: demo\README.md
REM K8s: 開啟K8sDemo.cmd (pick one). Low RAM: -SkipDocker -SkipLocust
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0demo\ensure-demo-links.ps1" -ForceRestart %*
if errorlevel 1 (
  echo.
  echo LOOP not green. See FAIL above, or run: .\demo\doctor-demo.ps1
  pause
)

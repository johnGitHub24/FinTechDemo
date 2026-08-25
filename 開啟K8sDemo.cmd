@echo off
REM K8s Demo one-click: stop local 808x/Vite, then kind+apply (pick one with Demo.cmd)
REM On success: port-forward :18080 + Vite :5173 (use -SkipFrontend to skip Vite only)
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0demo\start-k8s-demo.ps1" %*
if errorlevel 1 (
  echo.
  echo K8s Demo failed. Need Docker Desktop Ready. See docs\guides\intellij-k8s.md
  pause
)

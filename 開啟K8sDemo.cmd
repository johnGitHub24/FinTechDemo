@echo off
REM K8s Demo 一鍵：kind + 映像 + apply（與本機 開啟Demo.cmd 擇一，勿同時雙棧搶 RAM）
REM 就緒後：kubectl -n fintech-demo get pods；Gateway 例：port-forward → :18080
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0demo\start-k8s-demo.ps1" %*
if errorlevel 1 (
  echo.
  echo K8s Demo failed. Need Docker Desktop Ready. See docs\guides\intellij-k8s.md
  pause
)

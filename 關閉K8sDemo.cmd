@echo off
REM Stop K8s Demo (port-forward :18080, Vite). Pair: 開啟K8sDemo.cmd
REM Delete kind cluster: 關閉K8sDemo.cmd -DeleteCluster
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0demo\stop-k8s-demo.ps1" %*
if errorlevel 1 (
  echo.
  echo Stop K8s Demo failed. See messages above.
  pause
) else (
  echo.
  echo Done. Re-start K8s: 開啟K8sDemo.cmd
  pause
)

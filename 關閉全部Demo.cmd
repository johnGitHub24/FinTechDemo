@echo off
REM Stop ALL Demo modes: local services then K8s port-forward/Vite (keeps kind cluster).
REM To also delete kind: 關閉全部Demo.cmd -DeleteCluster
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0demo\stop-demo-services.ps1" -StopDocker
if errorlevel 1 goto :fail
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0demo\stop-k8s-demo.ps1" %*
if errorlevel 1 goto :fail
echo.
echo ALL_DEMO_STOPPED_OK
pause
exit /b 0
:fail
echo.
echo Stop all Demo failed. See messages above.
pause
exit /b 1

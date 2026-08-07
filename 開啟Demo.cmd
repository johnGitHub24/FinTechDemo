@echo off
REM Demo 一鍵：Order+Risk+Vite（可成交）。加參數 -Full 才開齊。
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-demo-ready.ps1" -FreeKind -OpenBrowser %*
if errorlevel 1 pause

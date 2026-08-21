#Requires -Version 5.1
# Compat: print entries (see demo/README.md)
param(
    [switch]$WithInfra,
    [switch]$WithKafkaDemo,
    [switch]$Check
)
$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
Set-Location $Root
Write-Host "NOTE: start-guide converged -> demo/README.md" -ForegroundColor DarkYellow
Write-Host ""
Write-Host "Local:  .\開啟Demo.cmd" -ForegroundColor Green
Write-Host "K8s:    .\開啟K8sDemo.cmd  (pick one)" -ForegroundColor Green
Write-Host "Doctor: .\demo\doctor-demo.ps1 -Fix" -ForegroundColor Green
Write-Host "Gate:   .\demo\verify-pipeline.ps1" -ForegroundColor Green
Write-Host ""
if ($WithInfra -or $WithKafkaDemo) {
    Write-Host "== docker compose up -d ==" -ForegroundColor Cyan
    docker compose up -d
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
if ($Check) {
    Write-Host "== gradlew check ==" -ForegroundColor Cyan
    .\gradlew.bat check
    exit $LASTEXITCODE
}
exit 0
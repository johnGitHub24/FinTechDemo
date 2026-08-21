#Requires -Version 5.1
# Compat shim -> ensure-demo-links.ps1 (see demo/README.md)
param(
    [switch]$Minimal,
    [switch]$UpInfra,
    [switch]$StartMinimal,
    [switch]$OpenDocs
)
$ErrorActionPreference = "Continue"
$Root = Split-Path $PSScriptRoot -Parent
Set-Location $Root
Write-Host "NOTE: start-demo -> prefer .\開啟Demo.cmd (demo/README.md)" -ForegroundColor DarkYellow

if ($Minimal -and -not $StartMinimal -and -not $UpInfra) {
    Write-Host "Prefer: .\開啟Demo.cmd"
    Write-Host "Or: risk + order bootRun, then cd frontend; npm run dev"
    Write-Host "Login: http://localhost:5173/login  trader1 / password"
    exit 0
}
if ($UpInfra) {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        Write-Host "ERROR: -UpInfra requires docker" -ForegroundColor Red
        exit 1
    }
    Write-Host "== docker compose up -d ==" -ForegroundColor Cyan
    docker compose up -d
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
& (Join-Path $PSScriptRoot "ensure-demo-links.ps1") -SkipDocker -SkipLocust
$code = $LASTEXITCODE
if ($OpenDocs -and $code -eq 0) { Start-Process "http://127.0.0.1:5500/docs/index.html" }
exit $code
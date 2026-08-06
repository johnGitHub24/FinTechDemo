<#
.SYNOPSIS
  Locust baseline：安裝 deps 並對 order-service / Gateway 跑短壓測。

.EXAMPLE
  .\scripts\run-loadtest.ps1
  .\scripts\run-loadtest.ps1 -HostUrl http://localhost:8080 -Users 5 -RunTime 30s
#>
param(
    [string]$HostUrl = "http://localhost:8081",
    [int]$Users = 5,
    [int]$SpawnRate = 1,
    [string]$RunTime = "30s"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
$LoadDir = Join-Path $Root "loadtest"
Set-Location $LoadDir

if (-not (Get-Command python -ErrorAction SilentlyContinue)) {
    Write-Host "python not found. Install Python 3.10+ then retry." -ForegroundColor Red
    exit 1
}

Write-Host "== pip install -r requirements.txt ==" -ForegroundColor Cyan
python -m pip install -r requirements.txt
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "== locust headless host=$HostUrl users=$Users time=$RunTime ==" -ForegroundColor Cyan
python -m locust -f locustfile.py --host $HostUrl --headless -u $Users -r $SpawnRate -t $RunTime
exit $LASTEXITCODE

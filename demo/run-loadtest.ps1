<#
.SYNOPSIS
  Locust 壓測：baseline（order :8081）或 fullflow（gateway :8080），產出 reports。

.EXAMPLE
  .\demo\run-loadtest.ps1
  .\demo\run-loadtest.ps1 -Scenario fullflow -HostUrl http://localhost:8080
  .\demo\run-loadtest.ps1 -WebUi   # 開 Locust UI :8089（前端「壓測 UI」可點）
#>
param(
    [ValidateSet("baseline", "fullflow")]
    [string]$Scenario = "baseline",
    [string]$HostUrl = "",
    [int]$Users = 5,
    [int]$SpawnRate = 1,
    [string]$RunTime = "30s",
    [switch]$WebUi
)

$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
$LoadDir = Join-Path $Root "loadtest"
$ReportDir = Join-Path $LoadDir "reports"
Set-Location $LoadDir

if (-not $HostUrl) {
    if ($Scenario -eq "fullflow") { $HostUrl = "http://localhost:8080" }
    else { $HostUrl = "http://localhost:8081" }
}

if (-not (Get-Command python -ErrorAction SilentlyContinue)) {
    Write-Host "python not found. Install Python 3.10+ then retry." -ForegroundColor Red
    exit 1
}

New-Item -ItemType Directory -Force -Path $ReportDir | Out-Null
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$html = Join-Path $ReportDir "$Scenario-$stamp.html"
$csvPrefix = Join-Path $ReportDir "$Scenario-$stamp"

$env:FINTECH_SCENARIO = $Scenario

Write-Host "== pip install -r requirements.txt ==" -ForegroundColor Cyan
python -m pip install -r requirements.txt
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if ($WebUi) {
    Write-Host "== locust WEB UI host=$HostUrl scenario=$Scenario → http://localhost:8089 ==" -ForegroundColor Cyan
    Write-Host "簡報：開 Grafana http://localhost:3000 後再壓測" -ForegroundColor Yellow
    python -m locust -f locustfile.py --host $HostUrl --web-host 0.0.0.0 --web-port 8089
    exit $LASTEXITCODE
}

Write-Host "== locust headless scenario=$Scenario host=$HostUrl users=$Users time=$RunTime ==" -ForegroundColor Cyan
Write-Host "門檻：錯誤率 < 1%（排除刻意風控拒絕）；搭配 Grafana 看 RPS" -ForegroundColor Yellow
python -m locust -f locustfile.py --host $HostUrl --headless -u $Users -r $SpawnRate -t $RunTime `
    --html $html --csv $csvPrefix
$code = $LASTEXITCODE
Write-Host "Report: $html" -ForegroundColor Green
exit $code

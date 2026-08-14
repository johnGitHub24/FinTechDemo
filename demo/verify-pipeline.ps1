# FinTechDemo — pipeline verify
#   .\demo\verify-pipeline.ps1
#   .\demo\verify-pipeline.ps1 -Up
#   .\demo\verify-pipeline.ps1 -Up -Smoke

param(
    [switch]$Up,
    [switch]$Smoke
)

$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
Set-Location $Root

function Step([string]$Name) {
    Write-Host ""
    Write-Host "== $Name ==" -ForegroundColor Cyan
}

function Test-Health([string]$Url) {
    try {
        $r = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
        return ($r.StatusCode -eq 200)
    } catch {
        return $false
    }
}

Step "1/4 gradlew check"
.\gradlew.bat check --no-daemon
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host "OK check" -ForegroundColor Green

Step "2/4 docker compose config"
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host "SKIP: docker not found" -ForegroundColor Yellow
} else {
    docker compose config --quiet
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    Write-Host "OK compose config" -ForegroundColor Green
}

Step "3/4 kustomize"
& "$PSScriptRoot\check-k8s.ps1"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if ($Up) {
    Step "4/4 docker compose up -d --build (core)"
    docker compose up -d --build redpanda redis risk-service account-service order-service gateway
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    Write-Host "OK compose up" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "4/4 SKIP deploy. Use: .\demo\verify-pipeline.ps1 -Up" -ForegroundColor Yellow
}

if ($Smoke) {
    Step "API smoke (health)"
    $checks = @(
        @{ Name = 'order'; Url = 'http://localhost:8081/actuator/health' },
        @{ Name = 'risk'; Url = 'http://localhost:8082/actuator/health' },
        @{ Name = 'account'; Url = 'http://localhost:8084/actuator/health' }
    )
    $allOk = $true
    foreach ($c in $checks) {
        $ok = Test-Health $c.Url
        Write-Host ("  {0}: {1}" -f $c.Name, $(if ($ok) { 'UP' } else { 'DOWN' }))
        if (-not $ok) { $allOk = $false }
    }
    if (-not $allOk) {
        Write-Host "SMOKE: services not all UP (start via ensure-demo-links or compose -Up)" -ForegroundColor Yellow
        exit 2
    }
    Write-Host "OK smoke health" -ForegroundColor Green
}

Write-Host ""
Write-Host "PIPELINE_OK" -ForegroundColor Green
Write-Host "Demo LOOP:  .\demo\ensure-demo-links.ps1   or  開啟Demo.cmd"
Write-Host "Docker:     .\demo\verify-pipeline.ps1 -Up"
Write-Host "K8s 全棧:   .\demo\start-k8s-demo.ps1"
exit 0

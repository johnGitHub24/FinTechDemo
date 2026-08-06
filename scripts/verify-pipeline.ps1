# FinTechDemo — 一條 Pipeline 驗證（簡單可跑）
# 用法：
#   .\scripts\verify-pipeline.ps1              # check + compose config + k8s
#   .\scripts\verify-pipeline.ps1 -Up          # 同上 + docker compose up --build
#   .\scripts\verify-pipeline.ps1 -Up -Smoke   # 再打一輪 API 煙霧

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

Step "3/4 kustomize (account included)"
& "$PSScriptRoot\check-k8s.ps1"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if ($Up) {
    Step "4/4 docker compose up -d --build (core)"
    # 核心：infra + risk + account + order + gateway（不含 job/frontend profile）
    docker compose up -d --build redpanda redis risk-service account-service order-service gateway
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    Write-Host "OK compose up" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "4/4 SKIP deploy. Use: .\scripts\verify-pipeline.ps1 -Up" -ForegroundColor Yellow
}

if ($Smoke) {
    Step "API smoke"
    & "$PSScriptRoot\smoke-distributed.ps1" -SkipCompose
    if ($LASTEXITCODE -ne 0 -and $LASTEXITCODE -ne 2) { exit $LASTEXITCODE }
    if ($LASTEXITCODE -eq 2) {
        Write-Host "Services still starting — wait then: .\scripts\smoke-distributed.ps1 -SkipCompose" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "PIPELINE_OK" -ForegroundColor Green
Write-Host "Minimal run:  .\gradlew.bat :order-service:bootRun + frontend npm run dev"
Write-Host "Docker run:   .\scripts\verify-pipeline.ps1 -Up"
Write-Host "K8s render:   .\scripts\check-k8s.ps1"
exit 0

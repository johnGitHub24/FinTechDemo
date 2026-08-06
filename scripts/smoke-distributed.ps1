<#
.SYNOPSIS
  分散式手測：Kafka + Redis infra +（若服務已起）API 煙霧流程。

.DESCRIPTION
  1) docker compose up（Redpanda :19092、Redis :6379）
  2) 探測 order/risk/account health
  3) 若三服務就緒：login → 下單 → execute → 查 account-service 餘額
  服務未起時：印出啟動指令（exit 2），方便學習「先 infra、再 MS」。

.EXAMPLE
  .\scripts\smoke-distributed.ps1
  .\scripts\smoke-distributed.ps1 -SkipCompose
#>
param(
    [switch]$SkipCompose,
    [int]$WaitSeconds = 45
)

$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
Set-Location $Root

function Test-Port([int]$Port) {
    try {
        $c = New-Object System.Net.Sockets.TcpClient
        $c.Connect("127.0.0.1", $Port)
        $c.Close()
        return $true
    } catch {
        return $false
    }
}

function Wait-Port([int]$Port, [int]$Seconds) {
    $deadline = (Get-Date).AddSeconds($Seconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-Port $Port) { return $true }
        Start-Sleep -Seconds 2
    }
    return $false
}

function Test-Health([string]$Url) {
    try {
        $r = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
        return $r.StatusCode -eq 200
    } catch {
        return $false
    }
}

Write-Host "== FinTechDemo smoke-distributed ==" -ForegroundColor Cyan

if (-not $SkipCompose) {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        Write-Host "ERROR: docker not found" -ForegroundColor Red
        exit 1
    }
    Write-Host "docker compose up -d ..." -ForegroundColor Cyan
    docker compose up -d
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

Write-Host "Waiting Kafka :19092 / Redis :6379 ..." -ForegroundColor Cyan
if (-not (Wait-Port 19092 $WaitSeconds)) {
    Write-Host "ERROR: Kafka (19092) not ready" -ForegroundColor Red
    exit 1
}
if (-not (Wait-Port 6379 $WaitSeconds)) {
    Write-Host "ERROR: Redis (6379) not ready" -ForegroundColor Red
    exit 1
}
Write-Host "Infra OK (Kafka + Redis)" -ForegroundColor Green

$orderOk = Test-Health "http://localhost:8081/actuator/health"
$riskOk = Test-Health "http://localhost:8082/actuator/health"
$accountOk = Test-Health "http://localhost:8084/actuator/health"

Write-Host "order:8081   health=$(if($orderOk){'UP'}else{'DOWN'})"
Write-Host "risk:8082    health=$(if($riskOk){'UP'}else{'DOWN'})"
Write-Host "account:8084 health=$(if($accountOk){'UP'}else{'DOWN'})"

if (-not ($orderOk -and $riskOk -and $accountOk)) {
    Write-Host ""
    Write-Host "Services not all up. Start Distributed Demo (separate terminals):" -ForegroundColor Yellow
    Write-Host "  .\gradlew.bat :risk-service:bootRun"
    Write-Host "  .\gradlew.bat :account-service:bootRun --args='--spring.profiles.active=demo'"
    Write-Host "  .\gradlew.bat :order-service:bootRun --args='--spring.profiles.active=demo'"
    Write-Host "  .\gradlew.bat :gateway:bootRun"
    Write-Host "Then re-run: .\scripts\smoke-distributed.ps1 -SkipCompose"
    Write-Host "Docs: docs/architecture.md · docs/分散式系統落地.md"
    exit 2
}

Write-Host "API smoke: login → create → execute → account ..." -ForegroundColor Cyan
$login = Invoke-RestMethod -Uri http://localhost:8081/api/auth/login -Method POST `
    -ContentType 'application/json' -Body '{"username":"trader1","password":"password"}'
if (-not $login.token) {
    Write-Host "ERROR: login failed" -ForegroundColor Red
    exit 1
}
$h = @{ Authorization = "Bearer $($login.token)" }
$cid = "SMOKE-" + [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$body = @{
    clientOrderId = $cid
    symbol = "MSFT"
    side = "BUY"
    quantity = 1
    price = 10.00
} | ConvertTo-Json

$created = Invoke-RestMethod -Uri http://localhost:8081/api/orders -Method POST `
    -Headers $h -ContentType 'application/json' -Body $body
$orderId = $created.id
Write-Host "created orderId=$orderId status=$($created.status)"

# sync path always works; with demo kafka, create may auto-execute — still call execute if PENDING
if ($created.status -eq "PENDING") {
    $executed = Invoke-RestMethod -Uri "http://localhost:8081/api/orders/$orderId/execute" -Method POST -Headers $h
    Write-Host "executed status=$($executed.status)"
} else {
    Write-Host "order already $($created.status) (likely Kafka consumer)"
}

Start-Sleep -Seconds 2
$acc = Invoke-RestMethod -Uri http://localhost:8084/api/accounts/me -Headers $h
Write-Host "account-service cashBalance=$($acc.cashBalance) currency=$($acc.currency)" -ForegroundColor Green

if (Get-Command redis-cli -ErrorAction SilentlyContinue) {
    $keys = redis-cli KEYS "account:*"
    Write-Host "redis keys sample: $keys"
} else {
    Write-Host "redis-cli not installed — skip key dump (Redis port is up)"
}

Write-Host "SMOKE_DISTRIBUTED_OK" -ForegroundColor Green
exit 0

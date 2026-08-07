#Requires -Version 5.1
<#
.SYNOPSIS
  Demo 一鍵就緒：優先保證 Order+Risk+Vite（可成交），再依序補齊其餘服務。

.DESCRIPTION
  Root cause（本機曾炸）：RAM 不足時同時起多個 Gradle Daemon／bootRun →
  「insufficient memory」「服務狀態全 DOWN」。
  本腳本：檢查記憶體 → 依序啟動 → 每起一個等 health → 避免一次開五個 JVM。

.PARAMETER Full
  在最短可成交後，再依序起 gateway／account／job。

.PARAMETER FreeKind
  先 docker stop kind control-plane 釋放 RAM（Demo 完可再用 TradingKubernetes start-local 拉回）。

.EXAMPLE
  .\scripts\start-demo-ready.ps1
  .\scripts\start-demo-ready.ps1 -Full -FreeKind -OpenBrowser
#>
param(
    [switch] $Full,
    [switch] $FreeKind,
    [switch] $OpenBrowser,
    [int] $MinFreeMb = 800
)

$ErrorActionPreference = 'Continue'
$Root = Split-Path $PSScriptRoot -Parent
Set-Location $Root
$logs = Join-Path $Root 'logs'
New-Item -ItemType Directory -Force -Path $logs | Out-Null
$gradlew = Join-Path $Root 'gradlew.bat'
$env:GRADLE_OPTS = '-Xmx512m'

function Get-FreeMb {
    $os = Get-CimInstance Win32_OperatingSystem
    return [int]($os.FreePhysicalMemory / 1024)
}

function Test-HttpOk([string] $Url) {
    try {
        $r = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
        return ($r.StatusCode -ge 200 -and $r.StatusCode -lt 500)
    } catch { return $false }
}

function Wait-HttpOk([string] $Url, [int] $Seconds = 180) {
    $deadline = (Get-Date).AddSeconds($Seconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-HttpOk $Url) { return $true }
        Start-Sleep -Seconds 3
    }
    return $false
}

function Write-Mem {
    Write-Host ("  可用記憶體: {0} MB" -f (Get-FreeMb)) -ForegroundColor DarkCyan
}

function Ensure-Boot([string] $Name, [string] $Task, [string] $Health, [string[]] $ExtraArgs = @()) {
    if (Test-HttpOk $Health) {
        Write-Host "  OK UP $Name" -ForegroundColor Green
        return $true
    }
    $free = Get-FreeMb
    if ($free -lt $MinFreeMb) {
        Write-Host "  SKIP $Name — 可用 RAM ${free}MB < ${MinFreeMb}MB。請關瀏覽器分頁／Docker／kind 後重跑 -Full" -ForegroundColor Yellow
        return $false
    }
    Write-Host "  START $Name (sequential, no parallel gradle) ..." -ForegroundColor Cyan
    Write-Mem
    $args = @($Task, '--no-daemon') + $ExtraArgs
    Start-Process -FilePath $gradlew -ArgumentList $args `
        -WorkingDirectory $Root -WindowStyle Minimized `
        -RedirectStandardOutput (Join-Path $logs "$Name.out.log") `
        -RedirectStandardError (Join-Path $logs "$Name.err.log")
    if (Wait-HttpOk $Health 240) {
        Write-Host "  OK UP $Name" -ForegroundColor Green
        Start-Sleep -Seconds 2
        return $true
    }
    Write-Host "  FAIL $Name — logs\$Name.*.log" -ForegroundColor Red
    return $false
}

function Ensure-Vite {
    if (Test-HttpOk 'http://localhost:5173/login') {
        Write-Host "  OK UP frontend :5173" -ForegroundColor Green
        return $true
    }
    Write-Host "  START frontend ..." -ForegroundColor Cyan
    Start-Process -FilePath 'cmd.exe' -ArgumentList @('/c', 'npm.cmd run dev') `
        -WorkingDirectory (Join-Path $Root 'frontend') -WindowStyle Minimized `
        -RedirectStandardOutput (Join-Path $logs 'frontend.out.log') `
        -RedirectStandardError (Join-Path $logs 'frontend.err.log')
    if (Wait-HttpOk 'http://localhost:5173/login' 90) {
        Write-Host "  OK UP frontend" -ForegroundColor Green
        return $true
    }
    Write-Host "  FAIL frontend" -ForegroundColor Red
    return $false
}

Write-Host ''
Write-Host '======== FinTechDemo Demo 啟動 ========' -ForegroundColor Cyan
Write-Host '目標：登入 → 下單 → 成交（Order+Risk+Vite）。-Full 再補 Gateway/Account/Job。' -ForegroundColor Yellow
Write-Mem

if ($FreeKind) {
    Write-Host '釋放 kind control-plane 記憶體…' -ForegroundColor Cyan
    docker stop trading-local-control-plane 2>$null | Out-Null
    Start-Sleep -Seconds 3
    Write-Mem
}

Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -match 'GradleDaemon' } |
    ForEach-Object {
        Write-Host "  stop GradleDaemon pid=$($_.ProcessId)" -ForegroundColor DarkGray
        Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
    }
Start-Sleep -Seconds 2
Write-Mem

Write-Host ''
Write-Host '== Demo 最短可成交（必開）==' -ForegroundColor Cyan
Ensure-Boot 'risk-service' ':risk-service:bootRun' 'http://localhost:8082/actuator/health' | Out-Null
Ensure-Boot 'order-service' ':order-service:bootRun' 'http://localhost:8081/actuator/health' | Out-Null
Ensure-Vite | Out-Null

if ($Full) {
    Write-Host ''
    Write-Host '== Demo 完整敘事（依序）==' -ForegroundColor Cyan
    Ensure-Boot 'gateway' ':gateway:bootRun' 'http://localhost:8080/actuator/health' | Out-Null
    Ensure-Boot 'account-service' ':account-service:bootRun' 'http://localhost:8084/actuator/health' | Out-Null
    Ensure-Boot 'job-service' ':job-service:bootRun' 'http://localhost:8083/actuator/health' | Out-Null
}

Write-Host ''
Write-Host '== Demo 檢查清單 ==' -ForegroundColor Cyan
$checks = @(
    @{ n = 'Vite'; u = 'http://localhost:5173/login'; must = $true },
    @{ n = 'Order'; u = 'http://localhost:8081/actuator/health'; must = $true },
    @{ n = 'Risk'; u = 'http://localhost:8082/actuator/health'; must = $true },
    @{ n = 'Gateway'; u = 'http://localhost:8080/actuator/health'; must = $false },
    @{ n = 'Account'; u = 'http://localhost:8084/actuator/health'; must = $false },
    @{ n = 'Job'; u = 'http://localhost:8083/actuator/health'; must = $false }
)
$failMust = 0
foreach ($c in $checks) {
    $ok = Test-HttpOk $c.u
    if ($ok) { Write-Host "  OK  $($c.n)" -ForegroundColor Green }
    else {
        $col = if ($c.must) { 'Red' } else { 'Yellow' }
        Write-Host "  DOWN $($c.n)$(if (-not $c.must) { '（可選）' })" -ForegroundColor $col
        if ($c.must) { $failMust++ }
    }
}

Write-Host ''
if ($failMust -eq 0) {
    Write-Host 'Demo 最短路徑 READY：http://localhost:5173/login  （trader1 / password）' -ForegroundColor Green
    Write-Host 'Demo 劇本：登入 → /trade 下單 → 成交（需 Risk UP）→ /portal 看歷史' -ForegroundColor Green
    if ($OpenBrowser) {
        Start-Process 'http://localhost:5173/login'
        Start-Process 'http://localhost:5173/blueprint'
    }
    exit 0
}

Write-Host '必開服務尚未齊。關 Docker Desktop 未用容器／瀏覽器後重跑：' -ForegroundColor Red
Write-Host '  .\scripts\start-demo-ready.ps1 -FreeKind -OpenBrowser' -ForegroundColor Yellow
exit 1

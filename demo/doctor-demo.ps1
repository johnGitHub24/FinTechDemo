#Requires -Version 5.1
<#
.SYNOPSIS
  診斷 FinTechDemo「localhost 時好時壞／拒絕連線」並可一鍵修復。

.DESCRIPTION
  Root cause（不是防火牆）：本機 Demo 依賴手動行程（Vite :5173、bootRun :808x）。
  行程結束後埠就 DOWN → 瀏覽器 ERR_CONNECTION_REFUSED。
  kind 裡 Pod Running ≠ 本機 Vite 有在聽。

.PARAMETER Fix
  呼叫 ensure-demo-links 拉起缺的服務（含前端）。

.PARAMETER FrontendOnly
  只確保 Vite :5173（＋可選開瀏覽器）。

.PARAMETER OpenBrowser
  修復後開啟登入／藍圖 K8s 指令頁。

.EXAMPLE
  .\demo\doctor-demo.ps1
  .\demo\doctor-demo.ps1 -Fix -OpenBrowser
  .\demo\doctor-demo.ps1 -FrontendOnly -OpenBrowser
#>
param(
    [switch] $Fix,
    [switch] $FrontendOnly,
    [switch] $OpenBrowser
)

$ErrorActionPreference = 'Continue'
$Root = Split-Path $PSScriptRoot -Parent
Set-Location $Root

function Test-PortListen([int] $Port) {
    return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1)
}

function Test-HttpOk([string] $Url) {
    try {
        $r = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
        return ($r.StatusCode -ge 200 -and $r.StatusCode -lt 500)
    } catch {
        return $false
    }
}

Write-Host ""
Write-Host "======== FinTechDemo doctor-demo ========" -ForegroundColor Cyan
Write-Host "【Root cause】本機網頁不是永遠開著的服務。" -ForegroundColor Yellow
Write-Host "  Vite(npm run dev)、gradle bootRun 是「手動行程」。" -ForegroundColor Yellow
Write-Host "  關掉終端機／重開機／Agent 工作結束 → 埠消失 → ERR_CONNECTION_REFUSED。" -ForegroundColor Yellow
Write-Host "  這通常不是 Proxy／防火牆；也不是「K8s 壞了」。" -ForegroundColor Yellow
Write-Host "  kind Pod Running 只代表叢集內容器，不會自動提供 http://localhost:5173" -ForegroundColor Yellow
Write-Host ""

$checks = @(
    @{ Name = 'Vite frontend'; Port = 5173; Url = 'http://localhost:5173/login'; Hint = 'npm run dev 或 ensure-demo-links' },
    @{ Name = 'Order';         Port = 8081; Url = 'http://localhost:8081/actuator/health'; Hint = ':order-service:bootRun' },
    @{ Name = 'Risk';          Port = 8082; Url = 'http://localhost:8082/actuator/health'; Hint = ':risk-service:bootRun' },
    @{ Name = 'Gateway';       Port = 8080; Url = 'http://localhost:8080/actuator/health'; Hint = ':gateway:bootRun' },
    @{ Name = 'Job';           Port = 8083; Url = 'http://localhost:8083/actuator/health'; Hint = ':job-service:bootRun' },
    @{ Name = 'Account';       Port = 8084; Url = 'http://localhost:8084/actuator/health'; Hint = ':account-service:bootRun' },
    @{ Name = 'Docs static';   Port = 5500; Url = 'http://127.0.0.1:5500/docs/index.html'; Hint = '.\docs\tools\serve-docs.ps1' }
)

$down = @()
foreach ($c in $checks) {
    $listen = Test-PortListen $c.Port
    $http = if ($listen) { Test-HttpOk $c.Url } else { $false }
    if ($http) {
        Write-Host ("  OK   :{0,-5} {1}" -f $c.Port, $c.Name) -ForegroundColor Green
    } else {
        $down += $c
        $why = if (-not $listen) { '埠未 Listen（行程沒開）' } else { '有 Listen 但 HTTP 失敗' }
        Write-Host ("  DOWN :{0,-5} {1} — {2}" -f $c.Port, $c.Name, $why) -ForegroundColor Red
        Write-Host ("         修復提示: {0}" -f $c.Hint) -ForegroundColor DarkYellow
    }
}

# Docker / kind（附帶說明，避免混淆）
Write-Host ""
Write-Host "--- Docker / kind（與 localhost:5173 是另一條路）---" -ForegroundColor Cyan
$dockerOk = $false
try {
    $info = docker info 2>&1 | Out-String
    if ($info -match 'Server Version:') {
        $dockerOk = $true
        Write-Host "  OK   Docker 引擎 Ready" -ForegroundColor Green
    }
} catch { }
if (-not $dockerOk) {
    Write-Host "  DOWN Docker 引擎未就緒 → 先開 Docker Desktop" -ForegroundColor Yellow
}
try {
    $ready = kubectl get --raw=/readyz 2>$null
    if ($ready -eq 'ok') {
        Write-Host "  OK   kind API readyz=ok（≠ Vite 已開）" -ForegroundColor Green
    } else {
        Write-Host "  DOWN kind API 無法連（context 可能是死叢集）" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  SKIP kubectl 不可用或叢集未起" -ForegroundColor DarkGray
}

Write-Host ""
if ($down.Count -eq 0 -and -not $Fix -and -not $FrontendOnly) {
    Write-Host "診斷結果：本機 Demo 埠皆可達。" -ForegroundColor Green
    if ($OpenBrowser) {
        Start-Process 'http://localhost:5173/login'
        Start-Process 'http://localhost:5173/blueprint#k8s-verify'
    }
    exit 0
}

if ($down.Count -gt 0) {
    Write-Host "診斷結果：有 $($down.Count) 個本機目標 DOWN → 瀏覽器會「拒絕連線」。" -ForegroundColor Red
    $viteDown = $down | Where-Object { $_.Port -eq 5173 }
    if ($viteDown) {
        Write-Host ""
        Write-Host "你現在若開 http://localhost:5173 → ERR_CONNECTION_REFUSED 的直接原因：" -ForegroundColor Magenta
        Write-Host "  Vite 開發伺服器沒在跑（行程消失），不是網站壞掉。" -ForegroundColor Magenta
    }
}

if (-not $Fix -and -not $FrontendOnly) {
    Write-Host ""
    Write-Host "一鍵修復：" -ForegroundColor Cyan
    Write-Host "  .\demo\doctor-demo.ps1 -Fix -OpenBrowser"
    Write-Host "  .\demo\doctor-demo.ps1 -FrontendOnly -OpenBrowser   # 只救網頁"
    Write-Host "  .\demo\ensure-demo-links.ps1"
    Write-Host "  或雙擊：開啟Demo.cmd"
    exit 1
}

Write-Host ""
Write-Host "== 開始修復 ==" -ForegroundColor Cyan
$ensure = Join-Path $PSScriptRoot 'ensure-demo-links.ps1'
if ($FrontendOnly) {
    & $ensure -FrontendOnly -SkipDocker -SkipLocust
} else {
    & $ensure
}
$code = $LASTEXITCODE

if ($OpenBrowser -or $Fix -or $FrontendOnly) {
    Start-Sleep -Seconds 1
    if (Test-HttpOk 'http://localhost:5173/login') {
        Write-Host 'Opening browser...' -ForegroundColor Green
        Start-Process 'http://localhost:5173/login'
        Start-Process 'http://localhost:5173/blueprint#k8s-verify'
    } else {
        Write-Host 'Frontend not ready — see logs\frontend.*.log' -ForegroundColor Red
    }
}

exit $code

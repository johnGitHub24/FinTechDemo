# FinTechDemo ensure-demo-links.ps1
# LOOP: start services until Demo shortcut URLs respond.
# Encoding: UTF-8 with BOM (Windows PowerShell 5.x)
#
# 【Root cause 提醒】localhost 時好時壞＝本機行程沒常駐（Vite/bootRun），
# 不是防火牆。kind Pod Running ≠ :5173。詳見 doctor-demo.ps1 / SPEC §3.1。
param(
    [switch]$SkipDocker,
    [switch]$SkipLocust,
    [switch]$FrontendOnly
)

$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$Root = Split-Path $PSScriptRoot -Parent
Set-Location $Root
$logs = Join-Path $Root "logs"
New-Item -ItemType Directory -Force -Path $logs | Out-Null
$gradlew = Join-Path $Root "gradlew.bat"

Write-Host "== LOOP ensure-demo-links ==" -ForegroundColor Cyan
Write-Host "提醒: Vite/bootRun 關掉終端就沒了 → 瀏覽器會拒絕連線。本腳本會把缺的行程拉起來。" -ForegroundColor Yellow
Write-Host ""

function Wait-HttpOk([string]$Url, [int]$Seconds = 120) {
    $deadline = (Get-Date).AddSeconds($Seconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $r = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
            if ($r.StatusCode -ge 200 -and $r.StatusCode -lt 500) { return $true }
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    return $false
}

function Test-HttpOk([string]$Url) {
    try {
        $r = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
        return ($r.StatusCode -ge 200 -and $r.StatusCode -lt 500)
    } catch {
        return $false
    }
}

function Ensure-BootService([string]$Name, [string]$GradleTask, [string]$HealthUrl) {
    if (Test-HttpOk $HealthUrl) {
        Write-Host "  OK UP $Name  $HealthUrl" -ForegroundColor Green
        return $true
    }
    Write-Host "  START $Name ($GradleTask) ..." -ForegroundColor Cyan
    Start-Process -FilePath $gradlew -ArgumentList $GradleTask `
        -WorkingDirectory $Root -WindowStyle Minimized `
        -RedirectStandardOutput (Join-Path $logs "$Name.out.log") `
        -RedirectStandardError (Join-Path $logs "$Name.err.log")
    if (Wait-HttpOk $HealthUrl 180) {
        Write-Host "  OK UP $Name  $HealthUrl" -ForegroundColor Green
        return $true
    }
    Write-Host "  FAIL $Name not ready. See logs\$Name.*.log" -ForegroundColor Red
    return $false
}

function Ensure-Frontend {
    if (Test-HttpOk "http://localhost:5173/login") {
        Write-Host "  OK frontend :5173" -ForegroundColor Green
        return $true
    }
    Write-Host "  START frontend vite (detached) ..." -ForegroundColor Cyan
    # cmd /c + Minimized：獨立行程，不隨本腳本視窗結束
    Start-Process -FilePath "cmd.exe" -ArgumentList @("/c", "npm.cmd run dev") `
        -WorkingDirectory (Join-Path $Root "frontend") -WindowStyle Minimized `
        -RedirectStandardOutput (Join-Path $logs "frontend.out.log") `
        -RedirectStandardError (Join-Path $logs "frontend.err.log")
    if (Wait-HttpOk "http://localhost:5173/login" 90) {
        Write-Host "  OK frontend :5173" -ForegroundColor Green
        return $true
    }
    Write-Host "  FAIL frontend — see logs\frontend.*.log" -ForegroundColor Red
    return $false
}

function Ensure-Docs {
    if (Test-HttpOk "http://127.0.0.1:5500/docs/index.html") {
        Write-Host "  OK docs :5500" -ForegroundColor Green
        return $true
    }
    Write-Host "  START serve-docs ..." -ForegroundColor Cyan
    Start-Process -FilePath "powershell.exe" `
        -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", (Join-Path $PSScriptRoot "serve-docs.ps1")) `
        -WorkingDirectory $Root -WindowStyle Minimized
    if (Wait-HttpOk "http://127.0.0.1:5500/docs/index.html" 45) {
        Write-Host "  OK docs :5500" -ForegroundColor Green
        return $true
    }
    Write-Host "  FAIL docs" -ForegroundColor Red
    return $false
}

if ($FrontendOnly) {
    Write-Host "== FrontendOnly mode ==" -ForegroundColor Cyan
    if (Ensure-Frontend) {
        Ensure-Docs | Out-Null
        Write-Host "LOOP OK (frontend)" -ForegroundColor Green
        exit 0
    }
    Write-Host "LOOP incomplete: frontend still down" -ForegroundColor Red
    exit 1
}

Ensure-BootService "risk-service" ":risk-service:bootRun" "http://localhost:8082/actuator/health" | Out-Null
Ensure-BootService "account-service" ":account-service:bootRun" "http://localhost:8084/actuator/health" | Out-Null
Ensure-BootService "order-service" ":order-service:bootRun" "http://localhost:8081/actuator/health" | Out-Null
Ensure-BootService "gateway" ":gateway:bootRun" "http://localhost:8080/actuator/health" | Out-Null
Ensure-BootService "job-service" ":job-service:bootRun" "http://localhost:8083/actuator/health" | Out-Null

Ensure-Frontend | Out-Null
Ensure-Docs | Out-Null

if (-not $SkipDocker) {
    if (Get-Command docker -ErrorAction SilentlyContinue) {
        $prev = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        $dinfo = docker info 2>&1 | Out-String
        $ErrorActionPreference = $prev
        if ($dinfo -notmatch 'Server Version:') {
            Write-Host "  SKIP monitoring: Docker Desktop 引擎未 Ready（先開 Docker Desktop）" -ForegroundColor Yellow
        } else {
            Write-Host "  START docker compose --profile monitoring ..." -ForegroundColor Cyan
            & docker compose --profile monitoring up -d prometheus grafana
            Wait-HttpOk "http://localhost:9090/-/healthy" 120 | Out-Null
            Wait-HttpOk "http://localhost:3000/login" 120 | Out-Null
            if (Test-HttpOk "http://localhost:9090/-/healthy") {
                Write-Host "  OK prometheus :9090" -ForegroundColor Green
            } else {
                Write-Host "  FAIL prometheus" -ForegroundColor Red
            }
            if (Test-HttpOk "http://localhost:3000/login") {
                Write-Host "  OK grafana :3000" -ForegroundColor Green
            } else {
                Write-Host "  FAIL grafana" -ForegroundColor Red
            }
        }
    } else {
        Write-Host "  SKIP docker not found" -ForegroundColor Yellow
    }
}

if (-not $SkipLocust) {
    if (Test-HttpOk "http://localhost:8089/") {
        Write-Host "  OK locust :8089" -ForegroundColor Green
    } elseif (Get-Command python -ErrorAction SilentlyContinue) {
        Write-Host "  START locust Web UI ..." -ForegroundColor Cyan
        $load = Join-Path $Root "loadtest"
        Push-Location $load
        python -m pip install -r requirements.txt -q
        Pop-Location
        Start-Process -FilePath "python" `
            -ArgumentList @("-m", "locust", "-f", "locustfile.py", "--host", "http://localhost:8081", "--web-host", "0.0.0.0", "--web-port", "8089") `
            -WorkingDirectory $load -WindowStyle Minimized `
            -RedirectStandardOutput (Join-Path $logs "locust.out.log") `
            -RedirectStandardError (Join-Path $logs "locust.err.log")
        if (Wait-HttpOk "http://localhost:8089/" 90) {
            Write-Host "  OK locust :8089" -ForegroundColor Green
        } else {
            Write-Host "  FAIL locust" -ForegroundColor Red
        }
    } else {
        Write-Host "  SKIP locust (python not found)" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "== Verify probe ==" -ForegroundColor Cyan
$probe = @(
    "http://localhost:5173/login",
    "http://localhost:5173/blueprint",
    "http://localhost:5173/demo/risk-check.html",
    "http://localhost:5173/demo/account-me.html",
    "http://localhost:8081/actuator/health",
    "http://localhost:8081/swagger-ui/index.html",
    "http://localhost:8081/v3/api-docs",
    "http://localhost:8081/h2-console/",
    "http://localhost:8081/actuator/prometheus",
    "http://localhost:8082/actuator/health",
    "http://localhost:8080/actuator/health",
    "http://localhost:8083/actuator/health",
    "http://localhost:8084/actuator/health",
    "http://localhost:3000/login",
    "http://localhost:9090/-/healthy",
    "http://localhost:8089/",
    "http://127.0.0.1:5500/docs/index.html",
    "http://127.0.0.1:5500/docs/portals/demo-flow.html",
    "http://127.0.0.1:5500/docs/portals/handbook.html",
    "http://127.0.0.1:5500/docs/portals/swagger.html",
    "http://127.0.0.1:5500/docs/portals/codeGraphic.html"
)

$fail = 0
foreach ($u in $probe) {
    if (Test-HttpOk $u) {
        Write-Host "  OK  $u" -ForegroundColor Green
    } else {
        $fail++
        Write-Host "  FAIL $u" -ForegroundColor Red
    }
}

if ($fail -gt 0) {
    Write-Host "LOOP incomplete: $fail failing" -ForegroundColor Red
    Write-Host "下一步: .\scripts\doctor-demo.ps1   （看哪個埠 DOWN）" -ForegroundColor Yellow
    exit 1
}
Write-Host "LOOP OK: all demo link targets reachable" -ForegroundColor Green
exit 0

# FinTechDemo ensure-demo-links.ps1
# LOOP: start services until Demo shortcut URLs respond.
# Encoding: UTF-8 with BOM (Windows PowerShell 5.x)
#
# 【Root cause 提醒】localhost 時好時壞＝本機行程沒常駐（Vite/bootRun），
# 不是防火牆。kind Pod Running ≠ :5173。詳見 doctor-demo.ps1 / SPEC §3.1。
param(
    [switch]$SkipDocker,
    [switch]$SkipLocust,
    [switch]$SkipTestReports,
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
Write-Host "由 OrderServiceApplication 自動觸發時：已 UP 的服務會 KEEP，只補 DOWN。" -ForegroundColor DarkCyan
Write-Host ""

function Clear-ZombiePort([int]$Port, [string]$HealthUrl) {
    if (Test-HttpOk $HealthUrl) { return }
    $conns = @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
    foreach ($c in $conns) {
        $ownerPid = [int]$c.OwningProcess
        if ($ownerPid -le 0) { continue }
        Write-Host "  clear zombie :$Port pid=$ownerPid" -ForegroundColor DarkGray
        Stop-Process -Id $ownerPid -Force -ErrorAction SilentlyContinue
    }
}

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

function Ensure-BootService([string]$Name, [string]$GradleTask, [string]$HealthUrl, [int]$Port) {
    if (Test-HttpOk $HealthUrl) {
        Write-Host "  OK UP $Name  $HealthUrl" -ForegroundColor Green
        return $true
    }
    Clear-ZombiePort $Port $HealthUrl
    Write-Host "  START $Name ($GradleTask) ..." -ForegroundColor Cyan
    Start-Process -FilePath $gradlew -ArgumentList "$GradleTask --no-daemon" `
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
    Clear-ZombiePort 5173 "http://localhost:5173/login"
    $npmCmd = Get-Command npm.cmd -ErrorAction SilentlyContinue
    if (-not $npmCmd) {
        Write-Host "  FAIL frontend — npm.cmd not found" -ForegroundColor Red
        return $false
    }
    Write-Host "  START frontend vite (detached) ..." -ForegroundColor Cyan
    Start-Process -FilePath $npmCmd.Source -ArgumentList @('run', 'dev', '--', '--host', '127.0.0.1', '--port', '5173', '--strictPort') `
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

Ensure-BootService "risk-service" ":risk-service:bootRun" "http://localhost:8082/actuator/health" 8082 | Out-Null
Ensure-BootService "account-service" ":account-service:bootRun" "http://localhost:8084/actuator/health" 8084 | Out-Null
Ensure-BootService "order-service" ":order-service:bootRun" "http://localhost:8081/actuator/health" 8081 | Out-Null
Ensure-BootService "gateway" ":gateway:bootRun" "http://localhost:8080/actuator/health" 8080 | Out-Null
Ensure-BootService "job-service" ":job-service:bootRun" "http://localhost:8083/actuator/health" 8083 | Out-Null

Ensure-Frontend | Out-Null
Ensure-Docs | Out-Null

function Ensure-Monitoring {
    if ((Test-HttpOk "http://localhost:9090/-/healthy") -and (Test-HttpOk "http://localhost:3000/login")) {
        Write-Host "  OK prometheus :9090 / grafana :3000" -ForegroundColor Green
        return $true
    }

    $dockerOk = $false
    if (Get-Command docker -ErrorAction SilentlyContinue) {
        $prev = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        $dinfo = docker info 2>&1 | Out-String
        $ErrorActionPreference = $prev
        $dockerOk = ($dinfo -match 'Server Version:')
    }

    if ($dockerOk) {
        Write-Host "  START docker compose --profile monitoring ..." -ForegroundColor Cyan
        & docker compose --profile monitoring up -d prometheus grafana
        Wait-HttpOk "http://localhost:9090/-/healthy" 120 | Out-Null
        Wait-HttpOk "http://localhost:3000/login" 120 | Out-Null
    }

    $needLocal = -not ((Test-HttpOk "http://localhost:9090/-/healthy") -and (Test-HttpOk "http://localhost:3000/login"))
    if ($needLocal) {
        if ($dockerOk) {
            Write-Host "  Docker monitoring 未就緒 → fallback start-monitoring-local.ps1" -ForegroundColor Yellow
        } else {
            Write-Host "  Docker 引擎未 Ready → fallback start-monitoring-local.ps1" -ForegroundColor Yellow
        }
        $localMon = Join-Path $PSScriptRoot "start-monitoring-local.ps1"
        if (Test-Path $localMon) {
            & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $localMon
            Wait-HttpOk "http://localhost:9090/-/healthy" 90 | Out-Null
            Wait-HttpOk "http://localhost:3000/login" 90 | Out-Null
        } elseif (-not $dockerOk) {
            Write-Host "  FAIL missing start-monitoring-local.ps1（請開 Docker Desktop 或安裝 tools/）" -ForegroundColor Red
            return $false
        }
    }

    $ok = $true
    if (Test-HttpOk "http://localhost:9090/-/healthy") {
        Write-Host "  OK prometheus :9090" -ForegroundColor Green
    } else {
        Write-Host "  FAIL prometheus" -ForegroundColor Red
        $ok = $false
    }
    if (Test-HttpOk "http://localhost:3000/login") {
        Write-Host "  OK grafana :3000" -ForegroundColor Green
    } else {
        Write-Host "  FAIL grafana" -ForegroundColor Red
        $ok = $false
    }
    return $ok
}

if (-not $SkipDocker) {
    Ensure-Monitoring | Out-Null
} else {
    Write-Host "  SKIP monitoring (-SkipDocker)" -ForegroundColor DarkGray
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
} else {
    Write-Host "  SKIP locust (-SkipLocust)" -ForegroundColor DarkGray
}

function Ensure-Javadoc {
    $idx = Join-Path $Root "docs\javadoc\index.html"
    if (Test-Path $idx) {
        Write-Host "  OK javadoc docs\javadoc\index.html" -ForegroundColor Green
        return $true
    }
    Write-Host "  START aggregateJavadoc ..." -ForegroundColor Cyan
    & $gradlew "aggregateJavadoc" "--no-daemon"
    if (Test-Path $idx) {
        Write-Host "  OK javadoc" -ForegroundColor Green
        return $true
    }
    Write-Host "  FAIL javadoc — see gradle output" -ForegroundColor Red
    return $false
}

function Ensure-TestReports {
    $mods = @("order-service", "risk-service", "account-service", "gateway", "job-service")
    $missing = @()
    foreach ($m in $mods) {
        $p = Join-Path $Root "$m\build\reports\tests\test\index.html"
        if (-not (Test-Path $p)) { $missing += $m }
    }
    if ($missing.Count -eq 0) {
        Write-Host "  OK test reports (all modules)" -ForegroundColor Green
        return $true
    }
    Write-Host "  START gradlew test (missing: $($missing -join ', ')) ..." -ForegroundColor Cyan
    $tasks = $missing | ForEach-Object { ":${_}:test" }
    & $gradlew @tasks "--continue" "--no-daemon"
    $still = @()
    foreach ($m in $mods) {
        $p = Join-Path $Root "$m\build\reports\tests\test\index.html"
        if (-not (Test-Path $p)) { $still += $m }
    }
    if ($still.Count -eq 0) {
        Write-Host "  OK test reports" -ForegroundColor Green
        return $true
    }
    Write-Host "  FAIL test reports still missing: $($still -join ', ')" -ForegroundColor Red
    return $false
}

Ensure-Javadoc | Out-Null
if (-not $SkipTestReports) {
    Ensure-TestReports | Out-Null
} else {
    Write-Host "  SKIP test reports (-SkipTestReports)" -ForegroundColor DarkGray
}

# Demo 快捷面板完整對照（學習文件／觀測／後端）— LOOP 必須全綠
Write-Host ""
Write-Host "== Verify probe（對齊 Demo 快捷） ==" -ForegroundColor Cyan
$probe = @(
    # 前端（Vite）
    "http://localhost:5173/login",
    "http://localhost:5173/trade",
    "http://localhost:5173/portal",
    "http://localhost:5173/portal/audit",
    "http://localhost:5173/blueprint",
    "http://localhost:5173/demo/risk-check.html",
    "http://localhost:5173/demo/account-me.html",
    # Order
    "http://localhost:8081/actuator/health",
    "http://localhost:8081/swagger-ui/index.html",
    "http://localhost:8081/v3/api-docs",
    "http://localhost:8081/h2-console/",
    "http://localhost:8081/actuator/prometheus",
    # Risk / 其他後端
    "http://localhost:8082/actuator/health",
    "http://localhost:8080/actuator/health",
    "http://localhost:8083/actuator/health",
    "http://localhost:8084/actuator/health",
    # Docs / Javadoc / 測試導覽
    "http://127.0.0.1:5500/docs/index.html",
    "http://127.0.0.1:5500/docs/portals/demo-flow.html",
    "http://127.0.0.1:5500/docs/portals/handbook.html",
    "http://127.0.0.1:5500/docs/portals/swagger.html",
    "http://127.0.0.1:5500/docs/portals/codeGraphic.html",
    "http://127.0.0.1:5500/docs/javadoc/index.html",
    "http://127.0.0.1:5500/docs/portals/test-reports.html",
    "http://127.0.0.1:5500/order-service/build/reports/tests/test/index.html",
    "http://127.0.0.1:5500/risk-service/build/reports/tests/test/index.html"
)
if (-not $SkipDocker) {
    $probe += @(
        "http://localhost:3000/login",
        "http://localhost:9090/-/healthy"
    )
}
if (-not $SkipLocust) {
    $probe += @("http://localhost:8089/")
}
if (-not $SkipTestReports) {
    $probe += @(
        "http://127.0.0.1:5500/account-service/build/reports/tests/test/index.html",
        "http://127.0.0.1:5500/gateway/build/reports/tests/test/index.html",
        "http://127.0.0.1:5500/job-service/build/reports/tests/test/index.html"
    )
}

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
    Write-Host "LOOP incomplete: $fail failing（Demo 快捷仍有 DOWN）" -ForegroundColor Red
    Write-Host "下一步: .\scripts\doctor-demo.ps1 -Fix   或看 logs\ensure-from-order.*.log" -ForegroundColor Yellow
    exit 1
}
Write-Host "LOOP OK: Demo 快捷目標皆可連" -ForegroundColor Green
exit 0

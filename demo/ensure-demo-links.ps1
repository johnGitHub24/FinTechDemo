# FinTechDemo ensure-demo-links.ps1
# LOOP: start services until Demo shortcut URLs respond.
# Encoding: UTF-8 with BOM (Windows PowerShell 5.x)
#
# 【Root cause】Windows 上 Start-Process 直接跑 .bat／npm.cmd + RedirectStandard*
# 常會「行程沒起來、log 0 byte」。一律改 cmd.exe /c + 檔案重新導向。
# 【概念】localhost 時好時壞＝本機行程沒常駐；kind Pod ≠ :5173。
# 【Demo 入口】Gateway UP 後 Force 重啟 Vite，VITE_API_TARGET=http://localhost:8080（經 Gateway）。
param(
    [switch]$SkipDocker,
    [switch]$SkipLocust,
    [switch]$SkipTestReports,
    [switch]$FrontendOnly,
    # Order 自動觸發：先保 Order+Risk+Vite，再補齊橫幅全部服務（Gateway/Job/Account/Docs/監控/Locust）
    [switch]$FromOrder,
    # 開啟Demo：即使 UP 也重啟 Order／Account，載入最新 yml（feign-sync）與清 H2
    [switch]$ForceRestart,
    # 覆蓋 platform-run.properties 的 ENABLE_K8S
    [switch]$EnableK8s,
    [switch]$SkipK8s
)

$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$Root = Split-Path $PSScriptRoot -Parent
Set-Location $Root
. (Join-Path $PSScriptRoot 'platform-env.ps1') -ProjectRoot $Root
$ErrorActionPreference = "Continue"
$WantK8s = -not $SkipK8s -and ($EnableK8s -or $PlatformEnableK8s)
$logs = Join-Path $Root "logs"
New-Item -ItemType Directory -Force -Path $logs | Out-Null
$gradlew = Join-Path $Root "gradlew.bat"

if ($FromOrder) {
    # Order 背景 LOOP：勿被 javadoc／全模組 test 拖死（過去常被誤認「沒自動修」）
    $SkipTestReports = $true
}

# 低 RAM 根修：清 Daemon；勿設 JAVA_TOOL_OPTIONS（會連 Gradle 一起限死 → 空 log／秒退）
$env:GRADLE_OPTS = '-Xmx512m -Dorg.gradle.daemon=false'
Remove-Item Env:JAVA_TOOL_OPTIONS -ErrorAction SilentlyContinue

function Get-FreeMb {
    $os = Get-CimInstance Win32_OperatingSystem
    return [int]($os.FreePhysicalMemory / 1024)
}

function Clear-GradleDaemons {
    # 只清常駐 GradleDaemon；千萬別殺 GradleWrapperMain（bootRun 父行程，殺了服務就死）
    Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue |
        Where-Object {
            $_.CommandLine -and
            $_.CommandLine -match 'GradleDaemon' -and
            $_.CommandLine -notmatch 'bootRun' -and
            $_.CommandLine -notmatch 'GradleWrapperMain'
        } |
        ForEach-Object {
            Write-Host "  stop gradle-daemon pid=$($_.ProcessId)" -ForegroundColor DarkGray
            Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
        }
}

function Clear-DemoHostileContainers {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { return }
    $names = @()
    try { $names = @(docker ps -a --format '{{.Names}}' 2>$null) } catch { return }
    foreach ($name in $names) {
        # 本機 Demo 清 kind 省 RAM；若本次要起 K8s（WantK8s）則勿刪，避免拆再建
        $isKind = $name -match '^(trading-local|tradingkubernetes|kind-)'
        if ($isKind -and $WantK8s) { continue }
        if ($isKind -or
            ($name -match 'kafka|zookeeper|redpanda' -and $name -notmatch 'fintech-demo-prometheus|fintech-demo-grafana')) {
            Write-Host "  docker rm -f $name (free RAM)" -ForegroundColor DarkGray
            docker rm -f $name 2>$null | Out-Null
        }
    }
}

Write-Host "== LOOP ensure-demo-links ==" -ForegroundColor Cyan
Write-Host "Root fix: cmd /c 起 gradlew／npm vite（可重試；FromOrder 拉齊橫幅全部服務）" -ForegroundColor DarkCyan
Write-Host "必開：Order+Risk+Vite；FromOrder 再補 Gateway/Account/Job/Docs/Grafana/Prom/Locust。" -ForegroundColor Yellow
Write-Host ("  free RAM before cleanup: {0} MB" -f (Get-FreeMb)) -ForegroundColor DarkCyan
Clear-GradleDaemons
Clear-DemoHostileContainers
Start-Sleep -Seconds 2
Write-Host ("  free RAM after cleanup: {0} MB" -f (Get-FreeMb)) -ForegroundColor DarkCyan
Write-Host ""

function Test-HttpOk([string]$Url) {
    try {
        $r = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
        return ($r.StatusCode -ge 200 -and $r.StatusCode -lt 500)
    } catch {
        # PS 5.1：401/403 會丟例外，但代表埠已在服務（Account/Gateway 常見）
        $resp = $_.Exception.Response
        if ($null -ne $resp) {
            try {
                $code = [int]$resp.StatusCode
                return ($code -ge 200 -and $code -lt 500)
            } catch {
                return $true
            }
        }
        return $false
    }
}

function Wait-HttpOk([string]$Url, [int]$Seconds = 120) {
    $deadline = (Get-Date).AddSeconds($Seconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-HttpOk $Url) { return $true }
        Start-Sleep -Seconds 2
    }
    return $false
}

function Clear-ZombiePort([int]$Port, [string]$HealthUrl, [switch]$Force) {
    # Force＝即使 health 仍 UP 也清埠（前端改 VITE_API_TARGET 必須重啟）
    if (-not $Force -and (Test-HttpOk $HealthUrl)) { return }
    $conns = @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
    foreach ($c in $conns) {
        $ownerPid = [int]$c.OwningProcess
        if ($ownerPid -le 0) { continue }
        Write-Host "  clear :$Port pid=$ownerPid$(if ($Force) { ' (force)' })" -ForegroundColor DarkGray
        Stop-Process -Id $ownerPid -Force -ErrorAction SilentlyContinue
    }
    Start-Sleep -Seconds $(if ($Force) { 2 } else { 1 })
}

# 【目的】可靠啟動長駐行程（Windows）。
# 【根修】寫暫時 .cmd 再 Start-Process（避免 ArgumentList 引號把 redirect 吃掉 → 空 log／秒退）。
function Start-DetachedViaCmd(
    [string]$WorkDir,
    [string]$InnerCmd,
    [string]$OutLog,
    [string]$ErrLog,
    [string[]]$EnvLines = @()
) {
    if (-not (Test-Path $WorkDir)) {
        Write-Host "  FAIL missing workdir: $WorkDir" -ForegroundColor Red
        return $false
    }
    $stamp = Get-Date -Format 'yyyyMMddHHmmssfff'
    $cmdFile = Join-Path $logs ("run-" + $stamp + ".cmd")
    $lines = @(
        '@echo off'
        "cd /d `"$WorkDir`""
    )
    foreach ($e in $EnvLines) {
        if ($e) { $lines += $e }
    }
    # >> 附加：舊 Vite 若仍鎖 log 也不會擋新行程啟動
    $lines += "$InnerCmd >> `"$OutLog`" 2>> `"$ErrLog`""
    $lines | Set-Content -Encoding ascii -Path $cmdFile
    Start-Process -FilePath $cmdFile -WorkingDirectory $WorkDir -WindowStyle Minimized | Out-Null
    return $true
}

function Ensure-BootService([string]$Name, [string]$GradleTask, [string]$HealthUrl, [int]$Port, [int]$Attempts = 2, [switch]$Force) {
    $health = $HealthUrl -replace 'localhost', '127.0.0.1'
    if ($Force) {
        Write-Host "  ForceRestart $Name :$Port ..." -ForegroundColor Yellow
        Clear-ZombiePort $Port $health -Force
    } elseif ((Test-HttpOk $health) -or (Test-HttpOk $HealthUrl)) {
        Write-Host "  OK UP $Name  $health" -ForegroundColor Green
        return $true
    }
    $outLog = Join-Path $logs "$Name.out.log"
    $errLog = Join-Path $logs "$Name.err.log"
    for ($i = 1; $i -le $Attempts; $i++) {
        $free = Get-FreeMb
        Write-Host ("  free RAM {0} MB before $Name" -f $free) -ForegroundColor DarkCyan
        if ($free -lt 450) {
            Write-Host "  RAM low — clear GradleDaemon then retry" -ForegroundColor Yellow
            Clear-GradleDaemons
            Start-Sleep -Seconds 2
        }
        Clear-ZombiePort $Port $health
        Write-Host "  START $Name ($GradleTask) try $i/$Attempts ..." -ForegroundColor Cyan
        $stamp = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
        "$( $stamp ) START $GradleTask" | Set-Content -Encoding utf8 $outLog
        # 勿把含空白的 -Dspring-boot.run.jvmArguments 塞進 cmd（會被拆壞 → 空 log／^C）
        $inner = "`"$gradlew`" $GradleTask --no-daemon"
        if (-not (Start-DetachedViaCmd -WorkDir $Root -InnerCmd $inner -OutLog $outLog -ErrLog $errLog)) {
            continue
        }
        if ((Wait-HttpOk $health 300) -or (Wait-HttpOk $HealthUrl 15)) {
            Write-Host "  OK UP $Name  $health" -ForegroundColor Green
            Start-Sleep -Seconds 3
            return $true
        }
        Write-Host "  WARN $Name not ready on try $i — tail logs\$Name.err.log" -ForegroundColor Yellow
        if (Test-Path $errLog) {
            Get-Content $errLog -Tail 12 -ErrorAction SilentlyContinue | ForEach-Object {
                Write-Host "    $_" -ForegroundColor DarkRed
            }
        }
        Clear-GradleDaemons
        Start-Sleep -Seconds 2
    }
    Write-Host "  FAIL $Name not ready after $Attempts tries" -ForegroundColor Red
    return $false
}

function Ensure-Frontend([int]$Attempts = 2, [switch]$ViaGateway, [switch]$Force) {
    $loginUrl = "http://127.0.0.1:5173/login"
    # Demo 預設經 Gateway；Force＝Gateway 就緒後重啟 Vite 讓環境變數生效
    $apiTarget = if ($ViaGateway) { 'http://localhost:8080' } else { 'http://localhost:8081' }
    if (-not $Force -and ((Test-HttpOk $loginUrl) -or (Test-HttpOk "http://localhost:5173/login") -or (Test-HttpOk "http://127.0.0.1:5173/"))) {
        Write-Host "  OK frontend :5173" -ForegroundColor Green
        return $true
    }
    $feDir = Join-Path $Root "frontend"
    $nodeCmd = Get-Command node.exe -ErrorAction SilentlyContinue
    $npmCmd = Get-Command npm.cmd -ErrorAction SilentlyContinue
    if (-not $nodeCmd -and -not $npmCmd) {
        Write-Host "  FAIL frontend — node.exe / npm.cmd not found" -ForegroundColor Red
        return $false
    }
    if (-not (Test-Path (Join-Path $feDir "node_modules"))) {
        if (-not $npmCmd) {
            Write-Host "  FAIL frontend — node_modules missing and npm.cmd not found" -ForegroundColor Red
            return $false
        }
        Write-Host "  npm install (frontend) ..." -ForegroundColor Cyan
        Push-Location $feDir
        & $npmCmd.Source install
        Pop-Location
    }
    $runStamp = Get-Date -Format 'yyyyMMddHHmmss'
    $outLog = Join-Path $logs ("frontend-$runStamp.out.log")
    $errLog = Join-Path $logs ("frontend-$runStamp.err.log")
    $viteJs = Join-Path $feDir "node_modules\vite\bin\vite.js"
    $envLines = @(
        "set VITE_API_TARGET=$apiTarget"
    )
    for ($i = 1; $i -le $Attempts; $i++) {
        # Force／重試必須殺掉仍 UP 的舊 Vite，否則 VITE_API_TARGET 不會生效
        Clear-ZombiePort 5173 $loginUrl -Force:($Force -or $i -gt 1)
        Write-Host ("  START frontend try {0}/{1}  VITE_API_TARGET={2} ..." -f $i, $Attempts, $apiTarget) -ForegroundColor Cyan
        $stamp = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
        try {
            "$( $stamp ) START frontend VITE_API_TARGET=$apiTarget" | Set-Content -Encoding utf8 -Path $outLog -ErrorAction Stop
        } catch {
            Write-Host "  WARN log write skipped (file locked) — $($_.Exception.Message)" -ForegroundColor DarkYellow
        }
        $started = $false
        # 優先 npm run dev；失敗再直接 node + vite.js
        if ($npmCmd) {
            $inner = "`"$($npmCmd.Source)`" run dev -- --host 0.0.0.0 --port 5173 --strictPort"
            $started = Start-DetachedViaCmd -WorkDir $feDir -InnerCmd $inner -OutLog $outLog -ErrLog $errLog -EnvLines $envLines
        } elseif ($nodeCmd -and (Test-Path $viteJs)) {
            $inner = "`"$($nodeCmd.Source)`" `"$viteJs`" --host 0.0.0.0 --port 5173 --strictPort"
            $started = Start-DetachedViaCmd -WorkDir $feDir -InnerCmd $inner -OutLog $outLog -ErrLog $errLog -EnvLines $envLines
        }
        if (-not $started) { continue }
        if ((Wait-HttpOk $loginUrl 120) -or (Wait-HttpOk "http://127.0.0.1:5173/" 15) -or (Wait-HttpOk "http://localhost:5173/login" 15)) {
            if ($ViaGateway) {
                Write-Host "  OK frontend :5173 → API Gateway :8080" -ForegroundColor Green
            } else {
                Write-Host "  OK frontend :5173 → API Order :8081" -ForegroundColor Green
            }
            return $true
        }
        Write-Host "  WARN frontend not ready on try $i — tail $errLog" -ForegroundColor Yellow
        if (Test-Path $errLog) {
            Get-Content $errLog -Tail 20 -ErrorAction SilentlyContinue | ForEach-Object {
                Write-Host "    $_" -ForegroundColor DarkRed
            }
        }
    }
    Write-Host "  FAIL frontend after $Attempts tries — logs\frontend.*.log" -ForegroundColor Red
    return $false
}

function Ensure-Docs {
    if (Test-HttpOk "http://127.0.0.1:5500/docs/index.html") {
        Write-Host "  OK docs :5500" -ForegroundColor Green
        return $true
    }
    Write-Host "  START serve-docs ..." -ForegroundColor Cyan
    Start-Process -FilePath "powershell.exe" `
        -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", (Join-Path $Root "docs\tools\serve-docs.ps1")) `
        -WorkingDirectory $Root -WindowStyle Minimized | Out-Null
    if (Wait-HttpOk "http://127.0.0.1:5500/docs/index.html" 45) {
        Write-Host "  OK docs :5500" -ForegroundColor Green
        return $true
    }
    Write-Host "  FAIL docs" -ForegroundColor Red
    return $false
}

function Write-TradeReadyBanner {
    $order = Test-HttpOk "http://127.0.0.1:8081/actuator/health"
    $risk = Test-HttpOk "http://127.0.0.1:8082/actuator/health"
    $vite = (Test-HttpOk "http://127.0.0.1:5173/login") -or (Test-HttpOk "http://localhost:5173/login")
    Write-Host ""
    Write-Host "======== TRADE-READY CHECK ========" -ForegroundColor Cyan
    Write-Host ("  Order {0}  Risk {1}  Vite {2}" -f `
        ($(if ($order) { '[UP]' } else { '[DOWN]' })), `
        ($(if ($risk) { '[UP]' } else { '[DOWN]' })), `
        ($(if ($vite) { '[UP]' } else { '[DOWN]' })))
    if ($order -and $risk -and $vite) {
        Write-Host "  TRADE-READY OK → http://127.0.0.1:5173/login  (trader1 / password)" -ForegroundColor Green
        return $true
    }
    Write-Host "  TRADE-READY FAIL — 最短可成交尚未齊（先修 Risk/Vite）" -ForegroundColor Red
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

# ---- P0：可成交（必須先做完；可重試到 Order+Risk+Vite 全 UP）----
function Invoke-TradeReadyEnsure([int]$BootAttempts = 3, [int]$FrontendAttempts = 3) {
    Write-Host "== P0 trade-ready（Order + Risk + Vite）==" -ForegroundColor Cyan
    $okRisk = Ensure-BootService "risk-service" ":risk-service:bootRun" "http://127.0.0.1:8082/actuator/health" 8082 $BootAttempts
    $okVite = Ensure-Frontend $FrontendAttempts
    # ForceRestart：載入最新碼／清 H2（開啟Demo 預設開）
    $okOrder = Ensure-BootService "order-service" ":order-service:bootRun" "http://127.0.0.1:8081/actuator/health" 8081 1 -Force:$ForceRestart
    $bannerOk = Write-TradeReadyBanner
    return ($okRisk -and $okVite -and $okOrder -and $bannerOk)
}

$tradeOk = $false
$rounds = if ($FromOrder) { 3 } else { 1 }
for ($round = 1; $round -le $rounds; $round++) {
    if ($rounds -gt 1) {
        Write-Host ""
        Write-Host ("== trade-ready round {0}/{1} ==" -f $round, $rounds) -ForegroundColor Cyan
    }
    $tradeOk = Invoke-TradeReadyEnsure
    if ($tradeOk) { break }
    if ($round -lt $rounds) {
        Write-Host "  trade-ready 未齊，5 秒後重試…" -ForegroundColor Yellow
        Start-Sleep -Seconds 5
    }
}

if (-not $tradeOk) {
    Write-Host "WARN trade-ready 未齊 — 仍繼續補齊橫幅其餘服務" -ForegroundColor Yellow
}

# ---- P1：橫幅其餘後端（FromOrder 也要拉；依序避免一次搶爆 RAM）----
Write-Host ""
Write-Host "== P1 banner backends（Account / Gateway / Job / Docs）==" -ForegroundColor Cyan
Ensure-BootService "account-service" ":account-service:bootRun" "http://127.0.0.1:8084/actuator/health" 8084 2 -Force:$ForceRestart | Out-Null
$gwUp = Ensure-BootService "gateway" ":gateway:bootRun" "http://127.0.0.1:8080/actuator/health" 8080 2
Ensure-BootService "job-service" ":job-service:bootRun" "http://127.0.0.1:8083/actuator/health" 8083 2 | Out-Null
Ensure-Docs | Out-Null

# Gateway 就緒後重啟 Vite，讓 Demo 預設走統一入口（不必再手動設 VITE_API_TARGET）
Write-Host ""
Write-Host "== P1b frontend API 入口 ==" -ForegroundColor Cyan
if ($gwUp) {
    Write-Host "  Gateway UP → 重啟 Vite，/api 經 :8080" -ForegroundColor Cyan
    Ensure-Frontend -Attempts 2 -ViaGateway -Force | Out-Null
} else {
    Write-Host "  WARN Gateway DOWN → 前端維持直連 Order :8081（最短路徑）" -ForegroundColor Yellow
}

function Ensure-K8sDemo {
    if (-not $WantK8s) {
        Write-Host "  SKIP K8s — 本機 Demo；要叢集請雙擊 開啟K8sDemo.cmd" -ForegroundColor DarkGray
        return $true
    }
    Write-Host ""
    Write-Host "== ENABLE_K8S=true → kind fintech-demo ==" -ForegroundColor Magenta
    Use-PlatformKube
    $running = 0
    try {
        $rows = kubectl -n $PlatformK8sNamespace get pods --no-headers 2>$null
        if ($rows) {
            $running = @($rows | Where-Object { $_ -match '\sRunning\s' }).Count
        }
    } catch { }
    if ($running -ge 4) {
        Write-Host ("  OK {0} already {1} Running" -f $PlatformK8sNamespace, $running) -ForegroundColor Green
        return $true
    }
    Write-Host "  START demo/start-k8s-demo.ps1 (需 Docker Desktop Ready，較久) ..." -ForegroundColor Cyan
    & (Join-Path $PSScriptRoot 'start-k8s-demo.ps1')
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  FAIL start-k8s-demo.ps1 exit=$LASTEXITCODE" -ForegroundColor Red
        return $false
    }
    Write-Host "  OK K8s Demo（前端仍連本機 :8081，除非 port-forward）" -ForegroundColor Green
    return $true
}

Ensure-K8sDemo | Out-Null

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
        Write-Host "  START docker compose monitoring (prometheus grafana only) ..." -ForegroundColor Cyan
        & docker compose --profile monitoring up -d prometheus grafana
        Wait-HttpOk "http://localhost:9090/-/healthy" 120 | Out-Null
        Wait-HttpOk "http://localhost:3000/login" 120 | Out-Null
    }

    $needLocal = -not ((Test-HttpOk "http://localhost:9090/-/healthy") -and (Test-HttpOk "http://localhost:3000/login"))
    if ($needLocal) {
        if (-not $dockerOk) {
            Write-Host "  SKIP monitoring (Docker not Ready). Use: docker compose --profile monitoring up -d" -ForegroundColor Yellow
            return $false
        }
        Write-Host "  WARN monitoring not UP after compose; check docker logs" -ForegroundColor Yellow
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
    } elseif ((Get-Command py -ErrorAction SilentlyContinue) -or (@(Get-Command python -All -ErrorAction SilentlyContinue) | Where-Object { $_.Source -and $_.Source -notmatch '\\WindowsApps\\' })) {
        Write-Host "  START locust Web UI ..." -ForegroundColor Cyan
        $pyLaunch = "py -3"
        if (-not (Get-Command py -ErrorAction SilentlyContinue)) { $pyLaunch = "python" }
        $load = Join-Path $Root "loadtest"
        Push-Location $load
        cmd /c "$pyLaunch -m pip install -r requirements.txt -q"
        Pop-Location
        $outLog = Join-Path $logs "locust.out.log"
        $errLog = Join-Path $logs "locust.err.log"
        $inner = "$pyLaunch -m locust -f locustfile.py --host http://localhost:8081 --web-host 0.0.0.0 --web-port 8089"
        Start-DetachedViaCmd -WorkDir $load -InnerCmd $inner -OutLog $outLog -ErrLog $errLog
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
    if ($FromOrder) {
        Write-Host "  SKIP javadoc (FromOrder — 不擋 trade-ready)" -ForegroundColor DarkGray
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
    Write-Host "  SKIP test reports (-SkipTestReports / FromOrder)" -ForegroundColor DarkGray
}

Write-Host ""
Write-Host "== Verify probe ==" -ForegroundColor Cyan
$tradeProbe = @(
    "http://127.0.0.1:8081/actuator/health",
    "http://127.0.0.1:8082/actuator/health",
    "http://127.0.0.1:5173/login"
)
# 啟動橫幅列到的服務（FromOrder 也視為必 UP；Skip* 才略過）
$bannerProbe = @(
    "http://127.0.0.1:8080/actuator/health",
    "http://127.0.0.1:8083/actuator/health",
    "http://127.0.0.1:8084/actuator/health",
    "http://127.0.0.1:5500/docs/index.html"
)
if (-not $SkipDocker) {
    $bannerProbe += @("http://localhost:3000/login", "http://localhost:9090/-/healthy")
}
if (-not $SkipLocust) {
    $bannerProbe += @("http://localhost:8089/")
}
$niceProbe = @(
    "http://localhost:5173/trade",
    "http://localhost:5173/blueprint",
    "http://localhost:8081/swagger-ui/index.html"
)

$tradeFail = 0
foreach ($u in $tradeProbe) {
    if (Test-HttpOk $u) {
        Write-Host "  OK  $u" -ForegroundColor Green
    } else {
        $tradeFail++
        Write-Host "  FAIL(trade) $u" -ForegroundColor Red
    }
}
$bannerFail = 0
foreach ($u in $bannerProbe) {
    if (Test-HttpOk $u) {
        Write-Host "  OK  $u" -ForegroundColor Green
    } else {
        $bannerFail++
        Write-Host "  FAIL(banner) $u" -ForegroundColor Red
    }
}
$niceFail = 0
foreach ($u in $niceProbe) {
    if (Test-HttpOk $u) {
        Write-Host "  OK  $u" -ForegroundColor Green
    } else {
        $niceFail++
        Write-Host "  DOWN(nice) $u" -ForegroundColor Yellow
    }
}

Write-TradeReadyBanner | Out-Null

if ($tradeFail -gt 0) {
    Write-Host "LOOP FAIL: trade-ready 未齊 ($tradeFail)。下一步: .\demo\doctor-demo.ps1 -Fix" -ForegroundColor Red
    exit 1
}
if ($FromOrder -and $bannerFail -gt 0) {
    Write-Host "LOOP FAIL (FromOrder): 橫幅服務仍 DOWN=$bannerFail（Gateway/Job/Account/Docs/監控/Locust）。下一步: .\demo\doctor-demo.ps1 -Fix" -ForegroundColor Red
    exit 1
}
if ($bannerFail -gt 0 -or $niceFail -gt 0) {
    Write-Host ("LOOP OK(trade-ready): 可成交；banner DOWN={0} nice DOWN={1}" -f $bannerFail, $niceFail) -ForegroundColor Green
} else {
    Write-Host "LOOP OK: trade-ready + 橫幅全部可連" -ForegroundColor Green
}
exit 0

#Requires -Version 5.1
<#
.SYNOPSIS
  Demo 一鍵就緒：優先保證 Order+Risk+Vite（可成交），再依序補齊其餘服務。

.DESCRIPTION
  Root cause（本機曾反覆炸）：
  1) RAM 不足時同時起多個 Gradle Daemon／bootRun → insufficient memory／服務全 DOWN
  2) 重啟時殘留埠／半死 JVM／舊 Vite → 腳本誤判「已 UP」或新行程搶不到埠
  3) 背景殘留 kind／TradingKubernetes 容器吃 RAM（即使你沒主動開 Demo）

  本腳本：先清 Demo 埠與敵對容器 → 檢查記憶體 → 依序啟動 → 每起一個等 health。

.PARAMETER Full
  起齊 gateway／account／job（橫幅可選連結才會通）。開啟Demo.cmd 預設帶此參數。

.PARAMETER ServeDocs
  背景起文件靜態伺服 :5500（學習文件連結才會通）。開啟Demo.cmd 預設帶此參數。

.PARAMETER SkipClean
  跳過埠清理（很少用）。

.PARAMETER ForceClean
  強制殺掉所有 Demo 相關行程再重起（含已健康的 IntelliJ bootRun）。
  預設改為「只清半殘」：health 已 UP 的埠保留，避免每次重開都砍掉 IDE 已起的 Order。

.EXAMPLE
  .\demo\start-demo-ready.ps1
  .\demo\start-demo-ready.ps1 -Full -ServeDocs -FreeKind -OpenBrowser
#>
param(
    [switch] $Full,
    [switch] $ServeDocs,
    [switch] $FreeKind,
    [switch] $OpenBrowser,
    [switch] $SkipClean,
    [switch] $ForceClean,
    [int] $MinFreeMb = 600
)

$ErrorActionPreference = 'Continue'
$Root = Split-Path $PSScriptRoot -Parent
Set-Location $Root
$logs = Join-Path $Root 'logs'
New-Item -ItemType Directory -Force -Path $logs | Out-Null
$gradlew = Join-Path $Root 'gradlew.bat'
$env:GRADLE_OPTS = '-Xmx512m'
$DemoPorts = @(5173, 8080, 8081, 8082, 8083, 8084)

function Get-FreeMb {
    $os = Get-CimInstance Win32_OperatingSystem
    return [int]($os.FreePhysicalMemory / 1024)
}

function Test-HttpOk([string] $Url) {
    try {
        $r = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
        return ($r.StatusCode -ge 200 -and $r.StatusCode -lt 500)
    } catch {
        $resp = $_.Exception.Response
        if ($null -ne $resp) {
            try {
                $code = [int]$resp.StatusCode
                return ($code -ge 200 -and $code -lt 500)
            } catch { return $true }
        }
        return $false
    }
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

function Stop-PidSafe([int] $ProcessId, [string] $Reason) {
    if ($ProcessId -le 0) { return }
    Write-Host "  stop pid=$ProcessId ($Reason)" -ForegroundColor DarkGray
    Stop-Process -Id $ProcessId -Force -ErrorAction SilentlyContinue
}

function Clear-DemoHostileContainers {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        Write-Host '  docker 不在 PATH，略過清容器' -ForegroundColor Yellow
        return
    }
    Write-Host '釋放 kind／TradingKubernetes／閒置容器記憶體…' -ForegroundColor Cyan
    $names = @()
    try {
        $names = @(docker ps -a --format '{{.Names}}' 2>$null)
    } catch {
        Write-Host '  docker 無法列出容器（Desktop 未就緒？）' -ForegroundColor Yellow
        return
    }
    foreach ($name in $names) {
        if ($name -match '^(trading-local|tradingkubernetes|kind-)') {
            Write-Host "  docker rm -f $name" -ForegroundColor DarkGray
            docker rm -f $name 2>$null | Out-Null
        }
    }
    # 額外：停掉非 FinTechDemo 的 compose 專案 kafka／postgres（常見吃 RAM 兇手）
    foreach ($name in $names) {
        if ($name -match 'kafka|zookeeper|redpanda' -and $name -notmatch 'fintech') {
            Write-Host "  docker stop $name (釋放 RAM)" -ForegroundColor DarkGray
            docker stop $name 2>$null | Out-Null
        }
    }
    Start-Sleep -Seconds 2
    Write-Mem
}

function Clear-GradleDaemons {
    Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue |
        Where-Object { $_.CommandLine -match 'GradleDaemon' } |
        ForEach-Object { Stop-PidSafe $_.ProcessId 'GradleDaemon' }
}

function Get-PortHealthUrl([int] $Port) {
    switch ($Port) {
        5173 { return 'http://127.0.0.1:5173/login' }
        8080 { return 'http://127.0.0.1:8080/actuator/health' }
        8081 { return 'http://127.0.0.1:8081/actuator/health' }
        8082 { return 'http://127.0.0.1:8082/actuator/health' }
        8083 { return 'http://127.0.0.1:8083/actuator/health' }
        8084 { return 'http://127.0.0.1:8084/actuator/health' }
        default { return $null }
    }
}

function Clear-DemoPorts {
    param([switch] $Force)

    if ($Force) {
        Write-Host '強制清理全部 Demo 行程（含已 UP）…' -ForegroundColor Yellow
    } else {
        Write-Host '智能清理：保留 health 已 UP 的服務，只砍半殘佔埠…' -ForegroundColor Cyan
    }

    if ($Force) {
        Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue |
            Where-Object {
                $_.CommandLine -and (
                    $_.CommandLine -match 'FinTechDemo' -or
                    $_.CommandLine -match 'fintech\.demo' -or
                    $_.CommandLine -match 'com\.fintech\.demo' -or
                    $_.CommandLine -match ':risk-service:bootRun|:order-service:bootRun|:gateway:bootRun|:account-service:bootRun|:job-service:bootRun'
                )
            } |
            ForEach-Object { Stop-PidSafe $_.ProcessId 'FinTechDemo java' }

        Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
            Where-Object {
                $_.Name -match '^(node|cmd)\.exe$' -and $_.CommandLine -and (
                    $_.CommandLine -match 'FinTechDemo[\\/]+frontend' -or
                    ($_.CommandLine -match 'vite' -and $_.CommandLine -match '5173')
                )
            } |
            ForEach-Object { Stop-PidSafe $_.ProcessId 'Vite/node' }
    }

    foreach ($port in $DemoPorts) {
        $healthUrl = Get-PortHealthUrl $port
        $healthy = $false
        if ($healthUrl -and -not $Force) {
            $healthy = Test-HttpOk $healthUrl
            if (-not $healthy -and $port -eq 5173) {
                $healthy = Test-HttpOk 'http://127.0.0.1:5173/'
            }
        }
        if ($healthy) {
            Write-Host "  KEEP :$port (health UP)" -ForegroundColor Green
            continue
        }

        $conns = @(Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue)
        if ($conns.Count -eq 0) { continue }

        foreach ($c in $conns) {
            $ownerPid = [int]$c.OwningProcess
            if ($ownerPid -le 0) { continue }
            $proc = Get-CimInstance Win32_Process -Filter "ProcessId=$ownerPid" -ErrorAction SilentlyContinue
            $cmd = if ($proc) { $proc.CommandLine } else { '' }
            # 半殘：佔埠但 health 不通 → 一律清掉，讓 Ensure-* 能重起
            if ($Force -or -not $healthy) {
                if ($cmd -match 'FinTechDemo|fintech\.demo|bootRun|gradle|vite|npm|jdk|java' -or -not $cmd -or $Force) {
                    Stop-PidSafe $ownerPid "zombie port $port"
                } else {
                    Write-Host "  WARN port $port 被其他行程佔用 pid=$ownerPid — 請手動處理" -ForegroundColor Yellow
                }
            }
        }
    }

    Start-Sleep -Seconds 2
    Write-Mem
}

function Ensure-Boot([string] $Name, [string] $Task, [string] $Health, [string[]] $ExtraArgs = @(), [switch] $Required) {
    if (Test-HttpOk $Health) {
        Write-Host "  OK UP $Name (已在跑，略過重起)" -ForegroundColor Green
        return $true
    }
    $free = Get-FreeMb
    if ($free -lt $MinFreeMb) {
        if ($Required) {
            Write-Host "  WARN $Name — 可用 RAM ${free}MB < ${MinFreeMb}MB，仍嘗試啟動（必開服務）…" -ForegroundColor Yellow
        } else {
            Write-Host "  SKIP $Name — 可用 RAM ${free}MB < ${MinFreeMb}MB。請關瀏覽器分頁／Docker 未用容器後重跑 -FreeKind" -ForegroundColor Yellow
            return $false
        }
    }
    Write-Host "  START $Name (sequential, no parallel gradle) ..." -ForegroundColor Cyan
    Write-Mem
    $outLog = Join-Path $logs "$Name.out.log"
    $errLog = Join-Path $logs "$Name.err.log"
    Remove-Item $outLog, $errLog -Force -ErrorAction SilentlyContinue
    $argStr = "$Task --no-daemon"
    if ($ExtraArgs -and $ExtraArgs.Count -gt 0) {
        $argStr = $argStr + ' ' + ($ExtraArgs -join ' ')
    }
    # Windows 根修復：勿對 .bat 用 Start-Process + RedirectStandard*（常空 log／不起程）
    $inner = "`"$gradlew`" $argStr"
    $wrapped = "$inner > `"$outLog`" 2> `"$errLog`""
    Start-Process -FilePath 'cmd.exe' -ArgumentList @('/c', $wrapped) `
        -WorkingDirectory $Root -WindowStyle Minimized | Out-Null
    if (Wait-HttpOk $Health 240) {
        Write-Host "  OK UP $Name" -ForegroundColor Green
        Start-Sleep -Seconds 2
        return $true
    }
    Write-Host "  FAIL $Name — logs\$Name.*.log" -ForegroundColor Red
    if (Test-Path $errLog) {
        Get-Content $errLog -Tail 20 -ErrorAction SilentlyContinue | ForEach-Object {
            Write-Host "    $_" -ForegroundColor DarkRed
        }
    }
    return $false
}

function Ensure-Vite {
    if (Test-HttpOk 'http://127.0.0.1:5173/login' -or (Test-HttpOk 'http://127.0.0.1:5173/')) {
        Write-Host "  OK UP frontend :5173" -ForegroundColor Green
        return $true
    }

    $frontend = Join-Path $Root 'frontend'
    $npmCmd = Get-Command npm.cmd -ErrorAction SilentlyContinue
    if (-not $npmCmd) {
        Write-Host '  FAIL frontend — 找不到 npm.cmd（請安裝 Node.js 並開新終端）' -ForegroundColor Red
        return $false
    }
    $npm = $npmCmd.Source
    if (-not (Test-Path (Join-Path $frontend 'node_modules'))) {
        Write-Host '  npm install（首次）...' -ForegroundColor Cyan
        & $npm install --prefix $frontend
        if ($LASTEXITCODE -ne 0) {
            Write-Host '  FAIL frontend — npm install 失敗' -ForegroundColor Red
            return $false
        }
    }

    Write-Host '  START frontend ...' -ForegroundColor Cyan
    $outLog = Join-Path $logs 'frontend.out.log'
    $errLog = Join-Path $logs 'frontend.err.log'
    # Windows 根修復：cmd /c + 完整 npm 路徑；綁 0.0.0.0 避免 localhost→::1 假 DOWN
    $inner = "`"$npm`" run dev -- --host 0.0.0.0 --port 5173 --strictPort"
    $wrapped = "$inner > `"$outLog`" 2> `"$errLog`""
    Start-Process -FilePath 'cmd.exe' -ArgumentList @('/c', $wrapped) `
        -WorkingDirectory $frontend -WindowStyle Minimized | Out-Null

    if (Wait-HttpOk 'http://127.0.0.1:5173/login' 90) {
        Write-Host '  OK UP frontend' -ForegroundColor Green
        return $true
    }
    if (Wait-HttpOk 'http://127.0.0.1:5173/' 15) {
        Write-Host '  OK UP frontend (root)' -ForegroundColor Green
        return $true
    }
    if (Wait-HttpOk 'http://localhost:5173/login' 15) {
        Write-Host '  OK UP frontend (localhost)' -ForegroundColor Green
        return $true
    }
    Write-Host '  FAIL frontend — logs\frontend.*.log' -ForegroundColor Red
    if (Test-Path $errLog) {
        Get-Content $errLog -Tail 30 -ErrorAction SilentlyContinue | ForEach-Object {
            Write-Host "    $_" -ForegroundColor DarkRed
        }
    }
    return $false
}

function Ensure-Docs {
    if (Test-HttpOk 'http://127.0.0.1:5500/docs/index.html') {
        Write-Host '  OK UP docs :5500' -ForegroundColor Green
        return $true
    }
    $py = Get-Command python -ErrorAction SilentlyContinue
    if (-not $py) {
        Write-Host '  SKIP docs — 找不到 python（.\docs\tools\serve-docs.ps1 需 Python）' -ForegroundColor Yellow
        return $false
    }
    Write-Host '  START docs :5500 ...' -ForegroundColor Cyan
    $outLog = Join-Path $logs 'docs.out.log'
    $errLog = Join-Path $logs 'docs.err.log'
    Start-Process -FilePath $py.Source -ArgumentList @('-m', 'http.server', '5500', '--bind', '127.0.0.1') `
        -WorkingDirectory $Root -WindowStyle Minimized `
        -RedirectStandardOutput $outLog `
        -RedirectStandardError $errLog
    if (Wait-HttpOk 'http://127.0.0.1:5500/docs/index.html' 30) {
        Write-Host '  OK UP docs' -ForegroundColor Green
        return $true
    }
    Write-Host '  FAIL docs :5500' -ForegroundColor Red
    return $false
}

Write-Host ''
Write-Host '======== FinTechDemo Demo 啟動 ========' -ForegroundColor Cyan
Write-Host '目標：橫幅連結可點＝對應服務必須 UP。最短＝Risk+Order+Vite；-Full＋-ServeDocs 才齊。' -ForegroundColor Yellow
Write-Mem

if ($FreeKind) {
    Clear-DemoHostileContainers
}

Clear-GradleDaemons
Start-Sleep -Seconds 1

if (-not $SkipClean) {
    Clear-DemoPorts -Force:$ForceClean
}

Write-Host ''
Write-Host '== Demo 最短可成交（必開）==' -ForegroundColor Cyan
$okRisk = Ensure-Boot 'risk-service' ':risk-service:bootRun' 'http://127.0.0.1:8082/actuator/health' -Required
$okOrder = Ensure-Boot 'order-service' ':order-service:bootRun' 'http://127.0.0.1:8081/actuator/health' -Required
$okVite = Ensure-Vite

$okGateway = $false
$okAccount = $false
$okJob = $false
if ($Full) {
    Write-Host ''
    Write-Host '== Demo 完整敘事（依序；橫幅可選連結）==' -ForegroundColor Cyan
    $okGateway = Ensure-Boot 'gateway' ':gateway:bootRun' 'http://127.0.0.1:8080/actuator/health'
    $okAccount = Ensure-Boot 'account-service' ':account-service:bootRun' 'http://127.0.0.1:8084/actuator/health'
    $okJob = Ensure-Boot 'job-service' ':job-service:bootRun' 'http://127.0.0.1:8083/actuator/health'
}

$okDocs = $false
if ($ServeDocs) {
    Write-Host ''
    Write-Host '== 學習文件 :5500 ==' -ForegroundColor Cyan
    $okDocs = Ensure-Docs
}

Write-Host ''
Write-Host '== Demo 檢查清單 ==' -ForegroundColor Cyan
$checks = @(
    @{ n = 'Vite'; u = 'http://127.0.0.1:5173/login'; must = $true },
    @{ n = 'Order'; u = 'http://127.0.0.1:8081/actuator/health'; must = $true },
    @{ n = 'Risk'; u = 'http://127.0.0.1:8082/actuator/health'; must = $true },
    @{ n = 'Gateway'; u = 'http://127.0.0.1:8080/actuator/health'; must = $Full },
    @{ n = 'Account'; u = 'http://127.0.0.1:8084/actuator/health'; must = $Full },
    @{ n = 'Job'; u = 'http://127.0.0.1:8083/actuator/health'; must = $Full },
    @{ n = 'Docs'; u = 'http://127.0.0.1:5500/docs/index.html'; must = $ServeDocs }
)
$failMust = 0
foreach ($c in $checks) {
    $ok = Test-HttpOk $c.u
    if (-not $ok -and $c.n -eq 'Vite') { $ok = Test-HttpOk 'http://127.0.0.1:5173/' }
    if ($ok) { Write-Host "  OK  $($c.n)" -ForegroundColor Green }
    else {
        $col = if ($c.must) { 'Red' } else { 'Yellow' }
        Write-Host "  DOWN $($c.n)$(if (-not $c.must) { '（未要求）' })" -ForegroundColor $col
        if ($c.must) { $failMust++ }
    }
}

Write-Host ''
Write-Mem
if ($failMust -eq 0 -and $okRisk -and $okOrder -and $okVite) {
    Write-Host 'Demo READY：http://localhost:5173/login  （trader1 / password）' -ForegroundColor Green
    if ($Full) {
        Write-Host ("  Full：Gateway={0} Account={1} Job={2}" -f $(if($okGateway){'UP'}else{'DOWN'}), $(if($okAccount){'UP'}else{'DOWN'}), $(if($okJob){'UP'}else{'DOWN'})) -ForegroundColor Cyan
    }
    if ($ServeDocs) {
        Write-Host ("  Docs：{0}  http://127.0.0.1:5500/docs/index.html" -f $(if($okDocs){'UP'}else{'DOWN'})) -ForegroundColor Cyan
    }
    if ($OpenBrowser) {
        Start-Process 'http://localhost:5173/login'
        Start-Process 'http://localhost:5173/blueprint'
        if ($okDocs) { Start-Process 'http://127.0.0.1:5500/docs/index.html' }
    }
    exit 0
}

Write-Host '必開服務尚未齊。建議：' -ForegroundColor Red
Write-Host '  雙擊 開啟Demo.cmd（已含 -Full -ServeDocs -FreeKind）' -ForegroundColor Yellow
Write-Host '  或：.\demo\start-demo-ready.ps1 -Full -ServeDocs -FreeKind -OpenBrowser' -ForegroundColor Yellow
exit 1

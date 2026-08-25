<#
.SYNOPSIS
  FinTechDemo 完整 K8s Demo：kind 叢集 -> build 映像 -> load -> apply -> 等 Pod Ready。

.DESCRIPTION
  對齊 deploy/k8s/overlays/dev（gateway + order + risk + account）。
  日常 bootRun／Compose 與本腳本擇一；RAM 不足時先停 kind 或舊 bootRun。

.PARAMETER RecreateCluster
  刪除並重建 kind trading-local（API 掛掉或 kubeconfig 漂移時）

.PARAMETER SkipBuild
  跳過 docker compose build（映像已存在時）

.PARAMETER SkipPortForward
  成功後不另開視窗做 Gateway port-forward（預設會開）

.PARAMETER SkipStopLocal
  略過「先停本機 bootRun／Vite／舊 port-forward」（預設會停，與本機 Demo 擇一）

.PARAMETER SkipFrontend
  SkipFrontend: skip Vite :5173 (default starts Vite, VITE_API_TARGET=http://127.0.0.1:18080)

.EXAMPLE
  .\demo\start-k8s-demo.ps1
  .\demo\start-k8s-demo.ps1 -RecreateCluster
  .\demo\start-k8s-demo.ps1 -SkipPortForward
  .\demo\start-k8s-demo.ps1 -SkipFrontend
#>
# Encoding: UTF-8 with BOM (Windows PowerShell 5.x)
param(
    [switch]$RecreateCluster,
    [switch]$SkipBuild,
    [switch]$SkipPortForward,
    [switch]$SkipStopLocal,
    [switch]$SkipFrontend
)

$ErrorActionPreference = 'Stop'
$Root = Split-Path $PSScriptRoot -Parent
Set-Location $Root

. (Join-Path $PSScriptRoot 'platform-env.ps1') -ProjectRoot $Root

$ClusterName = $PlatformK8sCluster
$Context = $PlatformK8sContext
$Namespace = $PlatformK8sNamespace
$Overlay = $PlatformK8sOverlay
$ToolsDir = $PlatformToolsDir
$KubeConfig = $PlatformKubeConfig
$Images = $PlatformK8sImages

function Write-Step([string]$msg) {
    Write-Host ""
    Write-Host "=== $msg ===" -ForegroundColor Cyan
}

function Stop-ListeningPort([int]$Port) {
    $conns = @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
    foreach ($c in $conns) {
        $ownerPid = [int]$c.OwningProcess
        if ($ownerPid -le 0) { continue }
        Write-Host "  stop :$Port pid=$ownerPid" -ForegroundColor DarkGray
        Stop-Process -Id $ownerPid -Force -ErrorAction SilentlyContinue
    }
}

function Stop-LocalDemoServices {
    Write-Step 'Stop local Demo (bootRun / Vite / docs / old port-forward)'
    Write-Host '  K8s vs local 808x: pick one. Free RAM and ports before kind.' -ForegroundColor DarkCyan
    $ports = @(
        $PlatformGatewayPort,
        $PlatformOrderPort,
        $PlatformRiskPort,
        $PlatformJobPort,
        $PlatformAccountPort,
        $PlatformVitePort,
        $PlatformH2TcpPort,
        9094,
        5500,
        $PlatformK8sGatewayPfLocal
    ) | Select-Object -Unique
    foreach ($p in $ports) {
        Stop-ListeningPort $p
    }
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
    Start-Sleep -Seconds 2
    Write-Host '  Local Demo ports cleared.' -ForegroundColor Green
}

function Test-HttpOk([string]$Url) {
    try {
        $r = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
        return ($r.StatusCode -ge 200 -and $r.StatusCode -lt 500)
    } catch {
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

function Wait-HttpOk([string]$Url, [int]$Seconds = 90) {
    $deadline = (Get-Date).AddSeconds($Seconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-HttpOk $Url) { return $true }
        Start-Sleep -Seconds 2
    }
    return $false
}

function Start-K8sFrontend {
    $pf = $PlatformK8sGatewayPfLocal
    $apiTarget = "http://127.0.0.1:$pf"
    $healthUrl = "$apiTarget/actuator/health"
    Write-Step "Vite frontend -> $apiTarget"
    if (-not (Wait-HttpOk $healthUrl 45)) {
        Write-Host "  WARN Gateway $healthUrl not ready - skip Vite" -ForegroundColor Yellow
        return $false
    }
    $feDir = Join-Path $Root 'frontend'
    $logs = Join-Path $Root 'logs'
    New-Item -ItemType Directory -Force -Path $logs | Out-Null
    $npmCmd = Get-Command npm.cmd -ErrorAction SilentlyContinue
    if (-not $npmCmd) {
        Write-Host '  WARN npm.cmd not found - manual: cd frontend; $env:VITE_API_TARGET=...; npm run dev' -ForegroundColor Yellow
        return $false
    }
    if (-not (Test-Path (Join-Path $feDir 'node_modules'))) {
        Write-Host '  npm install (frontend) ...' -ForegroundColor Cyan
        Push-Location $feDir
        & $npmCmd.Source install
        Pop-Location
    }
    Stop-ListeningPort $PlatformVitePort
    $stamp = Get-Date -Format 'yyyyMMddHHmmss'
    $outLog = Join-Path $logs "frontend-k8s-$stamp.out.log"
    $errLog = Join-Path $logs "frontend-k8s-$stamp.err.log"
    $cmdFile = Join-Path $logs "run-k8s-vite-$stamp.cmd"
    $lines = @(
        '@echo off',
        "cd /d `"$feDir`"",
        "set VITE_API_TARGET=$apiTarget",
        "`"$($npmCmd.Source)`" run dev -- --host 0.0.0.0 --port $PlatformVitePort --strictPort >> `"$outLog`" 2>> `"$errLog`""
    )
    $lines | Set-Content -Encoding ascii -Path $cmdFile
    Start-Process -FilePath $cmdFile -WorkingDirectory $feDir -WindowStyle Minimized | Out-Null
    $loginUrl = "http://127.0.0.1:$PlatformVitePort/login"
    if (Wait-HttpOk $loginUrl 120) {
        Write-Host "  OK frontend :$PlatformVitePort -> Gateway :$pf" -ForegroundColor Green
        Write-Host "  Login: $loginUrl  (trader1 / password)" -ForegroundColor Cyan
        return $true
    }
    Write-Host "  WARN frontend not ready - tail logs\frontend-k8s-*.err.log" -ForegroundColor Yellow
    return $false
}

function Get-DockerKindPlatform {
    Get-PlatformDockerBuildPlatform
}

function Resolve-Kind {
    if (Get-Command kind -ErrorAction SilentlyContinue) { return 'kind' }
    $walk = Split-Path $Root -Parent
    for ($i = 0; $i -lt 4; $i++) {
        $cand = Join-Path $walk 'TradingKubernetes\tools\kind.exe'
        if (Test-Path $cand) { return $cand }
        $parent = Split-Path $walk -Parent
        if (-not $parent -or $parent -eq $walk) { break }
        $walk = $parent
    }
    # 本機無 kind：下載到 demo/.tools（不污染 PATH）；架構對齊 Docker Server
    $localKind = Join-Path $Root 'demo\.tools\kind.exe'
    if (-not (Test-Path $localKind)) {
        Write-Step 'Download kind.exe'
        New-Item -ItemType Directory -Force -Path (Split-Path $localKind) | Out-Null
        $plat = Get-PlatformDockerBuildPlatform
        $kindArch = Get-PlatformKindWindowsArch
        $ver = $PlatformRun['KIND_VERSION']
        if (-not $ver) { $ver = '0.27.0' }
        $uri = "https://kind.sigs.k8s.io/dl/v$ver/kind-windows-$kindArch"
        Write-Host "  $uri" -ForegroundColor DarkGray
        Invoke-WebRequest -Uri $uri -OutFile $localKind -UseBasicParsing
    }
    return $localKind
}

function Use-DemoKube {
    Use-PlatformKube
}

function Export-And-FixKube([string]$kindCmd) {
    Export-PlatformKube -KindCmd $kindCmd -ClusterName $ClusterName
}

function Wait-Docker {
    Write-Step 'Docker engine'
    $deadline = (Get-Date).AddMinutes(5)
    while ((Get-Date) -lt $deadline) {
        $info = docker info 2>&1 | Out-String
        if ($info -match 'Server Version:\s*(\S+)') {
            Write-Host "Docker ready ($($Matches[1]))" -ForegroundColor Green
            return
        }
        Start-Sleep -Seconds 4
    }
    throw 'Docker not ready. Start Docker Desktop and wait for Ready.'
}

function Ensure-KindCluster([string]$kindCmd) {
    Write-Step "kind cluster: $ClusterName"
    $prev = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    $existing = & $kindCmd get clusters 2>&1 | ForEach-Object { "$_".Trim() } |
        Where-Object { $_ -and $_ -notmatch '(?i)no kind clusters' }
    $ErrorActionPreference = $prev
    $has = $existing -contains $ClusterName
    $recreate = [bool]$RecreateCluster

    if ($has -and -not $recreate) {
        Export-And-FixKube $kindCmd
        $ErrorActionPreference = 'Continue'
        kubectl get --raw='/readyz' 2>&1 | Out-Null
        $alive = ($LASTEXITCODE -eq 0)
        $ErrorActionPreference = $prev
        if (-not $alive) {
            Write-Host 'Existing cluster API unhealthy - recreating.' -ForegroundColor Yellow
            $recreate = $true
        }
    }

    if ($has -and $recreate) {
        Write-Host 'Deleting old cluster...'
        $ErrorActionPreference = 'Continue'
        & $kindCmd delete cluster --name $ClusterName 2>&1 | Out-Null
        $ErrorActionPreference = $prev
        $has = $false
    }

    if (-not $has) {
        $cfg = @"
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
  - role: control-plane
"@
        $cfgPath = Join-Path $env:TEMP 'fintech-kind-trading-local.yaml'
        Set-Content -Path $cfgPath -Value $cfg -Encoding ASCII
        & $kindCmd create cluster --name $ClusterName --config $cfgPath
        if ($LASTEXITCODE -ne 0) { throw 'kind create failed' }
        Export-And-FixKube $kindCmd
    } else {
        Write-Host 'Cluster exists - reuse'
        Export-And-FixKube $kindCmd
    }

    kubectl config use-context $Context | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "kubectl use-context $Context failed" }

    $deadline = (Get-Date).AddSeconds(120)
    while ((Get-Date) -lt $deadline) {
        $ErrorActionPreference = 'Continue'
        kubectl get --raw='/readyz' 2>&1 | Out-Null
        $ok = ($LASTEXITCODE -eq 0)
        $ErrorActionPreference = $prev
        if ($ok) {
            Write-Host "Kubernetes API ready (context=$Context)" -ForegroundColor Green
            return
        }
        Start-Sleep -Seconds 3
    }
    throw 'Kubernetes API not ready within 120s'
}

function Build-Images {
    if ($SkipBuild) {
        Write-Host 'SKIP build (-SkipBuild)' -ForegroundColor Yellow
        return
    }
    Write-Step 'gradlew bootJar (local JDK 21)'
    .\gradlew.bat :gateway:bootJar :order-service:bootJar :risk-service:bootJar :account-service:bootJar -x test --no-daemon
    if ($LASTEXITCODE -ne 0) { throw 'gradlew bootJar failed' }
    Write-Step 'docker build runtime images (Dockerfile.k8s-local)'
    $platform = Get-PlatformDockerBuildPlatform
    Write-Host "  platform=$platform (must match kind node)" -ForegroundColor DarkGray
    $env:DOCKER_DEFAULT_PLATFORM = $platform
    $modules = @(
        @{ Module = 'gateway'; Tag = 'fintech-demo/gateway:local' },
        @{ Module = 'order-service'; Tag = 'fintech-demo/order-service:local' },
        @{ Module = 'risk-service'; Tag = 'fintech-demo/risk-service:local' },
        @{ Module = 'account-service'; Tag = 'fintech-demo/account-service:local' }
    )
    foreach ($m in $modules) {
        $jar = Join-Path $Root "$($m.Module)\build\libs\app.jar"
        if (-not (Test-Path $jar)) {
            throw "bootJar missing: $jar - check .dockerignore and gradlew :$($m.Module):bootJar"
        }
        Write-Host "  build $($m.Tag)"
        docker build --platform $platform -f Dockerfile.k8s-local --build-arg MODULE=$($m.Module) -t $($m.Tag) .
        if ($LASTEXITCODE -ne 0) { throw "docker build failed: $($m.Tag)" }
    }
}

function Assert-ImageInKind([string]$imageRef) {
    Assert-PlatformImageInKind $imageRef
}

function Load-Images([string]$kindCmd) {
    Write-Step 'kind load docker-image'
    foreach ($img in $Images) {
        Write-Host "  load $img"
        & $kindCmd load docker-image $img --name $ClusterName
        if ($LASTEXITCODE -ne 0) { throw "kind load failed: $img" }
        Assert-ImageInKind $img
        Write-Host "  verified in kind node" -ForegroundColor DarkGray
    }
}

function Apply-And-Wait {
    Write-Step 'kubectl apply -k overlays/dev'
    kubectl apply -k $Overlay
    if ($LASTEXITCODE -ne 0) { throw 'kubectl apply failed' }

    Write-Step 'rollout status'
    $deploys = @('risk-service', 'account-service', 'order-service', 'gateway')
    foreach ($d in $deploys) {
        Write-Host "  waiting $d ..."
        kubectl -n $Namespace rollout status "deploy/$d" --timeout=300s
        if ($LASTEXITCODE -ne 0) {
            Write-Host "  --- kubectl describe deploy/$d (last events) ---" -ForegroundColor Yellow
            kubectl -n $Namespace describe "deploy/$d" | Select-Object -Last 25
            throw "rollout failed: $d"
        }
    }
}

function Show-Summary {
    Write-Host ""
    Write-Host '======== FinTechDemo K8s DEMO OK ========' -ForegroundColor Green
    Write-Host "context : $Context"
    Write-Host "namespace: $Namespace"
    kubectl -n $Namespace get pods,svc
    Write-Host ""
    Write-Host ''
    $pf = $PlatformK8sGatewayPfLocal
    $gw = $PlatformGatewayPort
    Write-Host "Browser (after port-forward): http://127.0.0.1:${pf}/actuator/health" -ForegroundColor Cyan
    Write-Host "Frontend (default): http://127.0.0.1:$PlatformVitePort/login  (trader1 / password)"
    Write-Host "Blueprint: http://127.0.0.1:$PlatformVitePort/blueprint#k8s-intellij"
    Write-Host '=========================================' -ForegroundColor Green
}

function Start-GatewayPortForwardWindow {
    if ($SkipPortForward) {
        Write-Host 'SKIP port-forward (-SkipPortForward). Manual:' -ForegroundColor Yellow
        Write-Host "  `$env:KUBECONFIG='$PlatformKubeConfig'"
        Write-Host "  kubectl -n $Namespace port-forward --address 127.0.0.1 svc/gateway ${PlatformK8sGatewayPfLocal}:$PlatformGatewayPort"
        return
    }
    $pf = $PlatformK8sGatewayPfLocal
    $gw = $PlatformGatewayPort
    $kube = $PlatformKubeConfig
    $listening = Get-NetTCPConnection -LocalPort $pf -State Listen -ErrorAction SilentlyContinue
    if ($listening) {
        Write-Host "Port $pf in use - restart port-forward." -ForegroundColor Yellow
        Stop-ListeningPort $pf
        Start-Sleep -Seconds 1
    }
    Write-Step "Gateway port-forward :$pf (new window)"
    # New window stays open; set KUBECONFIG so kubectl does not use docker-desktop.
    $inner = @"
`$ErrorActionPreference = 'Continue'
`$env:KUBECONFIG = '$kube'
kubectl config use-context $Context | Out-Null
Write-Host ''
Write-Host '======== Gateway port-forward ========' -ForegroundColor Green
Write-Host "  kubectl -n $Namespace port-forward --address 127.0.0.1 svc/gateway ${pf}:${gw}"
Write-Host "  Browser: http://127.0.0.1:${pf}/actuator/health"
Write-Host '  E0825 wsarecv in this window is usually harmless (client closed).' -ForegroundColor DarkGray
Write-Host '  Keep this window open. Ctrl+C to stop.' -ForegroundColor Yellow
Write-Host '======================================' -ForegroundColor Green
Write-Host ''
kubectl -n $Namespace port-forward --address 127.0.0.1 svc/gateway ${pf}:${gw}
Write-Host ''
Write-Host 'port-forward ended.' -ForegroundColor Yellow
pause
"@
    Start-Process -FilePath 'powershell.exe' -ArgumentList @(
        '-NoProfile', '-ExecutionPolicy', 'Bypass', '-NoExit', '-Command', $inner
    ) | Out-Null
    Write-Host "Opened new PowerShell for :$pf -> svc/gateway:$gw" -ForegroundColor Green
    Write-Host "  http://127.0.0.1:${pf}/actuator/health"
}

try {
    Use-PlatformKube
    if (-not $SkipStopLocal) {
        Stop-LocalDemoServices
    } else {
        Write-Host 'SKIP stop local (-SkipStopLocal)' -ForegroundColor Yellow
    }
    Wait-Docker
    $kind = Resolve-Kind
    Ensure-KindCluster $kind
    Build-Images
    Load-Images $kind
    Apply-And-Wait
    Show-Summary
    Start-GatewayPortForwardWindow
    if (-not $SkipFrontend) {
        Start-Sleep -Seconds 3
        Start-K8sFrontend | Out-Null
    } else {
        Write-Host 'SKIP frontend (-SkipFrontend)' -ForegroundColor Yellow
    }
    exit 0
} catch {
    Write-Host ""
    Write-Host "K8S_DEMO_FAIL: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host 'Hint: context kind-trading-local (not docker-desktop). See docs/guides/k8s-complete-guide.html' -ForegroundColor Yellow
    exit 1
}

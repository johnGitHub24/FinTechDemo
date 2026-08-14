<#
.SYNOPSIS
  FinTechDemo 完整 K8s Demo：kind 叢集 → build 映像 → load → apply → 等 Pod Ready。

.DESCRIPTION
  對齊 deploy/k8s/overlays/dev（gateway + order + risk + account）。
  日常 bootRun／Compose 與本腳本擇一；RAM 不足時先停 kind 或舊 bootRun。

.PARAMETER RecreateCluster
  刪除並重建 kind trading-local（API 掛掉或 kubeconfig 漂移時）

.PARAMETER SkipBuild
  跳過 docker compose build（映像已存在時）

.EXAMPLE
  .\demo\start-k8s-demo.ps1
  .\demo\start-k8s-demo.ps1 -RecreateCluster
#>
param(
    [switch]$RecreateCluster,
    [switch]$SkipBuild
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
            Write-Host 'Existing cluster API unhealthy — recreating.' -ForegroundColor Yellow
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
        Write-Host 'Cluster exists — reuse'
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
            throw "bootJar missing: $jar — check .dockerignore and gradlew :$($m.Module):bootJar"
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
    Write-Host 'Port-forward (new PowerShell window):' -ForegroundColor Cyan
    $pf = $PlatformK8sGatewayPfLocal
    $gw = $PlatformGatewayPort
    Write-Host "  kubectl -n $Namespace port-forward svc/gateway ${pf}:${gw}"
    Write-Host "  Browser: http://localhost:${pf}/actuator/health"
    Write-Host ''
    Write-Host "Frontend: Vite :$PlatformVitePort (npm run dev). Gateway via $pf."
    Write-Host "Blueprint: http://localhost:$PlatformVitePort/blueprint#k8s-intellij"
    Write-Host '=========================================' -ForegroundColor Green
}

try {
    Wait-Docker
    $kind = Resolve-Kind
    Ensure-KindCluster $kind
    Build-Images
    Load-Images $kind
    Apply-And-Wait
    Show-Summary
    exit 0
} catch {
    Write-Host ""
    Write-Host "K8S_DEMO_FAIL: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host 'Hint: use context kind-trading-local not docker-desktop. See docs/deploy/k8s-tips.html' -ForegroundColor Yellow
    exit 1
}

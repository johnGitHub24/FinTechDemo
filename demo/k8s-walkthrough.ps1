#Requires -Version 5.1
<#
.SYNOPSIS
  K8s walkthrough: Docker Desktop images vs kind vs Pods.
.EXAMPLE
  .\demo\k8s-walkthrough.ps1
  .\demo\k8s-walkthrough.ps1 -Fix
#>
param([switch]$Fix)

$ErrorActionPreference = 'Continue'
$Root = Split-Path $PSScriptRoot -Parent
Set-Location $Root
. (Join-Path $PSScriptRoot 'platform-env.ps1') -ProjectRoot $Root
Use-PlatformKube

$node = Get-PlatformKindNodeName
$platform = Get-PlatformDockerBuildPlatform

Write-Host ''
Write-Host '======== FinTechDemo K8s walkthrough ========' -ForegroundColor Cyan
Write-Host ''
Write-Host 'Layer 1: Docker Desktop Images (4x fintech-demo/*:local on host)' -ForegroundColor Yellow
Write-Host 'Layer 2: kind load -> trading-local-control-plane (1 container in Desktop)' -ForegroundColor Yellow
Write-Host 'Layer 3: kubectl apply -> Pods + Services in namespace fintech-demo' -ForegroundColor Yellow
Write-Host ''
Write-Host "DOCKER_BUILD_PLATFORM=$($PlatformRun['DOCKER_BUILD_PLATFORM']) -> $platform"
Write-Host "K8S_CONTEXT=$PlatformK8sContext"
Write-Host "K8S_NAMESPACE=$PlatformK8sNamespace"
Write-Host "KUBECONFIG=$PlatformKubeConfig"
Write-Host ''

Write-Host '--- Step 1: images on Docker Desktop host ---' -ForegroundColor Cyan
foreach ($img in $PlatformK8sImages) {
    $id = docker images -q $img 2>$null
    if ($id) {
        $arch = docker inspect $img --format '{{.Os}}/{{.Architecture}}' 2>$null
        Write-Host "  OK   $img ($arch)" -ForegroundColor Green
    } else {
        Write-Host "  MISS $img -> run .\demo\start-k8s-demo.ps1" -ForegroundColor Red
    }
}

Write-Host ''
Write-Host '--- Step 2: same images INSIDE kind node (K8s uses these) ---' -ForegroundColor Cyan
$inKind = docker exec $node crictl images 2>&1 | Out-String
foreach ($img in $PlatformK8sImages) {
    $repo = ($img -split ':')[0]
    if ($inKind -match [regex]::Escape($repo)) {
        Write-Host "  OK   $img (inside kind)" -ForegroundColor Green
    } else {
        Write-Host "  MISS $img (kind load failed or wrong CPU arch; need $platform)" -ForegroundColor Red
    }
}

Write-Host ''
Write-Host '--- Step 3: kubectl get svc / pods ---' -ForegroundColor Cyan
kubectl -n $PlatformK8sNamespace get svc 2>$null
Write-Host ''
kubectl -n $PlatformK8sNamespace get pods 2>$null

Write-Host ''
Write-Host 'Status hints:' -ForegroundColor Yellow
Write-Host '  ImagePullBackOff = image not in kind (often amd64 image on arm64 kind)'
Write-Host '  Running 1/1      = OK; use port-forward next'
Write-Host ''

if ($Fix) {
    Write-Host 'Running start-k8s-demo.ps1 ...' -ForegroundColor Magenta
    & (Join-Path $PSScriptRoot 'start-k8s-demo.ps1')
    exit $LASTEXITCODE
}

Write-Host 'Next steps:' -ForegroundColor Green
Write-Host '  Fix all:  .\demo\k8s-walkthrough.ps1 -Fix'
Write-Host '  Or:       .\demo\start-k8s-demo.ps1'
Write-Host ''
Write-Host 'When pods are Running (new window):'
Write-Host "  `$env:KUBECONFIG='$PlatformKubeConfig'"
$pf = "$($PlatformK8sGatewayPfLocal):$PlatformGatewayPort"
Write-Host "  kubectl -n $PlatformK8sNamespace port-forward svc/gateway $pf"
Write-Host "  http://localhost:$($PlatformK8sGatewayPfLocal)/actuator/health"
Write-Host "  frontend: cd frontend; npm run dev -> http://localhost:$PlatformVitePort"
Write-Host '============================================' -ForegroundColor Cyan

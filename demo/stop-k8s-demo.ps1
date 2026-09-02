#Requires -Version 5.1
<#
.SYNOPSIS
  Stop FinTechDemo K8s Demo (port-forward, Vite, optional kind cluster).

.DESCRIPTION
  Pair with: 開啟K8sDemo.cmd / start-k8s-demo.ps1

.PARAMETER DeleteCluster
  kind delete cluster (frees RAM; next K8s demo will rebuild).

.PARAMETER SkipStopLocal
  Only stop :18080 port-forward and skip :5173 Vite.

.EXAMPLE
  .\demo\stop-k8s-demo.ps1
  .\關閉K8sDemo.cmd
  .\demo\stop-k8s-demo.ps1 -DeleteCluster
#>
param(
    [switch]$DeleteCluster,
    [switch]$SkipStopLocal
)

$ErrorActionPreference = 'Continue'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$Root = Split-Path $PSScriptRoot -Parent
Set-Location $Root
. (Join-Path $PSScriptRoot 'platform-env.ps1') -ProjectRoot $Root

Write-Host '== STOP K8s FinTechDemo ==' -ForegroundColor Cyan
Write-Host '  Pair: 開啟K8sDemo.cmd -> start-k8s-demo' -ForegroundColor DarkCyan
Write-Host ''

Write-Host '--- Gateway port-forward ---' -ForegroundColor Yellow
Stop-PlatformListeningPort -Port $PlatformK8sGatewayPfLocal

if (-not $SkipStopLocal) {
    Write-Host '--- Vite (K8s frontend) ---' -ForegroundColor Yellow
    Stop-PlatformListeningPort -Port $PlatformVitePort
}

if ($DeleteCluster) {
    Write-Host '--- kind delete cluster ---' -ForegroundColor Yellow
    if (Get-Command kind -ErrorAction SilentlyContinue) {
        Write-Host "  kind delete cluster --name $PlatformK8sCluster" -ForegroundColor DarkGray
        kind delete cluster --name $PlatformK8sCluster 2>$null | Out-Null
    } else {
        $kindExe = Join-Path $PlatformToolsDir 'kind.exe'
        if (Test-Path -LiteralPath $kindExe) {
            & $kindExe delete cluster --name $PlatformK8sCluster 2>$null | Out-Null
        } else {
            Write-Host '  SKIP kind not found' -ForegroundColor DarkYellow
        }
    }
} else {
    Write-Host '  kind cluster kept (use -DeleteCluster to remove)' -ForegroundColor DarkGray
}

Start-Sleep -Seconds 1
Write-Host ''
Write-Host 'STOP_K8S_DEMO_OK' -ForegroundColor Green
Write-Host '  Local bootRun still up? Use 關閉Demo.cmd' -ForegroundColor DarkGray
Write-Host '  Re-start K8s: .\開啟K8sDemo.cmd' -ForegroundColor DarkGray
exit 0

#Requires -Version 5.1
<#
.SYNOPSIS
  Stop local FinTechDemo services started by 開啟Demo.cmd / ensure-demo-links.ps1.

.DESCRIPTION
  Frees ports: Gateway/Order/Risk/Job/Account, Vite, docs :5500, monitoring, Locust.
  Pair with: 關閉Demo.cmd

.PARAMETER StopDocker
  Also stop docker compose redis + monitoring (prometheus/grafana).

.PARAMETER StopDockerDown
  docker compose down (more aggressive; implies -StopDocker).

.EXAMPLE
  .\demo\stop-demo-services.ps1
  .\關閉Demo.cmd
  .\demo\stop-demo-services.ps1 -StopDocker
#>
param(
    [switch]$StopDocker,
    [switch]$StopDockerDown
)

$ErrorActionPreference = 'Continue'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$Root = Split-Path $PSScriptRoot -Parent
Set-Location $Root
. (Join-Path $PSScriptRoot 'platform-env.ps1') -ProjectRoot $Root

if ($StopDockerDown) { $StopDocker = $true }

Write-Host '== STOP local FinTechDemo ==' -ForegroundColor Cyan
Write-Host '  Pair: 開啟Demo.cmd -> ensure-demo-links' -ForegroundColor DarkCyan
Write-Host ''

Write-Host '--- listening ports ---' -ForegroundColor Yellow
foreach ($p in (Get-PlatformLocalDemoPorts)) {
    Stop-PlatformListeningPort -Port $p
}

Write-Host '--- Gradle daemons (not bootRun wrapper) ---' -ForegroundColor Yellow
Stop-PlatformGradleDaemons

if ($StopDocker -or $StopDockerDown) {
    Write-Host '--- docker compose ---' -ForegroundColor Yellow
    if ($StopDockerDown) {
        Write-Host '  docker compose down' -ForegroundColor DarkGray
        $null = Invoke-PlatformDockerComposeQuiet -ComposeArgs @('down')
    } else {
        Write-Host '  docker compose stop redis' -ForegroundColor DarkGray
        $null = Invoke-PlatformDockerComposeQuiet -ComposeArgs @('stop', 'redis')
        Write-Host '  docker compose --profile monitoring stop prometheus grafana' -ForegroundColor DarkGray
        $null = Invoke-PlatformDockerComposeQuiet -ComposeArgs @('--profile', 'monitoring', 'stop', 'prometheus', 'grafana')
    }
}

Start-Sleep -Seconds 1
Write-Host ''
Write-Host 'STOP_LOCAL_DEMO_OK' -ForegroundColor Green
Write-Host '  K8s still running? Use 關閉K8sDemo.cmd' -ForegroundColor DarkGray
Write-Host '  Re-start: .\開啟Demo.cmd' -ForegroundColor DarkGray
exit 0

#Requires -Version 5.1
# Compat shim -> ensure-demo-links.ps1 (see demo/README.md)
param(
    [switch] $Full,
    [switch] $ServeDocs,
    [switch] $FreeKind,
    [switch] $OpenBrowser,
    [switch] $SkipClean,
    [switch] $ForceClean,
    [int] $MinFreeMb = 600
)
$ErrorActionPreference = "Continue"
Write-Host "NOTE: start-demo-ready -> ensure-demo-links (demo/README.md)" -ForegroundColor DarkYellow
$ensure = Join-Path $PSScriptRoot "ensure-demo-links.ps1"
$argList = @()
if ($ForceClean) { $argList += "-ForceRestart" }
& $ensure @argList
$code = $LASTEXITCODE
if ($OpenBrowser -and $code -eq 0) { Start-Process "http://localhost:5173/login" }
exit $code
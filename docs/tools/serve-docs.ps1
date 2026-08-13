<#
.SYNOPSIS
  本機靜態文件伺服（專案根；無 .html 副檔名會回退到 .html/.md）。

.EXAMPLE
  .\docs\tools\serve-docs.ps1
  # http://127.0.0.1:5500/docs/index.html
#>
param(
    [int]$Port = 5500
)

$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
Set-Location $Root

$conns = @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
foreach ($c in $conns) {
    $ownerPid = [int]$c.OwningProcess
    if ($ownerPid -gt 0) {
        Write-Host "Stopping old listener pid=$ownerPid on :$Port" -ForegroundColor DarkGray
        Stop-Process -Id $ownerPid -Force -ErrorAction SilentlyContinue
        Start-Sleep -Milliseconds 500
    }
}

Write-Host "Serving $Root on http://127.0.0.1:$Port/" -ForegroundColor Cyan
Write-Host "MUST open: http://127.0.0.1:$Port/docs/index.html" -ForegroundColor Green
Write-Host "Ctrl+C to stop." -ForegroundColor Yellow

$py = Get-Command python -ErrorAction SilentlyContinue
if ($py) {
    $server = Join-Path $PSScriptRoot "serve_docs_http.py"
    & python $server --port $Port --bind 127.0.0.1 --root $Root
    exit $LASTEXITCODE
}

Write-Host "python not found - using npx serve (no extensionless fallback)" -ForegroundColor Yellow
npx --yes serve -l $Port .

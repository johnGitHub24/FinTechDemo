<#
.SYNOPSIS
  依 docs/catalog.yaml 產生主題目錄靜態 HTML。
.EXAMPLE
  .\docs\tools\generate-docs-html.ps1
#>
$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
Set-Location $Root
$env:npm_config_cache = Join-Path $Root ".npm-cache"
Write-Host "Generating docs HTML from catalog.yaml..." -ForegroundColor Cyan
Push-Location $PSScriptRoot
try {
  if (-not (Test-Path ".\node_modules\marked")) {
    Write-Host "npm install (marked, js-yaml) in docs/tools/ ..." -ForegroundColor DarkCyan
    npm install --no-fund --no-audit --no-progress
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
  }
  node .\generate-docs-html.mjs
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally {
  Pop-Location
}
Write-Host "Done. Open e.g. http://127.0.0.1:5500/docs/guides/why.html" -ForegroundColor Green

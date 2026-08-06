<#
.SYNOPSIS
  依 docs/catalog.yaml 產生主題目錄靜態 HTML，並寫入 legacy stub。
.EXAMPLE
  .\scripts\generate-docs-html.ps1
#>
$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
Set-Location $Root
$env:npm_config_cache = Join-Path $Root ".npm-cache"
Write-Host "Generating docs HTML from catalog.yaml..." -ForegroundColor Cyan
Push-Location (Join-Path $Root "scripts")
try {
  if (-not (Test-Path ".\node_modules\marked")) {
    Write-Host "npm install (marked, js-yaml) in scripts/ ..." -ForegroundColor DarkCyan
    npm install --no-fund --no-audit --no-progress
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
  }
  node .\generate-docs-html.mjs
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally {
  Pop-Location
}
Write-Host "Done. Open e.g. http://127.0.0.1:5500/docs/guides/why.html" -ForegroundColor Green

<#
.SYNOPSIS
  本機靜態文件伺服（避開 npx serve 對中文檔名 404；亦提供 ASCII 入口）。

.EXAMPLE
  .\scripts\serve-docs.ps1
  # 瀏覽器開 http://127.0.0.1:5500/docs/index.html
  # 或 http://127.0.0.1:5500/docs/learning-map.html
#>
param(
    [int]$Port = 5500
)

$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
Set-Location $Root

Write-Host "Serving $Root on http://127.0.0.1:$Port/" -ForegroundColor Cyan
Write-Host "MUST open: http://127.0.0.1:$Port/docs/index.html" -ForegroundColor Green
Write-Host "  (do NOT use http://127.0.0.1:$Port/docs  without index.html — relative links 404)" -ForegroundColor Yellow
Write-Host "Ctrl+C to stop." -ForegroundColor Yellow

# Prefer Python (UTF-8 path friendly)
$py = Get-Command python -ErrorAction SilentlyContinue
if ($py) {
    python -m http.server $Port --bind 127.0.0.1
    exit $LASTEXITCODE
}

# Fallback: npx serve (ASCII paths only)
Write-Host "python not found — using npx serve (use ASCII URLs only)" -ForegroundColor Yellow
npx --yes serve -l $Port .

# Start local Prometheus (:9090) + Grafana (:3000) without Docker.
# Encoding: UTF-8 BOM
$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
$tools = Join-Path $Root "tools"
$promHome = Join-Path $tools "prometheus-2.48.0.windows-amd64"
$grafExe = Join-Path $tools "grafana-10.2.0\bin\grafana-server.exe"
$grafHome = Join-Path $tools "grafana-10.2.0"

function Test-Up([string]$Url) {
  try { $r = Invoke-WebRequest $Url -UseBasicParsing -TimeoutSec 2; return $true } catch { return $false }
}

if (-not (Test-Path "$promHome\prometheus.exe")) {
  Write-Host "ERROR: missing $promHome\prometheus.exe (run download once)" -ForegroundColor Red
  exit 1
}
if (-not (Test-Path $grafExe)) {
  Write-Host "ERROR: missing $grafExe" -ForegroundColor Red
  exit 1
}

Copy-Item (Join-Path $tools "prometheus-local.yml") (Join-Path $promHome "prometheus.yml") -Force

if (-not (Test-Up "http://127.0.0.1:9090/-/healthy")) {
  Write-Host "START prometheus :9090" -ForegroundColor Cyan
  Start-Process -FilePath "$promHome\prometheus.exe" -WorkingDirectory $promHome `
    -ArgumentList "--config.file=prometheus.yml","--web.listen-address=0.0.0.0:9090","--storage.tsdb.path=data" `
    -WindowStyle Minimized
}

if (-not (Test-Up "http://127.0.0.1:3000/login")) {
  Write-Host "START grafana :3000" -ForegroundColor Cyan
  $env:GF_PATHS_PROVISIONING = Join-Path $Root "monitoring\grafana\provisioning"
  Start-Process -FilePath $grafExe -WorkingDirectory (Split-Path $grafExe) `
    -ArgumentList "--homepath=$grafHome" -WindowStyle Minimized
}

$ok = $true
for ($i=0; $i -lt 30; $i++) {
  $p = Test-Up "http://127.0.0.1:9090/-/healthy"
  $g = Test-Up "http://127.0.0.1:3000/login"
  if ($p -and $g) { break }
  Start-Sleep 2
}
if (Test-Up "http://127.0.0.1:9090/-/healthy") { Write-Host "OK prometheus http://localhost:9090" -ForegroundColor Green } else { Write-Host "FAIL prometheus"; $ok=$false }
if (Test-Up "http://127.0.0.1:3000/login") { Write-Host "OK grafana http://localhost:3000 (admin/admin)" -ForegroundColor Green } else { Write-Host "FAIL grafana"; $ok=$false }
if (-not $ok) { exit 1 }
exit 0
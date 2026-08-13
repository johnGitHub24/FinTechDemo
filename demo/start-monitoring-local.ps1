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
  # Docker 用的 provisioning 指向容器路徑；本機改寫一份到 tools/grafana-provisioning-local
  $provLocal = Join-Path $tools "grafana-provisioning-local"
  $dashDir = (Join-Path $Root "monitoring\grafana\dashboards") -replace '\\', '/'
  New-Item -ItemType Directory -Force -Path (Join-Path $provLocal "datasources") | Out-Null
  New-Item -ItemType Directory -Force -Path (Join-Path $provLocal "dashboards") | Out-Null
  @"
apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    uid: prometheus
    access: proxy
    url: http://127.0.0.1:9090
    isDefault: true
    editable: false
"@ | Set-Content -Encoding utf8 (Join-Path $provLocal "datasources\datasource.yml")
  @"
apiVersion: 1
providers:
  - name: FinTechDemo
    orgId: 1
    folder: ''
    type: file
    disableDeletion: false
    editable: true
    options:
      path: $dashDir
"@ | Set-Content -Encoding utf8 (Join-Path $provLocal "dashboards\dashboards.yml")
  $env:GF_PATHS_PROVISIONING = $provLocal
  $env:GF_SECURITY_ADMIN_USER = "admin"
  $env:GF_SECURITY_ADMIN_PASSWORD = "admin"
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
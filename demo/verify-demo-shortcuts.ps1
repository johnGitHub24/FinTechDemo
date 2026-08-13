# FinTechDemo verify-demo-shortcuts.ps1
# Probe only (no start). On FAIL run: .\demo\ensure-demo-links.ps1
param()
$ErrorActionPreference = "Continue"
$Root = Split-Path $PSScriptRoot -Parent
Set-Location $Root

function Test-HttpOk([string]$Url) {
    try {
        $r = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 4
        return ($r.StatusCode -ge 200 -and $r.StatusCode -lt 500)
    } catch { return $false }
}

$probe = @(
    @{ Label = "Vite login"; Url = "http://localhost:5173/login" },
    @{ Label = "Risk Check"; Url = "http://localhost:5173/demo/risk-check.html" },
    @{ Label = "Account Me"; Url = "http://localhost:5173/demo/account-me.html" },
    @{ Label = "Order Health"; Url = "http://localhost:8081/actuator/health" },
    @{ Label = "Order Swagger"; Url = "http://localhost:8081/swagger-ui/index.html" },
    @{ Label = "Order OpenAPI"; Url = "http://localhost:8081/v3/api-docs" },
    @{ Label = "Order H2"; Url = "http://localhost:8081/h2-console/" },
    @{ Label = "Order Prometheus"; Url = "http://localhost:8081/actuator/prometheus" },
    @{ Label = "Risk Health"; Url = "http://localhost:8082/actuator/health" },
    @{ Label = "Gateway"; Url = "http://localhost:8080/actuator/health" },
    @{ Label = "Job"; Url = "http://localhost:8083/actuator/health" },
    @{ Label = "Account"; Url = "http://localhost:8084/actuator/health" },
    @{ Label = "Grafana"; Url = "http://localhost:3000/login" },
    @{ Label = "Prometheus UI"; Url = "http://localhost:9090/-/healthy" },
    @{ Label = "Locust"; Url = "http://localhost:8089/" },
    @{ Label = "Docs"; Url = "http://127.0.0.1:5500/docs/index.html" },
    @{ Label = "Demo flow"; Url = "http://127.0.0.1:5500/docs/portals/demo-flow.html" },
    @{ Label = "Handbook"; Url = "http://127.0.0.1:5500/docs/portals/handbook.html" },
    @{ Label = "Swagger static"; Url = "http://127.0.0.1:5500/docs/portals/swagger.html" },
    @{ Label = "codeGraphic"; Url = "http://127.0.0.1:5500/docs/portals/codeGraphic.html" },
    @{ Label = "Javadoc"; Url = "http://127.0.0.1:5500/docs/javadoc/index.html" },
    @{ Label = "Test reports hub"; Url = "http://127.0.0.1:5500/docs/portals/test-reports.html" },
    @{ Label = "Report order"; Url = "http://127.0.0.1:5500/order-service/build/reports/tests/test/index.html" },
    @{ Label = "Report risk"; Url = "http://127.0.0.1:5500/risk-service/build/reports/tests/test/index.html" }
)

Write-Host "== verify-demo-shortcuts ==" -ForegroundColor Cyan
$fail = 0
foreach ($item in $probe) {
    if (Test-HttpOk $item.Url) {
        Write-Host ("  OK  {0,-20} {1}" -f $item.Label, $item.Url) -ForegroundColor Green
    } else {
        $fail++
        Write-Host ("  FAIL {0,-20} {1}" -f $item.Label, $item.Url) -ForegroundColor Red
    }
}
if ($fail -gt 0) {
    Write-Host "DOWN: $fail — run .\demo\ensure-demo-links.ps1" -ForegroundColor Yellow
    exit 1
}
Write-Host "ALL OK: Demo shortcut targets reachable" -ForegroundColor Green
exit 0
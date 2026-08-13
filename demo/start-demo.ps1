# FinTechDemo start-demo
# -Minimal          印出最短可成交指令（Risk + Order + frontend）
# -UpInfra          docker compose 起 Kafka/Redis（與核心服務，視 compose 而定）
# -StartMinimal     本腳本背景啟動 risk(:8082) + order(:8081)，並印出 frontend 指令
# -OpenDocs         啟動後嘗試開啟 JavaDoc / 啟動流程 HTML
param(
    [switch]$Minimal,
    [switch]$UpInfra,
    [switch]$StartMinimal,
    [switch]$OpenDocs
)

$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
Set-Location $Root

function Test-Cmd([string]$Name) {
    return [bool](Get-Command $Name -ErrorAction SilentlyContinue)
}

function Wait-HttpOk([string]$Url, [int]$Seconds = 90) {
    $deadline = (Get-Date).AddSeconds($Seconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $r = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
            if ($r.StatusCode -ge 200 -and $r.StatusCode -lt 500) { return $true }
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    return $false
}

Write-Host "== FinTechDemo start-demo ==" -ForegroundColor Cyan

if (-not (Test-Cmd "java")) {
    Write-Host "WARN: java not on PATH (need JDK 21)" -ForegroundColor Yellow
} else {
    Write-Host "java: OK" -ForegroundColor Green
}

$gradlew = Join-Path $Root "gradlew.bat"
if (-not (Test-Path $gradlew)) {
    Write-Host "ERROR: gradlew.bat missing at $Root" -ForegroundColor Red
    exit 1
}
Write-Host "gradlew: OK" -ForegroundColor Green

if ($UpInfra) {
    if (-not (Test-Cmd "docker")) {
        Write-Host "ERROR: -UpInfra requires docker" -ForegroundColor Red
        exit 1
    }
    Write-Host "== docker compose up -d ==" -ForegroundColor Cyan
    docker compose up -d
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

if ($StartMinimal) {
    Write-Host ""
    Write-Host "[StartMinimal] launching Risk(:8082) then Order(:8081) in background..." -ForegroundColor Green
    $logs = Join-Path $Root "logs"
    New-Item -ItemType Directory -Force -Path $logs | Out-Null

    Start-Process -FilePath $gradlew -ArgumentList ":risk-service:bootRun" `
        -WorkingDirectory $Root -WindowStyle Minimized `
        -RedirectStandardOutput (Join-Path $logs "risk-service.out.log") `
        -RedirectStandardError (Join-Path $logs "risk-service.err.log")
    Write-Host "  risk-service starting... logs\risk-service.*.log"

    if (-not (Wait-HttpOk "http://localhost:8082/actuator/health" 120)) {
        Write-Host "WARN: risk-service health not ready within timeout" -ForegroundColor Yellow
    } else {
        Write-Host "  risk-service UP  http://localhost:8082/actuator/health" -ForegroundColor Green
    }

    Start-Process -FilePath $gradlew -ArgumentList ":order-service:bootRun" `
        -WorkingDirectory $Root -WindowStyle Minimized `
        -RedirectStandardOutput (Join-Path $logs "order-service.out.log") `
        -RedirectStandardError (Join-Path $logs "order-service.err.log")
    Write-Host "  order-service starting... logs\order-service.*.log"

    if (-not (Wait-HttpOk "http://localhost:8081/actuator/health" 120)) {
        Write-Host "WARN: order-service health not ready within timeout" -ForegroundColor Yellow
    } else {
        Write-Host "  order-service UP  http://localhost:8081/actuator/health" -ForegroundColor Green
    }

    Write-Host ""
    Write-Host "Next (frontend terminal):" -ForegroundColor Cyan
    Write-Host "  cd frontend; npm run dev"
    Write-Host "  http://localhost:5173   trader1 / password"
    Write-Host ""
    Write-Host "URLs:"
    Write-Host "  Order Swagger  http://localhost:8081/swagger-ui.html"
    Write-Host "  Risk Health    http://localhost:8082/actuator/health"
    Write-Host "  JavaDoc HTML   docs\javadoc\index.html   (.\gradlew.bat aggregateJavadoc)"
    Write-Host "  Demo flow      docs\啟動與Demo運作流程.html"

    if ($OpenDocs) {
        $flow = Join-Path $Root "docs\啟動與Demo運作流程.html"
        $jd = Join-Path $Root "docs\javadoc\index.html"
        if (Test-Path $flow) { Start-Process $flow }
        if (Test-Path $jd) { Start-Process $jd }
    }
    exit 0
}

if ($Minimal) {
    Write-Host ""
    Write-Host "[Minimal] Open three terminals (成交需要 :8082 risk):" -ForegroundColor Green
    Write-Host "1. .\gradlew.bat :risk-service:bootRun"
    Write-Host "2. .\gradlew.bat :order-service:bootRun"
    Write-Host "3. cd frontend; npm run dev"
    Write-Host ""
    Write-Host "Or one-shot: .\demo\start-demo.ps1 -StartMinimal"
    Write-Host "Account: trader1 / password"
    Write-Host "Health: http://localhost:8082/actuator/health  +  :8081"
    Write-Host "JavaDoc: .\gradlew.bat aggregateJavadoc → docs\javadoc\index.html"
    exit 0
}

Write-Host ""
Write-Host "[Distributed Demo] multi-terminal (script only prints):" -ForegroundColor Green
Write-Host "0. .\demo\start-demo.ps1 -UpInfra   # Kafka+Redis"
Write-Host "1. .\gradlew.bat :risk-service:bootRun"
Write-Host "2. .\gradlew.bat :account-service:bootRun --args='--spring.profiles.active=demo'"
Write-Host "3. .\gradlew.bat :order-service:bootRun --args='--spring.profiles.active=demo'"
Write-Host "4. .\gradlew.bat :gateway:bootRun"
Write-Host "5. .\gradlew.bat :job-service:bootRun   # optional"
Write-Host "6. cd frontend; `$env:VITE_API_TARGET='http://localhost:8080'; npm run dev"
Write-Host ""
Write-Host "Shortcuts:"
Write-Host "  .\demo\start-demo.ps1 -StartMinimal [-OpenDocs]"
Write-Host "  .\gradlew.bat aggregateJavadoc"
Write-Host ""
Write-Host "Docs: docs/分散式系統落地.md"
Write-Host "Verify: .\scripts\check.ps1"
Write-Host "Accounts: trader1 / admin · password"
exit 0

# FinTechDemo — 載入 JDK／環境（對齊各 Trading* scripts/env.ps1 慣例）
# 若本機已有 JAVA_HOME 可略過；否則依團隊慣例設定 JDK 21。

$ErrorActionPreference = "Stop"
if (-not $env:JAVA_HOME) {
    Write-Host "JAVA_HOME not set — relying on PATH java (need JDK 21)" -ForegroundColor Yellow
}

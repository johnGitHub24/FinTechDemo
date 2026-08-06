. "$PSScriptRoot\env.ps1"
Set-Location (Split-Path $PSScriptRoot -Parent)
.\gradlew.bat check
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host "FinTechDemo check OK" -ForegroundColor Green

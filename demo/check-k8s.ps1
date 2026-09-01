<#
.SYNOPSIS
  驗證 FinTechDemo K8s overlay 可 kustomize build。

.DESCRIPTION
  若系統無 kubectl，印提示並 exit 0（示意產物不強制本機裝 kubectl）。
#>
param()

$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
Set-Location $Root

$overlay = "deploy/k8s/overlays/dev"

if (-not (Get-Command kubectl -ErrorAction SilentlyContinue)) {
    Write-Host "kubectl not found — skip kustomize build. Install kubectl to validate: kubectl kustomize $overlay" -ForegroundColor Yellow
    exit 0
}

Write-Host "== kubectl kustomize $overlay ==" -ForegroundColor Cyan
kubectl kustomize $overlay
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host "FinTechDemo k8s overlay OK" -ForegroundColor Green
exit 0

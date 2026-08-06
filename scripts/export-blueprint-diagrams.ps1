# Export FinTechDemo blueprint Mermaid diagrams to PNG/SVG
# Usage: .\scripts\export-blueprint-diagrams.ps1
$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root
$cfg = 'docs/architecture/mermaid/mmdc-config.json'
$out = 'docs/architecture/images'
New-Item -ItemType Directory -Force -Path $out | Out-Null
foreach ($name in @('01-layers', '02-flow', '03-order-state')) {
  Write-Host "Rendering $name ..."
  npx --yes @mermaid-js/mermaid-cli@11.4.2 -i "docs/architecture/mermaid/$name.mmd" -o "$out/$name.png" -b white -s 2 -c $cfg
  npx --yes @mermaid-js/mermaid-cli@11.4.2 -i "docs/architecture/mermaid/$name.mmd" -o "$out/$name.svg" -b white -c $cfg
}
Write-Host "Done → $out"
Get-ChildItem $out | Format-Table Name, Length

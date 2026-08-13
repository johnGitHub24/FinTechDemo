# FinTechDemo check-docs-links.ps1
# Scan docs HTML (exclude javadoc) for broken local href targets.
param()
$ErrorActionPreference = "Continue"
$Root = Split-Path $PSScriptRoot -Parent
$docs = Join-Path $Root "docs"

function Decode-Href([string]$Href) {
    try {
        return [Uri]::UnescapeDataString($Href)
    } catch {
        return $Href
    }
}

function Resolve-Target([string]$FromFile, [string]$Href) {
    if ([string]::IsNullOrWhiteSpace($Href)) { return $null }
    if ($Href.StartsWith("#")) { return "OK_HASH" }
    if ($Href -match "^(https?:|mailto:|javascript:)" ) { return "OK_EXT" }
    # JS regex / template false positives (e.g. href="([^"]+...) inside <script>)
    if ($Href -match '[\(\)\[\]\\|]' -and $Href -notmatch '^[./#a-zA-Z0-9_%\-]+') { return "OK_SKIP_JS" }

    $clean = (Decode-Href $Href).Split("#")[0].Split("?")[0]
    if ([string]::IsNullOrWhiteSpace($clean)) { return "OK_HASH" }

    if ($clean.StartsWith("/")) {
        $full = Join-Path $Root ($clean.TrimStart("/").Replace("/", [IO.Path]::DirectorySeparatorChar))
    } else {
        $dir = Split-Path $FromFile -Parent
        $full = [IO.Path]::GetFullPath((Join-Path $dir ($clean.Replace("/", [IO.Path]::DirectorySeparatorChar))))
    }
    if (Test-Path -LiteralPath $full) { return "OK" }
    # extensionless fallback (same as serve_docs_http.py)
    if (-not [IO.Path]::HasExtension($full)) {
        foreach ($ext in @(".html", ".md")) {
            if (Test-Path -LiteralPath ($full + $ext)) { return "OK_FALLBACK" }
        }
        if (Test-Path -LiteralPath (Join-Path $full "index.html")) { return "OK_FALLBACK" }
    }
    return $full
}

$htmlFiles = Get-ChildItem -Path $docs -Recurse -Filter *.html -File |
    Where-Object { $_.FullName -notmatch "[\\/]javadoc[\\/]" }

$fail = 0
$checked = 0
foreach ($f in $htmlFiles) {
    $text = Get-Content -LiteralPath $f.FullName -Raw -Encoding UTF8
    # Strip <script>...</script> so JS string literals are not treated as page hrefs
    $scan = [regex]::Replace($text, '(?is)<script\b[^>]*>.*?</script>', ' ')
    $matches = [regex]::Matches($scan, 'href\s*=\s*["'']([^"'']+)["'']')
    foreach ($m in $matches) {
        $href = $m.Groups[1].Value
        $checked++
        $r = Resolve-Target $f.FullName $href
        if ($r -like "OK*") { continue }
        $fail++
        $rel = $f.FullName.Substring($Root.Length + 1)
        Write-Host ("FAIL {0} -> {1}" -f $rel, $href) -ForegroundColor Red
        Write-Host ("     expected: {0}" -f $r) -ForegroundColor DarkGray
    }
}

Write-Host ""
Write-Host ("Checked {0} hrefs in {1} HTML files; broken={2}" -f $checked, $htmlFiles.Count, $fail)
if ($fail -gt 0) { exit 1 }
Write-Host "ALL OK" -ForegroundColor Green
exit 0

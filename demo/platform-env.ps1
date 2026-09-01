# platform-env.ps1 — Load demo/platform-run.properties (Run Anywhere constants)
#
# Usage:
#   . "$PSScriptRoot\platform-env.ps1" -ProjectRoot $Root
#   Use-PlatformKube
#
# 【職責】Demo 層埠號、K8s 叢集名、kubeconfig 路徑的唯一來源；腳本勿再硬編。
# 【技巧】路徑一律相對 ProjectRoot；本機產物在 demo/.tools（git 隱藏檔規則已排除）。

param(
    [string]$ProjectRoot = (Split-Path $PSScriptRoot -Parent)
)

$ErrorActionPreference = 'Stop'

function Read-PlatformRunProperties {
    param([string]$Path)
    $map = @{}
    if (-not (Test-Path $Path)) {
        throw "platform-run.properties not found: $Path"
    }
    Get-Content -LiteralPath $Path -Encoding UTF8 | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith('#')) { return }
        if ($line -match '^([^=]+)=(.*)$') {
            $map[$Matches[1].Trim()] = $Matches[2].Trim()
        }
    }
    return $map
}

$PlatformPropsPath = Join-Path $PSScriptRoot 'platform-run.properties'
$PlatformRun = Read-PlatformRunProperties $PlatformPropsPath

function Get-PlatformInt([string]$Key, [int]$Default = 0) {
    if ($PlatformRun.ContainsKey($Key) -and $PlatformRun[$Key] -match '^\d+$') {
        return [int]$PlatformRun[$Key]
    }
    if ($Default -gt 0) { return $Default }
    throw "platform-run.properties missing or invalid int: $Key"
}

function Get-PlatformBool([string]$Key) {
    if (-not $PlatformRun.ContainsKey($Key)) { return $false }
    return $PlatformRun[$Key] -match '^(?i)(true|1|yes)$'
}

function Resolve-PlatformPath([string]$RelKey) {
    $rel = $PlatformRun[$RelKey]
    if (-not $rel) { throw "platform-run.properties missing: $RelKey" }
    return Join-Path $ProjectRoot ($rel -replace '/', [IO.Path]::DirectorySeparatorChar)
}

# --- Ports & URLs ---
$PlatformGatewayPort = Get-PlatformInt 'GATEWAY_PORT'
$PlatformOrderPort = Get-PlatformInt 'ORDER_PORT'
$PlatformRiskPort = Get-PlatformInt 'RISK_PORT'
$PlatformJobPort = Get-PlatformInt 'JOB_PORT'
$PlatformAccountPort = Get-PlatformInt 'ACCOUNT_PORT'
$PlatformVitePort = Get-PlatformInt 'VITE_PORT'
$PlatformRedisHost = $PlatformRun['REDIS_HOST']
$PlatformRedisPort = Get-PlatformInt 'REDIS_PORT'
$PlatformH2TcpPort = Get-PlatformInt 'H2_TCP_PORT'
$PlatformH2JdbcTcp = $PlatformRun['H2_JDBC_TCP']
$PlatformKafkaBrokerPort = Get-PlatformInt 'KAFKA_BROKER_PORT'

# --- K8s ---
$PlatformEnableK8s = Get-PlatformBool 'ENABLE_K8S'
$PlatformK8sCluster = $PlatformRun['K8S_CLUSTER']
$PlatformK8sContext = $PlatformRun['K8S_CONTEXT']
$PlatformK8sNamespace = $PlatformRun['K8S_NAMESPACE']
$PlatformK8sOverlay = $PlatformRun['K8S_OVERLAY']
$PlatformK8sTlsInsecure = Get-PlatformBool 'K8S_TLS_INSECURE'
$PlatformK8sGatewayPfLocal = Get-PlatformInt 'K8S_GATEWAY_PF_LOCAL'
$PlatformToolsDir = Resolve-PlatformPath 'K8S_TOOLS_REL'
$PlatformKubeConfig = Resolve-PlatformPath 'K8S_KUBECONFIG_REL'
$PlatformK8sImages = @(
    ($PlatformRun['K8S_IMAGES'] -split ',') | ForEach-Object { $_.Trim() } | Where-Object { $_ }
)
$PlatformDockerHostilePattern = $PlatformRun['DOCKER_HOSTILE_NAME_PATTERN']

function Get-PlatformServiceChecks {
    return @(
        @{ Name = 'Vite frontend'; Port = $PlatformVitePort; Url = "http://localhost:$PlatformVitePort/login"; Hint = 'npm run dev 或 ensure-demo-links' },
        @{ Name = 'Order'; Port = $PlatformOrderPort; Url = "http://localhost:$PlatformOrderPort/actuator/health"; Hint = ':order-service:bootRun' },
        @{ Name = 'Risk'; Port = $PlatformRiskPort; Url = "http://localhost:$PlatformRiskPort/actuator/health"; Hint = ':risk-service:bootRun' },
        @{ Name = 'Gateway'; Port = $PlatformGatewayPort; Url = "http://localhost:$PlatformGatewayPort/actuator/health"; Hint = ':gateway:bootRun' }
    )
}

function Use-PlatformKube {
    New-Item -ItemType Directory -Force -Path $PlatformToolsDir | Out-Null
    $env:KUBECONFIG = $PlatformKubeConfig
}

function Export-PlatformKube {
    param(
        [Parameter(Mandatory = $true)][string]$KindCmd,
        [string]$ClusterName = $PlatformK8sCluster
    )
    & $KindCmd export kubeconfig --name $ClusterName --kubeconfig $PlatformKubeConfig | Out-Null
    kubectl config use-context $PlatformK8sContext --kubeconfig $PlatformKubeConfig | Out-Null
    if ($PlatformK8sTlsInsecure) {
        kubectl config set-cluster $PlatformK8sContext --insecure-skip-tls-verify=true --kubeconfig $PlatformKubeConfig | Out-Null
    }
    Use-PlatformKube
}

function Test-PlatformDockerHostileName([string]$ContainerName) {
    if (-not $PlatformDockerHostilePattern) { return $false }
    return $ContainerName -match $PlatformDockerHostilePattern
}

function Get-PlatformDockerServerArch {
    $arch = ''
    try { $arch = (docker version --format '{{.Server.Arch}}' 2>$null).Trim().ToLower() } catch { }
    if (-not $arch) {
        $info = docker info 2>&1 | Out-String
        if ($info -match '(?m)^\s*Architecture:\s*(\S+)') { $arch = $Matches[1].ToLower() }
    }
    if ($arch -in @('arm64', 'aarch64')) { return 'arm64' }
    if ($arch -in @('amd64', 'x86_64')) { return 'amd64' }
    return 'amd64'
}

function Get-PlatformDockerBuildPlatform {
    # 【概念】Docker Desktop 上的映像 arch 必須 = kind 節點 arch；否則 Desktop 看得到、K8s Pod 拉不到
    $cfg = $PlatformRun['DOCKER_BUILD_PLATFORM']
    if ($cfg -and $cfg -ne 'auto') { return $cfg.Trim() }
    $arch = Get-PlatformDockerServerArch
    if ($arch -eq 'arm64') { return 'linux/arm64' }
    return 'linux/amd64'
}

function Get-PlatformKindWindowsArch {
    $plat = Get-PlatformDockerBuildPlatform
    if ($plat -eq 'linux/arm64') { return 'arm64' }
    return 'amd64'
}

function Get-PlatformKindNodeName {
    return "$PlatformK8sCluster-control-plane"
}

function Assert-PlatformImageInKind([string]$ImageRef) {
    $node = Get-PlatformKindNodeName
    $repo = ($ImageRef -split ':')[0]
    $out = docker exec $node crictl images 2>&1 | Out-String
    if ($out -notmatch [regex]::Escape($repo)) {
        throw @(
            "kind node missing $ImageRef after load."
            "Build platform=$(Get-PlatformDockerBuildPlatform) must match kind node (docker exec $node uname -m)."
            "Fix: .\demo\start-k8s-demo.ps1  (rebuild + kind load)"
        ) -join ' '
    }
}

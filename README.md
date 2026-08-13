# FinTechDemo

可演示的分散式交易 Demo。繼承 EngineeringOS eos-minimal @ **0.1.10**。

## 目錄架構

| 目錄 | git | 用途 |
|------|-----|------|
| `scripts/` | **是（Pure）** | 下載＋JDK 21 → `check`／`bootRun` |
| `demo/` | 是（本專案專屬） | LOOP／pipeline／doctor |
| `docs/tools/` | 是（本專案專屬） | 文件伺服／HTML |
| `order-service/` 等 | 是 | 業務模組 |
| `.*` 隱藏檔 | **否** | 本機 only（僅 `.gitignore`） |

## Step1 — Pure（任何 OS）

```powershell
.\scripts\check.ps1
.\gradlew.bat :order-service:bootRun
```

```bash
./scripts/check.sh
./gradlew :order-service:bootRun
```

IntelliJ：Open **專案根** → SDK 21 → Gradle Sync → Gradle 窗 **bootRun**。

## Demo（可選）

```powershell
.\開啟Demo.cmd
# 或
.\demo\ensure-demo-links.ps1
.\demo\doctor-demo.ps1 -Fix
```

## 從公版對齊（workspace 有 EngineeringOS 時）

```powershell
cd ..\EngineeringOS\eos-minimal\hooks
.\apply-workspace.ps1 -WithDemo
```

## 文件入口

| 入口 | 路徑 |
|------|------|
| 規格 | [FinTechDemo-SPEC.md](FinTechDemo-SPEC.md) |
| 學習書櫃 | [docs/index.html](docs/index.html)（`.\docs\tools\serve-docs.ps1`） |
| Pure scripts | [scripts/README.md](scripts/README.md) |
| Demo | [demo/README.md](demo/README.md) |
| 公版操作 | `EngineeringOS/eos-minimal/knowledge/apply-workspace.md` |

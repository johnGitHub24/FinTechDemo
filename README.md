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

## Demo（可選 · 擇一）

```powershell
.\開啟Demo.cmd       # 本機 bootRun／Vite／Gateway
.\開啟K8sDemo.cmd    # kind 全棧（Docker Ready；不起 Vite）
# 詳見 demo/README.md · 藍圖 #k8s-intellij §⓪
```

## 從公版對齊（workspace 有 EngineeringOS 時）

```powershell
cd ..\EngineeringOS\eos-minimal\hooks
.\apply-workspace.ps1 -WithDemo
```

## 文件入口

單一入口：本 README。衝突以主規格為準。

| 文件 | 說明 |
|------|------|
| [FinTechDemo-SPEC.md](FinTechDemo-SPEC.md) | **主規格（權威）** |
| [docs/index.html](docs/index.html) | 學習書櫃（本專案唯一 HTML 入口） |
| [docs/文件完整度.md](docs/文件完整度.md) | **文件地圖／單一真相／勿重複維護** |
| [scripts/README.md](scripts/README.md) | Pure check／bootRun |
| [demo/README.md](demo/README.md) | Demo（可選）· `platform-run.properties` |
| [CLAUDE.md](CLAUDE.md) | AI 薄規則 |
| 藍圖 SPA | http://localhost:5173/blueprint · 前端頁面：`#frontend-pages` · K8s：`#k8s-intellij` |
| 雙庫／H2／Redis | [db.html](docs/architecture/db.html)（TCP 9093／9094、僅 account 連 :6379）· 手冊 [handbook.html#ch05](docs/portals/handbook.html#ch05) |
| [docs/portals/handbook.html](docs/portals/handbook.html) | 完整學習手冊（第 10 章＝工程規範） |
| [docs/guides/engineering-norms.html](docs/guides/engineering-norms.html) | SDD／TDD／Harness 三道門（hooks／pipeline／doctor） |
| 前端頁面（HTML） | [k8s-complete-guide.html §11](docs/guides/k8s-complete-guide.html#s11) |


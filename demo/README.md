# demo/ — FinTechDemo only（Platform Demo 層）

Canonical: `EngineeringOS/.../optional-demo-scripts/demo/`  
Sync: `apply-workspace.ps1 -WithDemo`  
**Do not copy to other projects.** Pure 層請用 [`../scripts/`](../scripts/)。

**互動教學（逐支說明 + Mermaid）：** [`docs/portals/demo-scripts.html`](../docs/portals/demo-scripts.html)  
（`.\docs\tools\serve-docs.ps1` → http://127.0.0.1:5500/docs/portals/demo-scripts.html）

---

## 命名原則（看檔名就知道類別）

| 前綴／模式 | 類別 | 做什麼 |
|------------|------|--------|
| `platform-*` | **設定／函式庫** | 埠、K8s、kubeconfig 常數 |
| `ensure-*`、`開啟Demo.cmd` | **本機啟動（權威）** | 拉起 bootRun／Vite／Gateway… |
| `start-k8s-*`、`k8s-*`、`check-k8s`、`開啟K8sDemo.cmd` | **K8s** | kind／walkthrough／只驗 YAML |
| `doctor-*`、`verify-*`、`smoke-*`、`check-*` | **診斷／驗證** | 探針、閘門、連結檢查 |
| `run-*` | **壓測** | Locust |
| `start-monitoring-*`、`export-*` | **可選工具** | 本機監控、匯出圖 |
| `start-demo*`、`start-guide` | **相容舊名** | 薄轉發 → ensure／README（勿再擴功能） |

**勿再新增**第二套「一鍵就緒」長腳本；要加能力請改 `ensure-demo-links.ps1` 或 `start-k8s-demo.ps1`。

---

## 你平常只記這幾個

```powershell
.\開啟Demo.cmd                 # 本機（＝ ensure-demo-links -ForceRestart）
.\開啟K8sDemo.cmd              # kind（＝ start-k8s-demo；與本機擇一）
.\demo\doctor-demo.ps1 -Fix    # 診斷＋修復
.\demo\verify-pipeline.ps1     # check + compose + k8s 閘門
```

`ENABLE_K8S=false`（預設）。Vue／npm **不能**起 kind。  
本機產物 → `demo/.tools/`（隱藏，不上 git）。

---

## 腳本目錄（完整）

### 0. 設定／函式庫（必須）

| 檔案 | 目的 |
|------|------|
| `platform-run.properties` | 埠、K8s、DOCKER_BUILD_PLATFORM 單一真相 |
| `platform-env.ps1` | 供其他腳本 dot-source |

### 1. 本機啟動（必須 · 權威實作只有 ensure）

| 檔案 | 目的 |
|------|------|
| `ensure-demo-links.ps1` | LOOP 拉起服務＋預設 Redis；`開啟Demo.cmd`／`DemoStackBootstrap -FromOrder` 呼叫 |
| `start-demo-ready.ps1` | **相容**：轉發 ensure |
| `start-demo.ps1` | **相容**：轉發 ensure（或 `-Minimal` 印指引） |
| `start-guide.ps1` | **相容**：印入口說明 |

### 2. K8s（必須 · 與本機擇一）

| 檔案 | 目的 |
|------|------|
| `start-k8s-demo.ps1` | kind + build/load + apply（`開啟K8sDemo.cmd`） |
| `k8s-walkthrough.ps1` | Images／kind／Pods 三層檢查 |
| `check-k8s.ps1` | 只 kustomize 驗 YAML（不連叢集） |

### 3. 診斷／驗證（必須）

| 檔案 | 目的 |
|------|------|
| `doctor-demo.ps1` | 埠／health 診斷；`-Fix` → ensure |
| `verify-demo-shortcuts.ps1` | 只探測 Demo 快捷 URL（不啟動） |
| `verify-pipeline.ps1` | 推版閘門：check + compose + k8s |
| `smoke-distributed.ps1` | 分散式／Kafka 路徑煙霧 |
| `check-docs-links.ps1` | docs HTML 壞連結掃描 |

### 4. 壓測／可選工具

| 檔案 | 目的 |
|------|------|
| `run-loadtest.ps1` | Locust |
| `start-monitoring-local.ps1` | 無 Docker 時本機 Prom／Grafana |
| `export-blueprint-diagrams.ps1` | 藍圖 Mermaid 匯出 PNG／SVG |

---

## 為什麼還有這麼多？（精簡後結論）

| 保留 | 原因 |
|------|------|
| ensure + doctor + verify-pipeline | 啟動／修／閘門 **三種職責**，不能併成一支還好用 |
| start-k8s + walkthrough + check-k8s | 部署／三層檢查／純 YAML **三種層級** |
| smoke / loadtest / docs-links / monitoring / export | 場景不同，體積小、按需用 |

**已精簡：** 刪除第二套完整啟動邏輯（`start-demo-ready`／`start-demo`／`start-guide` 改薄轉發），避免與 ensure 雙維護。

改埠或 K8s 叢集名：**只改** `platform-run.properties`。

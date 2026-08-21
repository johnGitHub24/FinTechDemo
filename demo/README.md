# demo/ — FinTechDemo only

Canonical: `EngineeringOS/.../optional-demo-scripts/demo/`  
Sync: `apply-workspace.ps1 -WithDemo`  
**Do not copy to other projects.**

## Run Anywhere 兩層

| 層 | 檔案 | 用途 |
|----|------|------|
| Pure | [`../scripts/`](../scripts/) | JDK、`check`、IntelliJ `bootRun`（全 workspace 對齊） |
| Platform Demo | `platform-run.properties` + `platform-env.ps1` | 埠號、K8s、Redis、H2 TCP、kubeconfig 路徑 |

改埠或 K8s 叢集名：**只改** `platform-run.properties`，Demo 腳本 dot-source `platform-env.ps1`。

`ENABLE_K8S=false`（預設）＝本機一鍵。K8s 請雙擊專案根 **`開啟K8sDemo.cmd`**（與本機擇一，勿雙棧搶 RAM）。Vue／npm **不能**起 kind。

```powershell
.\開啟Demo.cmd          # 本機 bootRun／Vite／Gateway
.\開啟K8sDemo.cmd       # kind 全棧（Docker Desktop Ready）
.\demo\doctor-demo.ps1 -Fix
.\demo\verify-pipeline.ps1
```

本機產物（kind、kubeconfig）→ `demo/.tools/`（隱藏目錄，不上 git）。

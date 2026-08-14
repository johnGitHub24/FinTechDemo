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

`ENABLE_K8S=false`（預設）＝只本機 bootRun／Vite；`true` 或 `.\demo\ensure-demo-links.ps1 -EnableK8s` 才順便跑 `start-k8s-demo.ps1`。Vue／npm **不能**起 kind。

文件：**[docs/文件完整度.md](../docs/文件完整度.md)** · 藍圖：http://localhost:5173/blueprint#frontend-pages · K8s：http://localhost:5173/blueprint#k8s-intellij

```powershell
.\開啟Demo.cmd
.\demo\doctor-demo.ps1 -Fix
.\demo\start-k8s-demo.ps1
.\demo\verify-pipeline.ps1
```

本機產物（kind、kubeconfig）→ `demo/.tools/`（隱藏目錄，不上 git）。

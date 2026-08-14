# docs/

> **推版前先看**：[文件完整度.md](文件完整度.md)  
> **瀏覽入口**：[index.html](index.html)（`.\docs\tools\serve-docs.ps1` → :5500）

| 類型 | 路徑 | 說明 |
|------|------|------|
| 權威規格 | 根目錄 `FinTechDemo-SPEC.md` | 產品／Phase |
| 互動教學 | Vite `/blueprint` | Docker／K8s Mermaid、複製指令 |
| 靜態 HTML | `docs/**/*.html` | 書櫃、k8s-tips、stages |
| 可貼 MD | `docs/architecture/*.md` | Redmine／GitHub |
| 源稿（本機） | `docs/_md/` | gitignore → `generate-docs-html.ps1` |

勿維護第二份相同指令長文；改 SPA 或 `demo/platform-run.properties`。

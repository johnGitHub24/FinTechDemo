# FinTechDemo — 統一文件入口（docs/index.html）設計

**日期**：2026-08-06  
**狀態**：Approved（方案 A + `docs/index.html`）  
**目標**：學習用單一入口，設定／部署／教學一次找齊；API 連到既有 `swagger.html`。

## 範圍

| 區塊 | 內容 | 呈現 |
|------|------|------|
| API | `swagger.html`、Runtime Swagger、`openapi.yaml`、`API規格書.md` | **連結**（不重做 Swagger UI） |
| 應用設定 | 各服務 `application.yml`／`application-demo.yml` | 側欄選 → 主區 fetch 顯示 |
| Compose／K8s | `docker-compose.yml`、`deploy/k8s/**` | 同上 |
| 教學 HTML | 部署指導、S0–S6、啟動流程、學習手冊、codeGraphic | 連結開頁 |
| 架構／設計 MD | architecture、分散式、engineering-config、testing… | 連結或文字預覽 |
| 排除 | `build/`、`node_modules/`、`docs/javadoc/**` 細節（只留 javadoc 總入口） | |

## UX

- 左：搜尋 + 分組清單；右：標題／路徑／內容或「開啟」按鈕  
- `file://` 或僅 serve `docs/` 時 fetch 失敗 → 提示改從**專案根目錄** `npx --yes serve -p 5500` 再開 `/docs/`  
- 清單以頁內 `CATALOG` 維護（可預測、可教學註解）

## 非目標

- 不取代 Runtime Actuator configprops  
- 不自動掃磁碟（日後可升方案 B）

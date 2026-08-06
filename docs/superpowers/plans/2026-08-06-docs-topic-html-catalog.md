# Docs 主題目錄 + catalog HTML Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 以 `docs/catalog.yaml` 驅動主題目錄（MD+HTML 並存）、靜態 HTML 產生、舊 MD stub 導向，並更新書櫃／閱讀器連結。

**Architecture:** 單一 catalog 為權威清單；generate-docs-html 讀 catalog 產出 HTML 與 legacy stubs；index／md-reader 改連或讀同一 catalog；手寫頁移入 `portals/`。

**Tech Stack:** Node（marked + js-yaml via npx）、既有靜態 HTML／Mermaid CDN、PowerShell wrapper。

---

## 檔案對照

| 路徑 | 職責 |
|------|------|
| `docs/catalog.yaml` | 唯一清單 |
| `scripts/generate-docs-html.mjs` | 讀 catalog、產 HTML、寫 stub |
| `scripts/generate-docs-html.ps1` | 呼叫 npx marked+js-yaml |
| `docs/guides|architecture|deploy/*` | 主題 MD＋產生 HTML |
| `docs/portals/*` | 手寫互動頁 |
| `docs/index.html` | 書櫃連結 |
| `docs/md-reader.html` | 側欄讀 catalog 或對齊 path |
| 根目錄 legacy stubs | 舊 URL 導向 |

### Task 1: catalog.yaml + 目錄骨架

- [ ] 建立 `docs/guides`、`architecture`、`deploy`、`portals`
- [ ] 寫入完整 `docs/catalog.yaml`（現有學習 MD + portals + legacy）

### Task 2: 搬移 MD 到主題目錄

- [ ] 從根／`pages/` 複製權威內容到 `guides|architecture|deploy/*.md`（ASCII）
- [ ] 根中文 MD、`pages/*.md` 列為 legacy（之後由產生器寫 stub）

### Task 3: 重寫 generate-docs-html.mjs

- [ ] 讀 catalog.yaml；產生 HTML 到指定路徑；相對連結與 Mermaid theme 修正
- [ ] 寫 legacy stub；更新 `.ps1` 依賴（marked + js-yaml）
- [ ] 跑腳本驗證產出

### Task 4: 搬移 portals + 根 stub

- [ ] 手寫 HTML 移入 `portals/`；根目錄 ASCII 名留 redirect
- [ ] 更新頁內相對連結（index／彼此）

### Task 5: 更新 index + md-reader

- [ ] 書櫃 path 改主題 HTML
- [ ] md-reader DOCS path 對齊 catalog（或 fetch catalog.yaml）

### Task 6: 驗收

- [ ] HTTP 檢查 why.html、舊 why.md stub、index、portals/learning-map
- [ ] 確認非 raw Markdown

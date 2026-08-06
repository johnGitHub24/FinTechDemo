# Docs 主題目錄 + 靜態 HTML（catalog 驅動）設計

> 狀態：已定案（2026-08-06）  
> 決策來源：brainstorm（閱讀 C、目錄 A、MD 位置 A、實作 A）

## 問題

直開 `docs/*.md`（含中文檔名）時，靜態伺服器回傳 raw Markdown，無法閱讀。文件散落根目錄與 `pages/`，難以分門別類管理。

## 目標

1. 每個學習用 MD 產生可書籤的靜態 HTML（含 Mermaid）。
2. `docs/` 依主題分子目錄；MD 與 HTML 並存。
3. 保留 `md-reader.html`；書櫃／地圖連結改指 HTML。
4. 舊中文／舊路徑 `.md` → stub 導向對應 HTML。
5. 以 `docs/catalog.yaml` 為唯一清單。

## 目錄

```
docs/
  catalog.yaml
  index.html
  md-reader.html
  guides/          # 學習／劇情／SPEC／為什麼
  architecture/    # 架構／DB／API／設定／測試設計
  deploy/          # 部署／K8s／階段
  portals/         # 手寫互動 HTML
  superpowers/     # 設計備註（維持）
  javadoc/         # 不動
```

根目錄舊中文 `.md`／舊 ASCII portal 檔名 → redirect stub。

## catalog.yaml

每篇 docs 條目：`id`、`section`、`title`、`md`、`html`、`legacy[]`（可選）、`group`（書櫃分類）。  
`portals` 區塊：手寫頁（不產自 MD）。

## 產生器

`scripts/generate-docs-html.mjs`（由 `.ps1` 呼叫）：

- 讀 catalog → MD → HTML（marked + mermaid 區塊）
- 輸出到條目指定的 `html` 路徑
- 為每個 `legacy` 寫 stub
- 頁內 `.md` 連結依 catalog 改寫為對應 HTML
- Mermaid 使用淺色 `theme: base` + themeVariables

## 連結

- `index.html` 書櫃：學習 MD → `/docs/{section}/{id}.html`
- `md-reader.html`：從 catalog 建側欄；`path` 指向主題目錄 MD
- portals／學習地圖內 raw `.md` 連結改為 HTML

## 不做

- 不改 javadoc 流程
- 不把 compose／k8s YAML 轉成 HTML 說明頁
- 不上 MkDocs／Docusaurus
- 不刪 repo 根 `FinTechDemo-SPEC.md`（與 `guides/spec.md` 同步）

## 成功標準

- `/docs/guides/why.html` 可讀排版 HTML
- 舊 `/docs/技術次序與架構為什麼.md` 導向 HTML，非 raw
- `/docs/index.html` 架構類連到主題 HTML
- `.\scripts\generate-docs-html.ps1` 可重跑

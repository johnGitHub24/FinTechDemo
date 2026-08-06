# System Blueprint Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** SPA `/blueprint` 頁以 HTML＋Mermaid 完整展示技術棧、分層、Gateway／服務運作過程與訂單狀態機，登入前後皆可進。

**Architecture:** `BlueprintView` 靜態 HTML 區塊；mermaid 字串集中於 `blueprint/diagrams.js`；路由無 `requiresAuth`；App nav＋Login 連結。

**Tech Stack:** Vue 3、Vue Router、mermaid（npm，動態 import）、既有 styles.css

**Spec:** `docs/superpowers/specs/2026-08-06-system-blueprint-design.md`

---

### Task 1: 依賴

- Modify: `frontend/package.json`
- [x] `npm install mermaid`（於 `frontend/`）

### Task 2: 圖碼與頁面

- Create: `frontend/src/blueprint/diagrams.js`
- Create: `frontend/src/views/BlueprintView.vue`
- Modify: `frontend/src/styles.css`
- [x] 版本表常數＋三張 mermaid（分層／過程／狀態機）＋S1–S3＋埠表
- [x] `onMounted` → dynamic `import('mermaid')` → `run`

### Task 3: 路由與導覽

- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/App.vue`
- Modify: `frontend/src/views/LoginView.vue`
- [x] `/blueprint` 無 auth；nav＋login 連結

### Task 4: 驗證

- [x] `npm run build` 成功
- [ ] 手動：未登入／已登入皆可開藍圖 → http://localhost:5173/blueprint

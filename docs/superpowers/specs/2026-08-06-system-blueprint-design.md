# FinTechDemo — 系統運作藍圖（SPA）設計

> 日期：2026-08-06  
> 狀態：已實作（2026-08-06）  
> 目標：Demo 時一點導覽／登入頁連結即可講清「用了哪些技術、分層、Gateway 與狀態過程」，不必另開說明文件

---

## 1. 問題與成功標準

### 問題

技術與運作過程分散在 docs／banner／PROCESS FLOW；現場 Demo 常需另開文件。希望在 SPA 內有一頁 **系統運作藍圖**，用 **HTML 區塊＋Mermaid** 完整表達導入技術與過程。

### 成功標準

1. 登入後導覽列「交易前台」旁有「系統運作藍圖」→ `/blueprint`
2. **未登入也可開** `/blueprint`；登入頁有連結（方案 C）
3. 頁面以 HTML 呈現完整技術架構與系統過程（含版本）
4. 含：技術棧版本、分層架構圖、完整運作過程（Gateway／各服務／狀態）、訂單狀態機、S1–S3、埠表
5. 不依賴 docs `:5500` 伺服器

---

## 2. 方案

採用 **方案 A**：Vue 路由頁 `BlueprintView` + `mermaid`（npm）於 `onMounted` 渲染頁內圖碼為 SVG。

否決：純 `public/` 靜態脫離導覽（B）；iframe 嵌 docs（C，仍依賴另開服務）。

---

## 3. 入口與權限

| 項目 | 規格 |
|------|------|
| 路由 | `/blueprint` → `BlueprintView.vue` |
| Auth | **不需** `requiresAuth`；已登入亦可進 |
| 導覽列 | `App.vue`：登入後顯示「系統運作藍圖」（鄰近「交易前台」） |
| 登入頁 | `LoginView.vue`：連結至 `/blueprint` |
| 未登入 UI | 不顯示完整 `app-nav`；頁內簡短頂列（品牌＋回登入）即可 |

---

## 4. 頁面內容（HTML 區塊順序）

整頁為可捲動 HTML；每區塊：`h2`／說明／（可選）`<pre class="mermaid">` 或 table。

### 4.1 技術棧與版本表

靜態常數（對齊 repo，實作時再核對一次）：

| 層 | 技術 | 版本（權威來源） |
|----|------|------------------|
| 前端 | Vue | ^3.5（`frontend/package.json`） |
| 前端 | Vue Router | ^4.5 |
| 前端 | Axios | ^1.7 |
| 前端 | Vite | ^6 |
| 後端 | Java | 21（toolchain） |
| 後端 | Spring Boot | 3.2.2 |
| 後端 | Spring Cloud | 2023.0.0 |
| 後端 | Gateway | spring-cloud-gateway-server-mvc |
| 後端 | OpenFeign | spring-cloud-starter-openfeign |
| 後端 | Security + JJWT | starter-security + jjwt 0.12.5 |
| 後端 | JPA / H2 | starter-data-jpa + h2 |
| 後端 | springdoc OpenAPI | 2.3.0 |
| 後端 | Actuator + Prometheus | micrometer-registry-prometheus |
| 可選基建 | Kafka | spring-kafka（order／account） |
| 可選基建 | Redis | starter-data-redis（account） |
| 可選基建 | PostgreSQL | prod 敘述（local 預設 H2） |
| 排程 | Job service | `@Scheduled` 風格逾時取消（:8083） |

### 4.2 分層架構 Mermaid

`flowchart`／subgraph：Browser（Vue）→ 直連 Order 或經 Gateway → Order／Risk／Account／Job → H2／Redis／Kafka。邊標註協定（HTTP／JWT／Feign／Kafka）。

### 4.3 完整運作過程 Mermaid（核心 Demo 敘事）

必須涵蓋並用 HTML／邊標註技術：

1. Login → JWT／RBAC（Security + JJWT）
2. 下單 → Order :8081 → 狀態 **PENDING**（JPA）
3. （可選）經 Gateway :8080 轉發（Gateway MVC + `X-Demo-Via-Gateway`）
4. 成交 → Order Feign → Risk :8082 名目風控
5. 通過 → **ACCEPTED**；拒絕 → **REJECTED**
6. （可選）Account :8084 套用成交／Redis／Kafka
7. （可選）Job :8083 逾時 → **CANCELLED**

每環節旁註「誰／埠／用到的框架」。

### 4.4 訂單狀態機 Mermaid

`stateDiagram-v2`：PENDING、ACCEPTED、REJECTED、CANCELLED；轉移條件一句話（與 PROCESS FLOW NOTE 一致）。

### 4.5 部署階梯 S1–S3（HTML NOTE）

| 階 | 條件 | 意義 |
|----|------|------|
| S1 | Order | 可登入／建單 |
| S2 | Order + Risk | 最短可成交 |
| S3 | S2 + Gateway **或** Account | 分散式敘事 |
| — | Job | **不進** S 公式 |
| S4+ | 文件／手動 | 面板不自動宣稱 K8s |

### 4.6 埠對照表

Gateway 8080、Order 8081、Risk 8082、Job 8083、Account 8084、Vue 5173。

### 4.7 明確不做

- 不嵌 `BackendStoryPanel`（即時燈號仍在 Trade／Portal）
- 不呼叫 topology API（藍圖為靜態說明）
- 不新增 Node BFF

---

## 5. 實作邊界

### 前端檔案

- `frontend/src/views/BlueprintView.vue` — HTML 區塊＋ mermaid 容器
- 可選 `frontend/src/blueprint/diagrams.js` — mermaid 字串集中，利於維護
- `frontend/src/router/index.js` — 註冊 `/blueprint`
- `frontend/src/App.vue` — nav link
- `frontend/src/views/LoginView.vue` — 藍圖連結
- `frontend/package.json` — 依賴 `mermaid`
- `frontend/src/styles.css` — `.blueprint` 閱讀寬度／表格

### Mermaid 載入

- `import mermaid from 'mermaid'`
- `mermaid.initialize({ startOnLoad: false, theme: 'neutral' })`
- `onMounted` → `mermaid.run({ nodes: ... })`
- `onUpdated` 若無動態改圖則不需重跑

### 測試

- 路由：未登入可開 `/blueprint`；登入後 nav 可見
- 手動：三張圖能渲染；版本表數字與 gradle／package.json 一致
- 無強制 E2E（專案現況）

---

## 6. 驗收 Checklist

- [ ] `/blueprint` 未登入可開
- [ ] 登入後 nav「系統運作藍圖」可進
- [ ] 登入頁有連結
- [ ] 技術版本表完整且對齊 repo
- [ ] 分層架構、完整運作過程、狀態機三圖可見
- [ ] Gateway／Risk／Account／Job／Kafka／Redis 皆在過程敘事中出現
- [ ] 不依賴 docs server

---

## 7. 後續

實作前：`writing-plans` 產出 `docs/superpowers/plans/2026-08-06-system-blueprint.md`。  
本倉庫若無 git，設計檔以工作區檔案為準（無法 commit）。

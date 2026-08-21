# 系統運作藍圖 — 完整教學導覽

> **互動頁（權威呈現）**：http://127.0.0.1:5173/blueprint  
> **怎麼開**：`cd frontend` → `npm run dev`  
> **面試簡版**：[interview-demo-simple.md](./interview-demo-simple.md)

藍圖 = 一頁講完 **怎麼開、怎麼跑、技術棧、流程、K8s**；每段有 Mermaid、表格、可複製指令。

---

## 0. 怎麼進入藍圖

| 步驟 | 動作 |
|------|------|
| 1 | 專案根 `cd frontend` |
| 2 | `npm run dev` |
| 3 | 瀏覽器 http://127.0.0.1:5173/blueprint |
| 4 | 用頁首 **目錄** 或右側 **導覽框** 跳區塊 |

可不登入瀏覽；要實際下單請另開 `/login` 並先 `.\開啟Demo.cmd`。

---

## 1. 區塊地圖（建議講解順序）

| 錨點 | 標題 | 講什麼 | 面試重點 |
|------|------|--------|----------|
| `#docker-start` | Docker／本機怎麼開 | Desktop Ready → `開啟Demo.cmd` | 最短 Demo 入口 |
| `#frontend-pages` | **前端 HTML 頁面關係** | index.html → App.vue → Router | **/blueprint 不是獨立 HTML** |
| `#frontend-image` | 前端 Docker 映像／ENABLE_K8S | Dockerfile、compose、properties | 打包與 K8s 開關 |
| `#docker-redis` | Docker／Redis | redis-cli 指令、IntelliJ 連 :6379（與 Demo 面板 Docker 分頁同源元件） | 基建除錯 |
| `#stack` | 技術棧 | Vue / Boot / Cloud / JWT / H2… | 版本與選型 |
| `#layers` | 分層架構 | 前端→Gateway→微服務→DB | **架構一張圖** |
| `#mechanisms` | 邊緣機制 | 限流、CORS、Redis Cache | 進階加分 |
| `#flow` | 完整運作過程 | Login→下單→Feign Risk→狀態 | **業務主路徑** |
| `#states` | 訂單狀態機 | PENDING/ACCEPTED/REJECTED/CANCELLED | 領域模型 |
| `#stages` | S1–S3 | 環境開到哪（綠燈公式） | Demo 敘事 |
| `#ports` | 埠對照 | 8080～8084、5173、6379… | 除錯必備 |
| `#observe` | 觀測／壓測 | Locust、Prometheus、Grafana | 可選 |
| `#k8s-intellij` | K8s／IntelliJ | 三層 Docker→kind→Pod、兩套大樓 | **部署主場** |
| `#k8s-verify` | K8s 驗證 | 四條 kubectl 一鍵複製 | 現場驗證 |

---

## 2. 各區塊詳述

### `#docker-start` — 本機最短可成交

1. Docker Desktop **Ready**
2. 專案根雙擊 **`開啟Demo.cmd`** 或 PowerShell 執行
3. 瀏覽器 http://localhost:5173/login（trader1/password）

可選：監控 compose profile、Locust 壓測（見區塊內複製鈕）。

---

### `#frontend-pages` — 前端 HTML／頁面關係

**重點：** `http://localhost:5173/blueprint` **不是**一份獨立 `.html` 檔。

流程：`index.html` → 掛載 `#app` → `App.vue`（殼）→ `router-view` 依 URL 換 View。

| URL | 檔案 | 誰能開 |
|-----|------|--------|
| `/login` | `LoginView.vue` | 公開 |
| `/blueprint` | `BlueprintView.vue` | 公開（本頁） |
| `/trade` | `TradeView.vue` | 需 JWT |
| `/portal` | `PortalView.vue` | 需 JWT |
| `/portal/audit` | `AuditView.vue` | 需 ADMIN |
| `/demo/risk-check.html` | `public/demo/` | 靜態 HTML，**不經** Router |
| `/demo/account-me.html` | `public/demo/` | 同上 |

登入後頂欄在 `App.vue`；`router-view` 只換中間內容。路由定義：`frontend/src/router/index.js`。

靜態書櫃同表：[k8s-complete-guide.html §11](./k8s-complete-guide.html#s11)

---

### `#frontend-image` — 前端打包成 Docker 映像

| 模式 | 怎麼跑前端 | `/api` 打到哪 |
|------|------------|---------------|
| Local 日常 | `npm run dev` 或 `開啟Demo.cmd` | Vite proxy → **localhost:8081**（Order bootRun） |
| Docker 映像 | `docker compose --profile full up -d frontend` | nginx → **gateway:8080** |
| kind 四 Pod | 目前**無** frontend Deployment | Vite + **port-forward** |

```powershell
docker build -t fintech-demo/frontend:local ./frontend
docker compose --profile full up -d frontend
```

**ENABLE_K8S**（`demo/platform-run.properties`，非 Vue yaml）：

| 值 | 效果 |
|----|------|
| `false`（預設） | 只本機 Order／Risk／Vite |
| `true` | `開啟Demo.cmd` 再跑 `start-k8s-demo.ps1` |

詳述：[k8s-complete-guide.html §12～§13](./k8s-complete-guide.html#s12)

---

### `#docker-redis` — Redis 指令教學

- 教 **提示字元**：PowerShell vs redis-cli vs 容器 shell
- 指令可一鍵複製：`docker compose up -d redis`、`PING`、`KEYS *`
- Account 可選 Redis cache-aside（:6379）

---

### `#stack` — 技術棧與版本

分框：**前端 / 後端 / 基建 / 排程**

- 權威來源：`package.json`、`build.gradle`
- 每列：**主要目的** + **本專案怎麼用**
- 名詞：BOM、OpenFeign、JJWT vs JWT

**講法：** 「不是堆套件，是每個技術掛在 Demo 哪個埠、哪條路徑。」

---

### `#layers` — 分層架構（Mermaid）

- **實線**：Vue :5173 → Order :8081 → Feign Risk :8082
- **虛線**：Gateway、Kafka、Job、Redis（可選）
- 區塊下 **↔ 檔案對照**：各層對應 Java/Vue 路徑

**講法：** 「最短可成交不經 Gateway；講分散式時才開 :8080。」

---

### `#mechanisms` — 邊緣機制

| 機制 | 類別 | 做什麼 |
|------|------|--------|
| RateLimitWebFilter | Gateway | 入口限流 429 |
| WebConfig | Order | CORS 允許 Vue |
| Redis Cache | Account | cache-aside TTL 60s |

每張卡片含 **設定 key** 與 **怎麼 Demo**。

---

### `#flow` — 完整運作過程（Mermaid）

主線：

1. **Login** → JWT + Spring Security
2. **Create** → JPA → PENDING
3. **路徑** → 直連 Order 或 經 Gateway
4. **Execute** → Feign → Risk
5. **結果** → ACCEPTED / REJECTED
6. 可選 Account、Job 逾時

區塊下 **↔ 檔案對照**：Controller、TradingService、RiskClient 等。

---

### `#states` — 訂單狀態機

四態與轉換條件；與 `#stages`（環境 S1–S3）是 **兩件事**：

- **states**：這筆單到哪
- **stages**：環境開到哪

---

### `#stages` — S1–S3 部署階梯

| 階 | 條件 | 能做什麼 |
|----|------|----------|
| S1 | 僅 Order 綠 | 登入、建 PENDING |
| S2 | Order + Risk | **最短可成交** |
| S3 | + Gateway 或 Account | 統一入口／帳務 |

由 `TopologyService` 依 `/actuator/health` 推斷；交易頁右側可見。

---

### `#ports` — 埠對照表

8080 Gateway · 8081 Order · 8082 Risk · 8083 Job · 8084 Account · 5173 Vue · 6379 Redis…

---

### `#observe` — 觀測／壓測

Locust :8089 → Order；Prometheus :9090；Grafana :3000。進階章節，面試可略。

---

### `#k8s-intellij` — K8s 主章（最重）

建議子順序：

| 小節 | 內容 |
|------|------|
| ① | IntelliJ Services vs K8s（Docker 插件 ≠ K8s 插件） |
| ② | 三層 Mermaid：Images → kind load → Pod |
| ②b | **流程 ↔ YAML／腳本／程式** 對照表 |
| ②c | **兩套 K8s 大樓**（Panel 找不到 fintech-demo） |
| ③ | 一句話：Builds / Images / Containers / kubectl |
| ④～⑩ | check-k8s、start-k8s-demo、驗證、IntelliJ 連線 |

**關鍵腳本：**

```powershell
.\demo\start-k8s-demo.ps1
.\demo\k8s-walkthrough.ps1
kubectl -n fintech-demo get pods
```

**設定檔：** `demo/platform-run.properties`、`deploy/k8s/`、`Dockerfile.k8s-local`

延伸閱讀：

- [docker-desktop-k8s.md](./docker-desktop-k8s.md)
- [intellij-k8s.md](./intellij-k8s.md)

---

### `#k8s-verify` — 精簡驗證面板

四條指令一鍵複製：Docker ready → readyz → nodes → fintech-demo pods。

---

## 3. 藍圖 vs 其他文件

| 需求 | 去哪 |
|------|------|
| 產品規格 | `FinTechDemo-SPEC.md` |
| 文件總地圖 | `docs/文件完整度.md` |
| 靜態書櫃 | `docs/index.html`（:5500） |
| **前端頁面關係** | 藍圖 `#frontend-pages` · [k8s-complete-guide.html §11](./k8s-complete-guide.html#s11) |
| **現場講解用** | **本藍圖 SPA** |
| 貼 GitHub/Redmine | `docs/architecture/系統運作藍圖.md`（摘要，指向 SPA） |

---

## 4. 兩種 Demo 路線對照

| | 日常 bootRun | K8s kind |
|--|--------------|----------|
| 入口 | `開啟Demo.cmd` | `start-k8s-demo.ps1` |
| 前端 | :5173 直打 Order | port-forward Gateway + Vite |
| 給誰看 | 產品、RBAC、成交 | 部署、映像、Pod |
| 藍圖章節 | `#flow` `#stages` | `#k8s-intellij` |

---

## 5. 建議講解時間（完整版 25～30 分）

| 時間 | 區塊 |
|------|------|
| 3 min | `#docker-start` + 實際登入成交 |
| 5 min | `#layers` + `#flow` + `#states` |
| 3 min | `#mechanisms` + `#ports`（可快掃） |
| 10 min | `#k8s-intellij` + live walkthrough |
| 2 min | `#k8s-verify` + Q&A |

面試請改用 [interview-demo-simple.md](./interview-demo-simple.md)（10 分鐘版）。

---

## 6. 開藍圖 + 書櫃（簡報日）

```powershell
# 視窗 1 — 互動藍圖
cd FinTechDemo\frontend
npm run dev

# 視窗 2 — 靜態 MD（可選）
cd FinTechDemo
.\docs\tools\serve-docs.ps1
```

| URL | 內容 |
|-----|------|
| http://127.0.0.1:5173/blueprint | 本導覽互動版 |
| http://127.0.0.1:5173/blueprint#frontend-pages | 前端頁面關係（Mermaid） |
| http://127.0.0.1:5500/docs/guides/k8s-complete-guide.html#s11 | 前端頁面（靜態 HTML） |
| http://127.0.0.1:5500/docs/guides/blueprint-tour.md | 本檔 |
| http://127.0.0.1:5500/docs/guides/interview-demo-simple.md | 面試簡版 |

# FinTechDemo — Backend Story Panel（PROCESS FLOW 儀表板）設計

> 日期：2026-08-06  
> 狀態：已實作（2026-08-06 loop-engineering）  
> 目標：前台操作時一眼看懂「後端在做什麼、過程步驟職責、對應狀態轉換」

---

## 1. 問題與成功標準

### 問題

Demo 已有 microservice／Gateway／（可選）K8s 敘事，但 Trade／Portal 操作時**看不見後端過程**。展演與自學都難以回答：「這一筆到底走了誰？現在開到 S 幾？訂單狀態怎麼變？」

### 成功標準

1. 在 **Trade** 與 **Portal** 同屏可見 **PROCESS FLOW 儀表板**（不另開路由、不必切頁）。
2. 每個 process 步驟固定三欄：**誰**／**做什麼**／**對應狀態（轉換）**。
3. **事實**：服務 health 綠紅燈（由 order-service `GET /api/demo/topology` 伺服器端探測，避開瀏覽器跨埠 CORS；語意等同規格「環境事實」）；API 回傳 `demoTrace` 驅動當次流程高亮。
4. **敘事**：依 health 推斷 S1–S3；可選「釘住」講解階段（不假裝 S4–S6 每次 HTTP 都經 K8s）。
5. 不做 Zipkin／完整 APM／K8s API 探測（YAGNI）。

---

## 2. 方案選擇

| 方案 | 摘要 | 結論 |
|------|------|------|
| A 純前端 ping＋推斷路徑 | 快，路徑不可信 | 否 |
| **B health ＋ API `demoTrace`** | 環境事實＋這一筆事實 | **採用** |
| C 完整 APM／K8s 觀測 | 過重 | 否 |

---

## 3. 畫面（§1）

### 3.1 元件

- 共用 Vue 元件：`BackendStoryPanel.vue`
- 嵌入：`TradeView.vue`、`PortalView.vue`（左右或上下並排；窄螢幕改堆疊）
- 不新增專用 router（已否決「只靠切分頁」；保留未來可抽成 `/demo-stage` 但不在 v1 範圍）

### 3.2 面板區塊（由上到下建議）

1. **PROCESS FLOW（主視覺）** — 當次動作的步驟列表（三欄模板）
2. **服務儀表板** — Gateway:8080、Order:8081、Risk:8082、Job:8083、Account:8084 綠／紅
3. **部署狀態機（S1–S3）** — 依綠燈推斷；可「釘住敘事」
4. **訂單狀態機** — PENDING → ACCEPTED → EXECUTED／CANCELLED／REJECTED（依專案既有狀態為準）

Login／Audit 頁 v1 **不嵌**（減少噪音）。

---

## 4. `demoTrace` 契約（§2）

### 4.1 形狀（附加於既有 JSON，可忽略）

```json
{
  "demoTrace": {
    "requestId": "uuid",
    "action": "CREATE_ORDER | EXECUTE | CANCEL | LIST",
    "viaGateway": false,
    "inferredStage": "S2",
    "orderId": 6,
    "orderStatus": "ACCEPTED",
    "hops": [
      {
        "service": "order-service",
        "port": 8081,
        "ok": true,
        "detail": "optional short note"
      }
    ],
    "at": "ISO-8601"
  }
}
```

### 4.2 責任

| 元件 | 責任 |
|------|------|
| order-service | 主寫手：create／execute／cancel／分頁 list 回應附帶 `demoTrace` |
| risk Feign | 成功／失敗皆寫 hop（`ok`＋簡短 `detail`） |
| gateway | 轉發時加 `X-Demo-Via-Gateway: 1`；order 據此設 `viaGateway` 並在 hops 前端插入 gateway 步驟 |
| account／job | v1 僅進 health 燈；無同步業務 hop 則不強制 |

### 4.3 明確不做

- Zipkin／OpenTelemetry 全鏈路
- K8s API 探測（S4–S6 用文件＋可手動釘住敘事）
- 改變既有業務欄位語意；缺 `demoTrace` 時面板顯示「尚無 trace／僅 health」

---

## 5. PROCESS FLOW 行為（§3）

### 5.1 步驟三欄模板（強制）

| 欄 | 說明 | 例 |
|----|------|-----|
| 誰 | 服務／層名稱＋埠 | Risk-service :8082 |
| 做什麼 | 一句人話職責 | 名目金額風控檢查 |
| 對應狀態 | 此步相關狀態或轉換 | 通過→可成交；拒絕→維持 ACCEPTED／失敗訊息 |

### 5.2 劇本表（前端）＋ hops（後端）

- 前端維護 **動作劇本表**（CREATE／EXECUTE／CANCEL）：預設步驟順序與「做什麼／狀態」文案。
- 後端 `hops[]` 覆寫該步 `ok`／`detail`，並決定哪些步驟亮起／失敗變紅。
- `viaGateway=true` 時在 Order 前插入 Gateway 步驟（職責：統一入口轉發）。

### 5.3 雙狀態機角色

| 狀態機 | 回答的問題 |
|--------|------------|
| 訂單狀態機 | 這一筆單子到哪 |
| S1–S3 部署階 | 環境開到哪（微服務／Gateway 是否在場） |
| PROCESS FLOW | 這一動的過程步驟與職責（主敘事） |

### 5.4 階段推斷（事實）

| 條件（health） | 推斷 |
|----------------|------|
| 僅 Order（± frontend） | S1 |
| Order + Risk | S2 |
| + Gateway 或 Account（可再細分文案） | S3 敘事 |

S4–S6：面板標為「文件／手動釘住」，不自動宣稱已上 K8s。

### 5.5 刷新

- Health／拓撲：約每 5s 呼叫 `GET /api/demo/topology`（可設定；頁面隱藏可降頻）
- Trace：每次相關 API 成功／失敗回應更新「最新一筆」；LIST 可帶摘要或沿用上次 mutate trace

---

## 6. 實作邊界（給 plan 用）

### 前端

- `components/BackendStoryPanel.vue`（+ 小模組：health probe、剧本 merge、狀態機展示）
- `api/client.js`：保存並暴露最新 `demoTrace`；可選 pinia／provide 共享
- Trade／Portal 布局嵌入面板
- 註解：comment_verbosity detailed（【職責】【技巧】【概念】）

### 後端

- order-service：組裝 `DemoTrace` DTO，掛在既有 response wrapper／DTO
- execute 路徑：Feign risk 前後寫 hop
- gateway：轉發 header `X-Demo-Via-Gateway`
- 既有測試補：回應含 `demoTrace`；risk 失敗 hop `ok:false`

### 文件

- 更新 Startup banner／學習地圖可選連結說明「Trade／Portal 內建後端敘事面板」
- 不強制改 docs catalog 除非實作後補一頁操作說明

### 測試

- 前端：劇本 merge 單元測（純 JS）或元件測（若專案已有）
- 後端：create／execute 契約測含 hops
- 手動 Demo：關 Risk → 成交 → 流程紅在 risk hop＋訂單狀態不變／錯誤可見

---

## 7. 非目標（v1）

- 專用 `/demo-stage` 路由（可後加）
- Audit 頁嵌入
- 真實分散式 tracing 後端
- 自動偵測 kind／kubectl

---

## 8. 驗收 Checklist

- [ ] Trade／Portal 可見 BackendStoryPanel
- [ ] 每步有「誰／做什麼／狀態」
- [ ] health 燈反映 gateway／order／risk／job／account 實際可連（經 `/api/demo/topology`）
- [ ] 下單／成交後 PROCESS FLOW 隨 `demoTrace` 更新
- [ ] 關 Risk 再成交：risk hop 失敗可見
- [ ] 無 `demoTrace` 時不炸頁，降級為僅 health＋提示

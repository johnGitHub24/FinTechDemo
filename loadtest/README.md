# FinTechDemo Locust（壓測／簡報）

最短壓測：login → POST `/api/orders` → GET `/api/orders?page=0&size=10`。

## 前置

1. 啟動 `order-service`（:8081），fullflow 另需 Gateway（:8080）
2. （建議）`docker compose --profile monitoring up -d` → Grafana `:3000`、Prometheus `:9090`
3. Python 3.10+；帳號預設 `trader1` / `password`

## 場景

| Scenario | Host 預設 | 說明 |
|----------|-----------|------|
| `baseline` | `:8081` | 直連 order |
| `fullflow` | `:8080` | 經 Gateway |

## 門檻（Demo）

- 錯誤率 **&lt; 1%**（排除刻意風控拒絕）
- 本機不設死 TPS；報告落 `loadtest/reports/`

## 指令

```powershell
.\scripts\run-loadtest.ps1
.\scripts\run-loadtest.ps1 -Scenario fullflow
.\scripts\run-loadtest.ps1 -WebUi   # http://localhost:8089 — 前端「壓測 UI」可點
```

環境變數（可選）：`FINTECH_USER`、`FINTECH_PASSWORD`、`FINTECH_SCENARIO`。

## 與 Grafana 聯動（簡報）

1. 起 monitoring profile  
2. 開 http://localhost:5173/login → 點 Grafana／Prometheus  
3. 另開 Locust Web UI 或 headless  
4. Overview dashboard 看 UP／RPS／5xx  

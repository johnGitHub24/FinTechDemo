# FinTechDemo Locust（壓測／簡報）

最短壓測：login → POST `/api/orders` → GET `/api/orders?page=0&size=10`。

## 前置

1. 啟動 `order-service`（:8081）；fullflow 另需 Gateway（:8080）
2. （建議）`docker compose --profile monitoring up -d` → Grafana `:3000`、Prometheus `:9090`
3. Python 3.10+ + `pip install locust`；帳號預設 `trader1` / `password`

## 場景

| Scenario | Host 預設 | 說明 |
|----------|-----------|------|
| `baseline` | `:8081` | 直連 order |
| `fullflow` | `:8080` | 經 Gateway |

## 指令（本目錄）

```powershell
cd loadtest
python -m locust -f locustfile.py --host http://localhost:8081 --web-host 127.0.0.1 --web-port 8089
# fullflow:
python -m locust -f locustfile.py --host http://localhost:8080 --web-host 127.0.0.1 --web-port 8089
```

Demo LOOP（`ensure-demo-links.ps1`）也會在需要時起 Locust Web UI `:8089`。

環境變數（可選）：`FINTECH_USER`、`FINTECH_PASSWORD`、`FINTECH_SCENARIO`。

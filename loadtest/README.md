# FinTechDemo Locust（P9）

最短壓測：login → POST `/api/orders` → GET `/api/orders?page=0&size=10`。

## 前置

1. 啟動 `order-service`（:8081），或經 Gateway（:8080）代理 `/api/**`
2. Python 3.10+；帳號預設 `trader1` / `password`

## 安裝與 baseline

```powershell
# 建議從專案根目錄
.\scripts\run-loadtest.ps1

# 或手動
cd loadtest
python -m pip install -r requirements.txt
locust -f locustfile.py --host http://localhost:8081 --headless -u 5 -r 1 -t 30s
```

## Host 參數

| 目標 | Host |
|------|------|
| order-service 直連 | `http://localhost:8081`（預設） |
| Gateway | `http://localhost:8080` |

```powershell
.\scripts\run-loadtest.ps1 -HostUrl http://localhost:8080
```

環境變數（可選）：`FINTECH_USER`、`FINTECH_PASSWORD`。

## Web UI

```powershell
cd loadtest
locust -f locustfile.py --host http://localhost:8081
# 瀏覽器開 http://localhost:8089
```

# FinTechDemo — 測試與 CI

總驗收勾選：[驗收清單.md](驗收清單.md)

## 驗證入口

```powershell
.\scripts\check.ps1                          # unit + integration
.\scripts\verify-pipeline.ps1                # check + compose config + k8s
.\scripts\verify-pipeline.ps1 -Up -Smoke     # 可選：容器 + API 煙霧
cd frontend; npm run build                   # 前端建置
```

## Case ID（Fixture：`docs/test-data/`）

| Case ID | 層 | 說明 |
|---------|----|------|
| AUTH-001 | Int | 登入成功 → token |
| AUTH-002 | Int | 錯誤密碼 → 401 |
| AUTH-003 | Int | 缺欄位 → 400 |
| ORDER-001 | Int | 下單 201 PENDING |
| ORDER-003 | Int | 缺必填 → 400 |
| ORDER-004 | Int | quantity 非法 → 400 |
| ORDER-008 | Int | 列表分頁 meta |
| SEC-001 | Int | 無 Token → 401 |
| RISK-001 | Unit+Int | 風控通過 |
| RISK-002 | Unit+Int | 風控拒絕 |
| ACCOUNT-001 | Int | JWT → 種子帳戶 |
| ACCOUNT-002 | Int | 無 Token → 401 |
| P-BASE-01 | Perf | Locust baseline |

載入器：`com.fintech.demo.support.DemoTestFixtures`（`testFixtures(project(':common'))`）

## 壓測／觀測

```powershell
.\scripts\run-loadtest.ps1 -WebUi            # http://localhost:8089
docker compose --profile monitoring up -d    # Grafana :3000 · Prometheus :9090
```

門檻：錯誤率 &lt; 1%。報告：`loadtest/reports/`。

## 簡報一條龍

1. Risk(:8082) + Order(:8081) + frontend `:5173`  
2. `/login` → `/trade` 成交 → `/blueprint`  
3. `.\scripts\check.ps1` → `.\scripts\verify-pipeline.ps1`  
4. （加分）Eureka 升級口徑見 SPEC §2.3／TradingMicroService  

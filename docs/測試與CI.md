# FinTechDemo — 測試與 CI

## 驗證入口

```powershell
.\scripts\check.ps1
# 或
.\gradlew.bat check
```

= 各模組 unit + integration。

## Fixture

- 路徑：`docs/test-data/{category}/{CASE-ID}.json`
- 類別：`com.fintech.demo.support.DemoTestFixtures`（`testFixtures(project(':common'))`）

## 壓測

```powershell
.\scripts\run-loadtest.ps1
.\scripts\run-loadtest.ps1 -Scenario fullflow
.\scripts\run-loadtest.ps1 -WebUi   # http://localhost:8089
```

門檻：錯誤率 &lt; 1%。報告：`loadtest/reports/`。

## 觀測（Demo）

```powershell
docker compose --profile monitoring up -d
```

- Grafana http://localhost:3000 （admin/admin）· dashboard `FinTechDemo Overview`
- Prometheus http://localhost:9090
- 前端快捷：http://localhost:5173/login

## 簡報一條龍

1. Risk(:8082) + Order(:8081) + `cd frontend && npm run dev`
2. 開 `/login` 看服務燈號與 Demo 快捷
3. `.\scripts\check.ps1`
4. monitoring profile → Grafana；Locust `-WebUi` → 壓測 UI

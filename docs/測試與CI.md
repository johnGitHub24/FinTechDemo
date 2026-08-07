# FinTechDemo — 測試與 CI

## 驗證入口

```powershell
.\scripts\check.ps1                 # unit + integration
.\scripts\verify-pipeline.ps1       # check + compose config + k8s
.\scripts\verify-pipeline.ps1 -Up -Smoke   # 可選：起容器 + API 煙霧
```

前端：

```powershell
cd frontend; npm run build
```

總驗收勾選：[驗收清單.md](驗收清單.md)

= 各模組 unit + integration（`gradlew check`）。

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
# 或
.\scripts\start-monitoring-local.ps1
```

- Grafana http://localhost:3000 （admin/admin）· dashboard `FinTechDemo Overview`
- Prometheus http://localhost:9090
- 前端快捷：http://localhost:5173/login

## 簡報一條龍

1. Risk(:8082) + Order(:8081) + `cd frontend && npm run dev`
2. 開 `/login` 看服務燈號與 Demo 快捷；`/blueprint` 講技術棧
3. `.\scripts\check.ps1` → `.\scripts\verify-pipeline.ps1`
4. monitoring profile → Grafana；Locust `-WebUi` → 壓測 UI
5. （加分口頭）Eureka 升級路徑見 SPEC §2.3／TradingMicroService

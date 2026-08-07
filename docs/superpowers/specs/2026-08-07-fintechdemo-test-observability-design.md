# FinTechDemo — 測試金字塔 × Fixture × 觀測／壓測 Demo 設計

> 狀態：已核准實作（方案 2 精簡版）  
> 日期：2026-08-07  
> 對齊：EngineeringOS `knowledge/testing.md`、TradingCRUD `CrudTestFixtures`、APIGatewayMQ `monitoring/prometheus.yml`

## 目標

簡報可一條龍展示：**單元／整合成對綠燈 → Locust 壓測 → Grafana／Prometheus 關鍵指標**，並在前端（登入頁＋頂部 nav）可點關鍵連結。

## 範圍

1. **Fixture 測試骨架（全服務）**：`docs/test-data/{auth,order,risk,account}/` + `DemoTestFixtures`；各服務 Happy＋錯誤路徑 Case ID。
2. **監控**：`monitoring/prometheus.yml` + Grafana provisioning／單一 Overview dashboard；compose `profiles: [monitoring]`。
3. **壓測（精簡）**：baseline + fullflow（Gateway）；錯誤率 &lt; 1%；報告落 `loadtest/reports/`。
4. **前端**：Login Demo 快捷列 + 登入後 nav（Grafana／Prometheus／Locust）。
5. **文件同步**：測試規格、測試與 CI、testing.md、README、loadtest README、StartupInfoLogger。

## 非目標

多 dashboard、告警規則、JMeter 全套、複雜 PromQL 教學。

## 驗收

- `.\scripts\check.ps1`（或 `gradlew check`）綠  
- `docker compose --profile monitoring up -d` 後 Grafana `:3000`、Prometheus `:9090` 可開  
- `/login` 與登入後 nav 可開觀測／壓測／關鍵 health／Swagger／docs  
- Locust baseline／fullflow 可跑並產報告  

# FinTechDemo — Testing

## 金字塔

| 層 | 指令／位置 | Acceptance |
|----|------------|--------------|
| Unit | `**/src/test` | Service 公開方法 ≥1；Risk 通過／拒絕各 1 |
| Integration | MockMvc + `DemoTestFixtures` | Auth、CRUD、分頁、風控、未授權 401 |
| Performance | `loadtest/` | baseline／fullflow；錯誤率 &lt; 1% |

成對一致性：契約變更 → 單元＋整合案例一起改（`eos-minimal/knowledge/testing.md`）。

## Case ID（摘要）

見 [測試規格書.md](../測試規格書.md)。Fixture：`docs/test-data/...`。

## 觀測

`monitoring/prometheus.yml` + compose profile `monitoring`；前端登入頁／nav 可開 Grafana／Prometheus／Locust。

## 驗證入口

```powershell
.\scripts\check.ps1
```

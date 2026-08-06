# FinTechDemo — Testing

## 金字塔

| 層 | 指令／位置 | Acceptance |
|----|------------|------------|
| Unit | `**/src/test` | Service 公開方法 ≥1；Risk 通過／拒絕各 1 |
| Integration | MockMvc 或 `@SpringBootTest` | Auth、CRUD、分頁、風控拒絕、未授權 401 |
| Performance | `loadtest/` | baseline 可跑；p95／錯誤率門檻寫於下方（可依機器調整） |

成對一致性：契約變更 → 單元＋整合案例一起改（`eos-minimal/knowledge/testing.md`）。  
感測：`EngineeringOS/eos-minimal/hooks/scan-paired-tests.ps1 -ProjectRoot .`

## 建議案例 ID

| ID | 層 | 情境 |
|----|----|------|
| U-ORD-01 | Unit | createOrder 合法 → ACCEPTED／PENDING |
| U-RSK-01 | Unit | 超限額 → reject |
| I-AUTH-01 | Int | login 成功取得 token |
| I-ORD-01 | Int | Bearer 下單 201 |
| I-ORD-02 | Int | 無 token 401 |
| I-ORD-03 | Int | 分頁 page=0 size=10 結構正確 |
| I-RSK-01 | Int | 超限額 422／REJECTED |
| P-BASE-01 | Perf | Locust 下單+列表 30s |

## Performance 門檻（初版，可調）

- 錯誤率 &lt; 1%（排除刻意風控拒絕情境）  
- 本機 baseline 不設死 TPS；Demo 展示「有腳本、有報告」即可  

## 驗證入口

```powershell
.\scripts\check.ps1
```

對齊各 Trading* 專案：`gradlew check` = unit + integration。

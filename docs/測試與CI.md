# FinTechDemo — 測試與 CI

總驗收勾選：[驗收清單.md](驗收清單.md)

## 驗證入口

```powershell
.\scripts\check.ps1                          # Pure：JDK 21 → gradlew check（unit + integration）
.\gradlew.bat :order-service:bootRun         # Pure 本機 Demo（:8081）
.\開啟Demo.cmd                               # 可選全棧 Demo
.\demo\verify-pipeline.ps1                   # check + compose + k8s
.\demo\verify-pipeline.ps1 -Up -Smoke        # 可選：容器 + health
```

IntelliJ：Open 專案根 → Gradle 窗 **`:order-service:bootRun`**（勿對 `*Application` 綠箭頭）。

## Case ID（Fixture：`docs/test-data/`）

成對掃描：`EngineeringOS/eos-minimal/hooks/scan-paired-tests.ps1`。  
HTTP API Case 必須 Unit + Integration 同一 ID。

| Case ID | 層 | 說明 |
|---------|----|------|
| AUTH-001 | Unit+Int | 登入成功 → token |
| AUTH-002 | Unit+Int | 錯誤密碼 → 401 |
| AUTH-003 | Unit+Int | 缺欄位 → 400 |
| ORDER-001 | Unit+Int | 下單 PENDING／DTO 合法 |
| ORDER-002 | Unit+Int | 重複 clientOrderId → 422 |
| ORDER-003 | Unit+Int | 缺必填 → 400 |
| ORDER-004 | Unit+Int | quantity 非法 → 400 |
| ORDER-005 | Unit+Int | 風控通過 → ACCEPTED |
| ORDER-006 | Unit+Int | 風控拒絕 → REJECTED |
| ORDER-007 | Unit+Int | 取消 PENDING → CANCELLED |
| ORDER-008 | Unit+Int | 列表分頁 meta |
| SEC-001 | Unit+Int | 無 Token → 401 |
| JWT-001 | Unit+Int | 有效 Token 可通過 |
| JWT-002 | Unit+Int | 無效 Token → 401 |
| RISK-001 | Unit+Int | 風控通過 |
| RISK-002 | Unit+Int | 風控拒絕（現金不足） |
| RISK-003 | Unit+Int | 名義金額超上限 |
| ACCOUNT-001 | Unit+Int | JWT → 種子帳戶 |
| ACCOUNT-002 | Unit+Int | 無 Token → 401 |
| LEDGER-001 | Unit+Int | BUY 入帳扣現金 |
| LEDGER-002 | Unit+Int | SELL 入帳加現金 |
| LEDGER-003 | Unit+Int | 現金不足 → 422 |
| LEDGER-004 | Unit+Int | 持倉列表 |
| GW-004 | Unit+Int | Gateway 未超限放行 |
| GW-005 | Unit+Int | Gateway 超限 429 |
| STALE-001 | Unit+Int | 逾時 PENDING 取消 |
| FLOW-001～007 | Unit+Int | 種子／RBAC／入口／成交／取消；007＝ADMIN 全站列表含 username |
| ORDER-BOOT-001 | Unit only | Order contextLoads |
| ACCOUNT-BOOT-001 | Unit only | Account contextLoads |
| RISK-BOOT-001 | Unit only | Risk contextLoads |
| GATEWAY-BOOT-001 | Unit only | Gateway contextLoads |
| JOB-BOOT-001 | Unit only | Job contextLoads |
| P-BASE-01 | Perf | Locust baseline |

Boot／面試煙測（`*-BOOT-001`）只斷言 ApplicationContext 載入，標 **unit-only**，不強制 HTTP 成對。

Account 測試關 Redis／H2 TCP：`account-service/src/test/resources/application.properties`（`fintech.redis.enabled=false`、`spring.h2.tcp.enabled=false`）。本機 Demo 預設開 Redis（TTL 600s），與測試設定不同。

掃描 hook 可能把 `ORDER-001`／`RISK-001`／`SEC-001`／`JWT-001` 的子字串（如 `RDER-001`、`ISK-001`、`EC-001`、`WT-001`）當成獨立 Case；**不是程式錯字**，勿為此新增假 Case。

載入器：`com.fintech.demo.support.DemoTestFixtures`（`testFixtures(project(':common'))`）

## 壓測／觀測

```powershell
cd loadtest
python -m locust -f locustfile.py --host http://localhost:8081 --web-port 8089
docker compose --profile monitoring up -d    # Grafana :3000 · Prometheus :9090
```

門檻：錯誤率 &lt; 1%。報告：`loadtest/reports/`。

## HTML 報表入口

| 類型 | 路徑 |
|------|------|
| Javadoc | `docs/javadoc/index.html`（`.\gradlew.bat aggregateJavadoc`） |
| 單元／整合測試導覽 | [docs/portals/test-reports.html](portals/test-reports.html) |
| 各模組 Gradle 報表 | `{module}/build/reports/tests/test/index.html`（先 `.\scripts\check.ps1`） |

Demo 快捷「學習文件」已含 **Javadoc／單元測試／整合測試** 連結（需 `serve-docs` :5500）。

## 簡報一條龍

1. Risk(:8082) + Order(:8081) + frontend `:5173`  
2. `/login` → `/trade` 成交 → `/blueprint`  
3. `.\scripts\check.ps1` → `.\demo\verify-pipeline.ps1`  
4. 閘門敘事見 [工程規範](guides/engineering-norms.html)（hooks／pipeline／doctor）  
5. （加分）Eureka 升級口徑見 SPEC §2.3／TradingMicroService  

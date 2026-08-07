# FinTechDemo

可演示的分散式交易 Demo（打通可跑，不堆複雜度）。

## 10 分鐘進入展示狀態

**成交會 Feign 呼叫 risk-service（:8082），最短可演示請開兩個後端：**

| 角色 | IntelliJ Run | 埠 |
|------|--------------|----|
| ★ 主入口 | `OrderServiceApplication` | 8081 |
| ★ 風控（點「成交」必開） | `RiskServiceApplication` | 8082 |

```powershell
# 一鍵最短後端（背景起 Risk + Order）
.\scripts\start-demo.ps1 -StartMinimal

# 或手動三終端
.\gradlew.bat :risk-service:bootRun
.\gradlew.bat :order-service:bootRun
cd frontend; npm install; npm run dev

# 產出 JavaDoc HTML
.\gradlew.bat aggregateJavadoc
# 開啟 docs\javadoc\index.html
```

- 前端：http://localhost:5173/login（**Demo 快捷入口**＝Console 橫幅可點版；含服務燈號）  
- 系統運作藍圖：http://localhost:5173/blueprint（可不登入；圖文版見 [docs/architecture/系統運作藍圖.md](docs/architecture/系統運作藍圖.md)）  
- 帳號：`trader1` / `password`（ADMIN：`admin` / `password`）  
- Swagger：http://localhost:8081/swagger-ui.html  
- Risk health：http://localhost:8082/actuator/health  
- 觀測：`docker compose --profile monitoring up -d` → Grafana http://localhost:3000 · Prometheus http://localhost:9090  
- 壓測：`.\scripts\run-loadtest.ps1 -WebUi` → http://localhost:8089  
- JavaDoc：`docs/javadoc/index.html`（需先跑 `aggregateJavadoc`）  
- 成交必開 Risk(:8082)；只開 Order 會 Feign Connection refused。  
- 測試：`.\scripts\check.ps1` · Fixture：`docs/test-data/` · 見 [docs/測試與CI.md](docs/測試與CI.md)

## 學習入口（HTML）

| 文件 | 用途 |
|------|------|
| [docs/index.html](docs/index.html) | **統一學習入口（書櫃）** |
| [docs/portals/demo-flow.html](docs/portals/demo-flow.html) | 啟動與 Demo 運作流程 |
| [docs/javadoc/index.html](docs/javadoc/index.html) | 聚合 JavaDoc（`.\gradlew.bat aggregateJavadoc`） |
| [docs/portals/handbook.html](docs/portals/handbook.html) | 完整學習手冊 |
| [docs/portals/swagger.html](docs/portals/swagger.html) | API（Swagger UI + openapi.yaml） |
| [docs/architecture/api-spec.html](docs/architecture/api-spec.html) | API 表格 |
| [docs/portals/codeGraphic.html](docs/portals/codeGraphic.html) | 架構圖 |
| [docs/architecture/系統運作藍圖.md](docs/architecture/系統運作藍圖.md) | **系統運作藍圖（圖文／GitHub·Redmine）** |
| [docs/architecture/architecture.html](docs/architecture/architecture.html) | 模組化／分散式 |

## Pipeline

```powershell
.\scripts\verify-pipeline.ps1        # 測試 + compose + k8s
.\scripts\verify-pipeline.ps1 -Up    # 再 docker 起核心容器（可選）
```

## 現場 Demo 六步

1. 登入 trader1  
2. `/trade` 下單  
3. execute（Swagger 或 UI）  
4. `/portal` 看餘額／持倉／歷史  
5. 換 admin 看 `/portal/audit`  
6. （加分）講 Gateway／3 MS／Kafka／Redis；Eureka＝可升級項  

權威規格：[FinTechDemo-SPEC.md](FinTechDemo-SPEC.md)（瀏覽請開 [docs/guides/spec.html](docs/guides/spec.html)）

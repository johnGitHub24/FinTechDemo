# FinTechDemo

可演示的分散式交易 Demo（打通可跑、可部署、文件可傳承）。

## 傳承入口（精簡）

| 文件 | 用途 |
|------|------|
| [FinTechDemo-SPEC.md](FinTechDemo-SPEC.md) | **權威規格**（含固定 URL Feign／Eureka 升級口徑） |
| [docs/驗收清單.md](docs/驗收清單.md) | **總驗收勾選** |
| [docs/測試與CI.md](docs/測試與CI.md) | check／pipeline／壓測／觀測／Case ID |
| [docs/architecture/系統運作藍圖.md](docs/architecture/系統運作藍圖.md) | 技術棧＋Mermaid 圖文 |
| [docs/index.html](docs/index.html) | 學習書櫃（HTML） |
| [docs/portals/loop-guide.html](docs/portals/loop-guide.html) | 部署跑通 · Loop Engineering |

瀏覽規格 HTML：[docs/guides/spec.html](docs/guides/spec.html) · 產生：`.\scripts\generate-docs-html.ps1`

## 10 分鐘進入展示狀態

**最短可成交請開兩個後端（成交會 Feign 呼叫 Risk）：**

| 角色 | IntelliJ Run | 埠 |
|------|--------------|----|
| ★ 主入口 | `OrderServiceApplication` | 8081 |
| ★ 風控 | `RiskServiceApplication` | 8082 |

```powershell
.\scripts\start-demo.ps1 -StartMinimal
cd frontend; npm install; npm run dev
```

- 前端：http://localhost:5173/login · 藍圖：http://localhost:5173/blueprint  
- 帳號：`trader1` / `password`（ADMIN：`admin` / `password`）  
- Swagger：http://localhost:8081/swagger-ui.html  

## 驗證／部署

```powershell
.\scripts\check.ps1                    # unit + integration
.\scripts\verify-pipeline.ps1          # check + compose config + k8s
.\scripts\verify-pipeline.ps1 -Up      # 再 docker 起核心容器
.\scripts\verify-pipeline.ps1 -Up -Smoke   # + API 煙霧
```

觀測：`docker compose --profile monitoring up -d` → Grafana :3000 · Prometheus :9090  
壓測：`.\scripts\run-loadtest.ps1 -WebUi` → :8089

## 現場 Demo 六步

1. 登入 trader1  
2. `/trade` 下單  
3. 成交（需 Risk :8082）  
4. `/portal` 看餘額／持倉／歷史  
5. admin → `/portal/audit`  
6. （加分）Gateway／Kafka／Redis；Eureka＝升級項（SPEC §2.3）

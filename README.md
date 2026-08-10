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

**推薦：只跑主入口，其餘由 Loop 自動補齊。**

| 步驟 | 做什麼 | 說明 |
|------|--------|------|
| 1 | IntelliJ 跑 `OrderServiceApplication` :8081 | 主入口 |
| 2 | 等 Console 出現 `【LOOP】…背景進行中` | `DemoStackBootstrap` → `ensure-demo-links.ps1` |
| 3 | 等 1～3 分鐘 | 依序補：Risk → Account → Gateway → Job → Vite → Docs（已 UP 則 KEEP） |
| 4 | 開 http://localhost:5173/login | `trader1` / `password` |

手動一鍵（等同 Loop）：雙擊 `開啟Demo.cmd` 或 `.\scripts\doctor-demo.ps1 -Fix`  

優先順序詳見：[docs/portals/demo-flow.html §2](docs/portals/demo-flow.html#s2) · Entry Point：[boot-entrypoint.html](docs/portals/boot-entrypoint.html)

- 前端：http://localhost:5173/login · 藍圖：http://localhost:5173/blueprint  
- Swagger：http://localhost:8081/swagger-ui.html  
- 學習文件：http://127.0.0.1:5500/docs/index.html（LOOP 會起 docs）

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

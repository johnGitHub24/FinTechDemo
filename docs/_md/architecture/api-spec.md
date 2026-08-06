# FinTechDemo — API 規格書

> 互動版：[swagger.html](swagger.html) · Runtime：`http://localhost:8081/swagger-ui.html`

## 認證

| Method | Path | 說明 |
|--------|------|------|
| POST | `/api/auth/login` | `{username,password}` → `{token,username,roles}` |

之後請求加 Header：`Authorization: Bearer <token>`。

示範帳：`trader1`／`admin`，密碼 `password`。

## 前台（交易）

| Method | Path | 角色 | 說明 |
|--------|------|------|------|
| GET | `/api/market/symbols` | USER/ADMIN | 參考價 |
| POST | `/api/orders` | USER/ADMIN | 下單 → PENDING |
| GET | `/api/orders` | USER/ADMIN | 歷史分頁 `page`/`size`/`status` |
| GET | `/api/orders/{id}` | 自己的 | 查單 |
| POST | `/api/orders/{id}/execute` | 自己的 | 風控後 ACCEPTED／REJECTED |
| DELETE | `/api/orders/{id}` | 自己的 | 取消 PENDING |

## 後台（會員）

| Method | Path | 角色 | 說明 |
|--------|------|------|------|
| GET | `/api/accounts/me` | USER/ADMIN | 餘額（正式由 account:8084／Gateway） |
| GET | `/api/positions` | USER/ADMIN | 持倉 |
| GET | `/api/audit-logs` | **ADMIN** | 審計摘要 |

## 內部（系統）

| Method | Path | 說明 |
|--------|------|------|
| POST | `/api/risk/check` | risk:8082，Feign |
| POST | `/api/internal/accounts/{userId}/apply-trade` | account 入帳，`X-Internal-Token` |
| POST | `/api/internal/jobs/cancel-stale` | Job 觸發，`X-Job-Token` |

## 錯誤碼

| HTTP | 情境 |
|------|------|
| 401 | 未登入／Token 無效 |
| 403 | 角色不符（如 USER 打 audit） |
| 404 | 資源不存在或不屬於你 |
| 422 | 業務規則（重複 clientOrderId 等） |

## 埠

| 服務 | Port |
|------|------|
| Gateway | 8080 |
| order | 8081 |
| risk | 8082 |
| job | 8083 |
| account | 8084 |
| Vue | 5173 |

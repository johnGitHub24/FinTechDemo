# frontend — Vue 3 + Router + JWT（P3）

## 啟動

先起後端：

```powershell
cd D:\ClaudeCode\FinTechDemo
.\gradlew.bat :order-service:bootRun
```

再開前端：

```powershell
cd frontend
npm install
npm run dev
```

→ http://localhost:5173/login  
帳號：`trader1` / `admin`，密碼：`password`

## 頁面

| 路由 | 說明 |
|------|------|
| `/login` | JWT 登入 |
| `/trade` | 前台下單／成交／取消 |
| `/portal` | 後台餘額／持倉／歷史分頁 |
| `/portal/audit` | ADMIN 審計 |

Proxy：`/api` → `http://localhost:8081`（P4 改 Gateway :8080）

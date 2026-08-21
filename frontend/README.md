# frontend — Vue 3 + Router + JWT（P3）

## 啟動

先起後端：

```powershell
cd D:\SouceDemo\RemoteSpringBoot\FinTechDemo
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
Demo：`trader1` 對帳（85000＋AAPL）；`admin` 看全站訂單，餘額／持倉仍是本人（100000、無持倉）。

## 頁面

| 路由 | 說明 |
|------|------|
| `/login` | JWT 登入 |
| `/trade` | 前台下單／成交／取消 |
| `/portal` | 後台餘額／持倉／歷史分頁 |
| `/portal/audit` | ADMIN 審計 |

Proxy：`開啟Demo.cmd` 在 Gateway UP 後會重啟 Vite，`/api` → `:8080`；純手動 `npm run dev` 仍預設 → `:8081`。  
要手動走 Gateway：`$env:VITE_API_TARGET='http://localhost:8080'; npm run dev`

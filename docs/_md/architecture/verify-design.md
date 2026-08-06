# FinTechDemo — 驗證設計（JWT／RBAC）

> 對齊 EngineeringOS `knowledge/validation-design.md`。權威行為以 [FinTechDemo-SPEC](../guides/spec.html) 為準。

## 驗證層次

| 層 | 做法 |
|----|------|
| 前端 UX | Vue Router `requiresAuth`；無 JWT → `/login` |
| HTTP | Axios Bearer；401（非 login）→ 清 session |
| API 校驗 | `@Valid` DTO（數量＞0、價格＞0 等） |
| 安全 | Spring Security STATELESS + JWT Filter；角色 USER／ADMIN |
| 業務 | 資源歸屬（USER 只能碰自己的單／帳戶）；餘額／持倉不足 → 拒絕 |
| 風控服務 | 名義金額 ≤ 限額（risk-service） |

## RBAC 摘要

| 端點類 | USER | ADMIN |
|--------|------|-------|
| 下單／進行中／取消自己的／自己的餘額持倉歷史 | ✓ | ✓ |
| 全站訂單歷史 | ✗ | ✓ |
| 審計列表 `/api/audit-logs` | ✗ | ✓ |
| 強制取消他人單 | ✗ | ✓ |
| 無 Bearer | 401 | 401 |

功能範圍精簡版見 SPEC **§1.5**（要做 F0–F4／B1–B5；不做行情流／改單／部分成交等）。

## 密鑰

- `app.jwt.secret`／`JWT_SECRET` 環境變數；禁止提交真實密鑰（EOS 安全紅線）

## 與 TradingCRUD／SpringSecurity 對照

- 登入簽發、FilterChain、router `unref(isLoggedIn)`、401 排除 login — **採納**  
- 前端守衛不可替代後端授權 — **必須兩邊都做**

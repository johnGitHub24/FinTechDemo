# FinTechDemo — 產品劇情與 RBAC

> 權威：[FinTechDemo-SPEC](spec.html)（含 **§1.5 精簡功能範圍**）

---

## 1. 三層規劃

```mermaid
flowchart TB
  subgraph PRODUCTF"產品：前後台精簡配合"]
    LF登入 JWT+RBAC]
    FOF前台 下單/進行中/取消]
    BOF後台 餘額/持倉/歷史/審計]
    L --> FO
    L --> BO
    FO --> DBF(同一 orders/accounts)]
    BO --> DB
  end
  EOSFEngineeringOS] --> PRODUCT
  PRODUCT --> TECHFGateway/MS/Kafka…]
```

Trading*＝運用模組；EOS＝怎麼建；本倉＝精簡完整的產品劇情。

---

## 2. 功能配合一覽（要做）

| 區 | ID | 功能 |
|----|-----|------|
| 前台 | F0–F4 | 登入、下單、進行中列表、取消、參考價 |
| 後台 | B1–B5 | 餘額、持倉、歷史分頁、ADMIN 審計、ADMIN 全站 |
| 配合 | — | 同身分、同授權、狀態／帳務一致 |

**不做**：即時行情、改單、部分成交、出入金、KYC…（見 SPEC §1.5）

---

## 3. 權限矩陣

| 能力 | 未登入 | USER | ADMIN |
|------|--------|------|-------|
| `/login` | ✓ | ✓ | ✓ |
| 前台下單／取消自己的／進行中 | ✗ | ✓ | ✓ |
| 後台自己的餘額／持倉／歷史 | ✗ | ✓ | ✓ |
| 後台全站歷史 | ✗ | ✗ | ✓ |
| 後台審計 | ✗ | ✗ | ✓ |
| API 無 Bearer | 401 | 401 | 401 |

---

## 4. Demo 台詞（約 2 分鐘）

1. 登入 USER → 前台下單 → 進行中看得到  
2. 切後台 → 歷史有這筆、餘額／持倉變了  
3. 取消一筆 PENDING → 歷史變 CANCELLED  
4. 未登入進不了後台；拔 Token → 401  
5. ADMIN 另可看審計／全站（可選演示）  
6. 其餘 Gateway／Kafka…是掛上的運用模組  

---

## 5. Phase

| 里程碑 | Phase |
|--------|-------|
| API：下單＋歷史＋餘額＋持倉 | P1 |
| RBAC | P2 |
| Vue 走完 F0–F4＋B1–B3（B4/B5 可同 Phase） | P3 |
| Gateway 起 | P4+ |

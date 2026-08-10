# FinTechDemo — 分散式系統落地（微服務 × Kafka × Redis）

> 展示主敘事：**Distributed System Demo**（不是單機 CRUD）。

## 三個業務微服務（至少）

| 服務 | 埠 | 職責 |
|------|-----|------|
| `order-service` | 8081 | 登入 JWT、訂單、審計、發 Kafka；**WebConfig CORS** |
| `risk-service` | 8082 | 名義金額風控（Feign 被叫） |
| `account-service` | 8084 | 餘額／持倉帳本、**Redis cache**、消費 trade-events |
| `job-service` | 8083 | 排程輔助（非業務主鏈） |
| `gateway` | 8080 | 依路徑路由到 order／account；**RateLimitWebFilter** |

## Kafka Event Bus

```text
POST /api/orders
   └─ Producer → topic order-events
        └─ order Consumer → Feign risk → 改訂單 ACCEPTED
             └─ Producer → topic trade-events
                  └─ account Consumer → 入帳 + 清 Redis cache
```

| Topic | Producer | Consumer | 之後做什麼 |
|-------|----------|----------|------------|
| `order-events` | order | order | 呼叫 **risk-service**，執行帳務編排 |
| `trade-events` | order | **account** | 更新現金／持倉，**evict Redis** |

- 預設 `fintech.kafka.enabled=false`（單元／整合測試與 standalone 不依賴 broker）
- Demo：`docker compose up -d` + `--spring.profiles.active=demo`（order **與** account 都開 demo）

## Redis Cache

| 項目 | 說明 |
|------|------|
| 落點 | **account-service** |
| Key | `account:{userId}`、`positions:{userId}` |
| TTL | 60s |
| 失效 | 入帳（Kafka／Feign apply-trade）後 `evict` |
| 降級 | 預設 `fintech.redis.enabled=false` + exclude RedisAutoConfiguration（無 Redis 可啟動） |
| Demo | `application-demo.yml` 開啟 Redis |

## 雙路徑一致性

| 模式 | 行為 |
|------|------|
| Kafka Demo | trade-events → account 最終一致（**不要**再開 feign-sync，避免雙重入帳） |
| Feign sync | `fintech.account.feign-sync=true` 且 kafka=false → order 同步打 account 內部 API |
| Standalone | 只起 order：本機 H2 帳本仍可用；portal 直打 :8081 |

## Gateway 路由

- `/api/accounts/**`、`/api/positions` → account:8084
- 其餘 `/api/**` → order:8081
- **限流**：`RateLimitWebFilter`（`fintech.gateway.rate-limit.*`）；超限 429

前端 Vite：standalone 預設 `:8081`；分散式設 `VITE_API_TARGET=http://localhost:8080`。  
Order 另有 `WebConfig` CORS（允許 `:5173` 直連，不必只靠 proxy）。

## 驗證

```powershell
.\scripts\check.ps1
.\scripts\check-k8s.ps1          # 應含 account-service（只 kustomize，不連 API）
.\scripts\smoke-distributed.ps1  # compose +（服務就緒時）API 煙霧
```

K8s／kind／Docker 跑通規則與故障排除（含 `connection refused` 解法）：  
[`K8s跑通與驗證技巧`](../deploy/k8s-tips.html)

逐層打通到可部署／上線（S0→S6）：  
[`上線部署階段層次（MD）`](../deploy/stages-doc.html) · [`上線部署階段層次（互動）`](../portals/stages.html)

完整架構圖：[`architecture`](architecture.html)

# FinTechDemo Implementation Plan

> **For agentic workers:** 依 Phase 執行；每步可驗證。權威：[FinTechDemo-SPEC.md](../../FinTechDemo-SPEC.md)  
> **工程**：EngineeringOS eos-minimal @ 0.1.9 · `HOW_TO_APPLY.md`

**Goal:** 精簡完整的前後台交易 Demo（JWT／RBAC）＋依序導入 Gateway／MS／Kafka…；先劇情後技術。

**Architecture（摘要）：** Vue `/trade`+`/portal` → Gateway:8080 → order:8081／risk:8082／job:8083；demo 路徑 Kafka；EOS 分層。

**Tech stack：** Java 21 · Spring Boot 3.2 · Gateway · Feign · JPA · H2/PG · Redis · Kafka · Vue3 · Locust · K8s overlay

---

## File structure（P0 鎖定）

```
FinTechDemo/
  settings.gradle, build.gradle, gradlew*
  common/          # DTO、錯誤碼、Topic 常數
  order-service/   # Auth、Order、Account、Position（P1+）
  risk-service/    # 一條風控（P5）
  job-service/     # Scheduled（P7）
  gateway/         # Spring Cloud Gateway（P4）
  frontend/        # Vue（P3）
  scripts/check.ps1
  loadtest/        # P9
  deploy/          # P10
```

---

## Phase 對照

| Phase | 內容 | 驗證 |
|-------|------|------|
| **P0** | 多模組骨架 + check + 健康端點佔位 | `gradlew check` 可跑 |
| P1 | Order/Account/Position CRUD＋分頁 | API curl |
| P2 | JWT＋RBAC | 401／403 |
| P3 | Vue 前後台 F0–F4／B1–B5 | 瀏覽器走完劇本 |
| P4–P10 | Gateway→MS→Kafka→Job/AOP→Prom→Locust→K8s | 依 SPEC |

---

### Task P0.1 — Gradle 多模組根專案

**Files:** `settings.gradle`, `build.gradle`, `gradle.properties`, `.gitignore`, 複製 `gradlew*` 自 TradingSpringCloud

**Step:** include common／order-service／risk-service／job-service／gateway；Java 21；Spring Boot 3.2.2 + Cloud 2023.0.0 BOM

### Task P0.2 — common 模組

**Files:** `common/build.gradle`；placeholder `package-info` 或 `ApiConstants`

### Task P0.3 — 四個可啟動服務骨架

各模組：`*Application.java`、`application.yml`（對應埠）、`build.gradle`（boot）、`GET /actuator/health` 可依賴 actuator

### Task P0.4 — scripts/check.ps1

呼叫 `gradlew check`；各模組至少 1 個 smoke test（context load 或 health）

### Task P0.5 — frontend 佔位＋README 更新狀態

`frontend/package.json` 極簡或 README 註明 P3 再填；更新 SPEC 狀態勾選 P0

**P0 Done when:** `.\scripts\check.ps1` 綠燈；四服務 `bootRun` 可起（可先不起齊）。

---

執行順序：完成本 plan 之 P0 → 再開 P1 plan／任務（TDD：先測後實作）。

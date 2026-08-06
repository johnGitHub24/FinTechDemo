# Backend Story Panel（PROCESS FLOW）Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 Trade／Portal 同屏顯示 PROCESS FLOW 儀表板（誰／做什麼／狀態轉換）＋服務燈＋S1–S3／訂單狀態機，由 health 事實與 API `demoTrace` 驅動。

**Architecture:** order-service 在 `OrderResponse` 附加可選 `demoTrace`；另提供 `GET /api/demo/topology` 由伺服器探測各服務 health（避開瀏覽器 CORS）。前端共用 `BackendStoryPanel`，以劇本表合併 hops 渲染流程。Gateway 轉發時加 `X-Demo-Via-Gateway: 1`。

**Tech Stack:** Spring Boot 3／Java 21／JUnit 5（order＋gateway）、Vue 3／Vite、既有 axios `frontend/src/api/client.js`

**Spec:** `docs/superpowers/specs/2026-08-06-backend-story-panel-design.md`

**Domain note（對齊現況）:** 訂單狀態為 `PENDING → ACCEPTED | REJECTED | CANCELLED`（**無** `EXECUTED`）。面板訂單狀態機必須用此四態。

---

## File map

| File | Responsibility |
|------|----------------|
| `common/.../dto/DemoTrace.java`（或放 order dto） | `demoTrace` record／DTO |
| `order-service/.../dto/DemoHop.java` | hop 結構 |
| `order-service/.../dto/OrderResponse.java` | 加可選 `demoTrace` |
| `order-service/.../demo/DemoTraceFactory.java` | 組裝 create／execute／cancel trace |
| `order-service/.../demo/TopologyController.java` + service | `GET /api/demo/topology` |
| `order-service/.../application/TradingService.java` | create／execute／cancel 填 trace |
| `order-service/.../config/SecurityConfig.java` | 放行或需 JWT 的 `/api/demo/topology`（與其他 `/api/**` 一致需 JWT） |
| `gateway/.../config/DemoGatewayHeaderFilter.java`（或併入 RouteConfig） | 加 `X-Demo-Via-Gateway` |
| `frontend/src/demo/processScripts.js` | CREATE／EXECUTE／CANCEL 劇本 |
| `frontend/src/demo/mergeTrace.js` | 劇本＋hops→步驟列 |
| `frontend/src/demo/inferStage.js` | topology→S1–S3 |
| `frontend/src/stores/demoStory.js` | 最新 trace＋topology＋pinStage |
| `frontend/src/components/BackendStoryPanel.vue` | UI |
| `frontend/src/api/client.js` | 擷取 demoTrace；`fetchTopology` |
| `frontend/src/views/TradeView.vue` | 嵌入面板；寫入 store |
| `frontend/src/views/PortalView.vue` | 同上 |
| `frontend/src/styles.css` | 面板樣式（克制、非紫白 AI 套路） |
| Tests | `DemoTraceFactoryTest`、`TradingServiceTest` 斷言、`TradingFlowIntegrationTest` jsonPath、`mergeTrace` 若可 node 測則測 |

**Spec deviation（必要）:** 規格寫「前端 ping :8080–8084」→ 改 **後端 topology 探測**（同 origin `/api`），避免 CORS；語意仍是「事實燈」。

---

### Task 1: DemoTrace DTO ＋ OrderResponse 欄位

**Files:**
- Create: `order-service/src/main/java/com/fintech/demo/order/dto/DemoHop.java`
- Create: `order-service/src/main/java/com/fintech/demo/order/dto/DemoTrace.java`
- Modify: `order-service/src/main/java/com/fintech/demo/order/dto/OrderResponse.java`
- Create: `order-service/src/test/java/com/fintech/demo/order/dto/DemoTraceSerializationTest.java`（可選 Jackson round-trip；或併入 Task 2）

- [ ] **Step 1: 新增 DTO**

```java
// DemoHop.java
package com.fintech.demo.order.dto;

/**
 * 【職責】demoTrace 單一 hop。
 * 【技巧】record 不可變；ok=false 時 detail 寫失敗摘要。
 * 【概念】展演用輕量 trace，非 OpenTelemetry span。
 */
public record DemoHop(String service, Integer port, boolean ok, String detail) {
    public static DemoHop of(String service, Integer port, boolean ok, String detail) {
        return new DemoHop(service, port, ok, detail);
    }
}
```

```java
// DemoTrace.java
package com.fintech.demo.order.dto;

import java.time.Instant;
import java.util.List;

/**
 * 【職責】附加於 OrderResponse 的 Demo 過程追蹤。
 * 【技巧】record；list hops 用 List.copyOf 於工廠組裝。
 * 【概念】舊客戶端可忽略此欄位。
 */
public record DemoTrace(
        String requestId,
        String action,
        boolean viaGateway,
        String inferredStage,
        Long orderId,
        String orderStatus,
        List<DemoHop> hops,
        Instant at
) {}
```

- [ ] **Step 2: OrderResponse 加欄位**

在 `OrderResponse` 增加：

```java
private DemoTrace demoTrace;
```

（Lombok `@Data` 已涵蓋 getter／setter。）

- [ ] **Step 3: Commit**

```bash
git add order-service/src/main/java/com/fintech/demo/order/dto/DemoHop.java \
  order-service/src/main/java/com/fintech/demo/order/dto/DemoTrace.java \
  order-service/src/main/java/com/fintech/demo/order/dto/OrderResponse.java
git commit -m "feat(order): add optional demoTrace DTO on OrderResponse"
```

---

### Task 2: DemoTraceFactory ＋ TradingService 填入（TDD）

**Files:**
- Create: `order-service/src/main/java/com/fintech/demo/order/demo/DemoTraceFactory.java`
- Create: `order-service/src/test/java/com/fintech/demo/order/demo/DemoTraceFactoryTest.java`
- Modify: `order-service/src/main/java/com/fintech/demo/order/application/TradingService.java`
- Modify: `order-service/src/test/java/com/fintech/demo/order/application/TradingServiceTest.java`

- [ ] **Step 1: 寫失敗測試（Factory）**

```java
@Test
void executeTrace_whenRiskOk_shouldIncludeOrderAndRiskHops() {
    DemoTrace t = DemoTraceFactory.forExecute(
            /*viaGateway*/ false,
            /*orderId*/ 5L,
            /*status*/ "ACCEPTED",
            /*riskOk*/ true,
            /*riskDetail*/ "notional within limit");
    assertThat(t.action()).isEqualTo("EXECUTE");
    assertThat(t.hops()).extracting(DemoHop::service)
            .containsExactly("order-service", "risk-service");
    assertThat(t.hops().get(1).ok()).isTrue();
}

@Test
void executeTrace_whenViaGateway_shouldPrefixGatewayHop() {
    DemoTrace t = DemoTraceFactory.forExecute(true, 5L, "ACCEPTED", true, "ok");
    assertThat(t.viaGateway()).isTrue();
    assertThat(t.hops().get(0).service()).isEqualTo("gateway");
}
```

- [ ] **Step 2: 跑測確認紅燈**

Run: `.\gradlew.bat :order-service:test --tests com.fintech.demo.order.demo.DemoTraceFactoryTest`  
Expected: FAIL（class missing）

- [ ] **Step 3: 實作 Factory（最小）**

```java
public final class DemoTraceFactory {
    private DemoTraceFactory() {}

    public static DemoTrace forCreate(boolean viaGateway, Long orderId, String status) {
        List<DemoHop> hops = new ArrayList<>();
        if (viaGateway) hops.add(DemoHop.of("gateway", 8080, true, "route /api/** → order"));
        hops.add(DemoHop.of("order-service", 8081, true, "persist PENDING order"));
        return new DemoTrace(UUID.randomUUID().toString(), "CREATE_ORDER", viaGateway,
                null, orderId, status, List.copyOf(hops), Instant.now());
    }

    public static DemoTrace forExecute(boolean viaGateway, Long orderId, String status,
                                       boolean riskOk, String riskDetail) {
        List<DemoHop> hops = new ArrayList<>();
        if (viaGateway) hops.add(DemoHop.of("gateway", 8080, true, "route /api/** → order"));
        hops.add(DemoHop.of("order-service", 8081, true, "execute flow"));
        hops.add(DemoHop.of("risk-service", 8082, riskOk, riskDetail));
        return new DemoTrace(UUID.randomUUID().toString(), "EXECUTE", viaGateway,
                null, orderId, status, List.copyOf(hops), Instant.now());
    }

    public static DemoTrace forCancel(boolean viaGateway, Long orderId, String status) {
        List<DemoHop> hops = new ArrayList<>();
        if (viaGateway) hops.add(DemoHop.of("gateway", 8080, true, "route /api/** → order"));
        hops.add(DemoHop.of("order-service", 8081, true, "cancel PENDING → CANCELLED"));
        return new DemoTrace(UUID.randomUUID().toString(), "CANCEL", viaGateway,
                null, orderId, status, List.copyOf(hops), Instant.now());
    }
}
```

- [ ] **Step 4: TradingService 映射時 setDemoTrace**

在 `toResponse`（或 create／execute／cancel 回傳前）：

- 讀 `HttpServletRequest` header `X-Demo-Via-Gateway`（經 `RequestContextHolder` 或方法參數傳入 viaGateway，避免污染 domain——**建議** Controller 讀 header 傳入 Service 多載，或用 `DemoRequestContext` ThreadLocal 由 Filter 設定）。

**建議實作（較乾淨）：**

Create `order-service/.../demo/DemoGatewayHintFilter.java`：

```java
@Component
public class DemoGatewayHintFilter extends OncePerRequestFilter {
    public static final String ATTR = "demo.viaGateway";
    @Override
    protected void doFilterInternal(...) {
        boolean via = "1".equals(request.getHeader("X-Demo-Via-Gateway"));
        request.setAttribute(ATTR, via);
        filterChain.doFilter(request, response);
    }
}
```

Service 內：

```java
private boolean viaGateway() {
    var attrs = RequestContextHolder.getRequestAttributes();
    if (attrs instanceof ServletRequestAttributes sra) {
        Object v = sra.getRequest().getAttribute(DemoGatewayHintFilter.ATTR);
        return Boolean.TRUE.equals(v);
    }
    return false;
}
```

create 回傳前：`resp.setDemoTrace(DemoTraceFactory.forCreate(viaGateway(), id, status.name()));`  
execute：依 risk 結果填 `forExecute(...)`  
cancel：`forCancel(...)`

- [ ] **Step 5: 擴充 TradingServiceTest**

在既有 `execute_whenRiskAllows...` 加：

```java
assertThat(resp.getDemoTrace()).isNotNull();
assertThat(resp.getDemoTrace().action()).isEqualTo("EXECUTE");
assertThat(resp.getDemoTrace().hops()).anyMatch(h -> "risk-service".equals(h.service()) && h.ok());
```

reject 案例：`risk-service` hop `ok()==false`。

- [ ] **Step 6: 跑測綠燈**

Run: `.\gradlew.bat :order-service:test --tests com.fintech.demo.order.demo.DemoTraceFactoryTest --tests com.fintech.demo.order.application.TradingServiceTest`  
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add order-service/src/main/java/com/fintech/demo/order/demo \
  order-service/src/main/java/com/fintech/demo/order/application/TradingService.java \
  order-service/src/test/java/com/fintech/demo/order/demo \
  order-service/src/test/java/com/fintech/demo/order/application/TradingServiceTest.java
git commit -m "feat(order): attach demoTrace on create/execute/cancel"
```

---

### Task 3: Topology API（伺服器端 health 探測）

**Files:**
- Create: `order-service/.../demo/TopologyService.java`
- Create: `order-service/.../demo/TopologyController.java`
- Create: `order-service/.../dto/TopologyResponse.java`
- Modify: `order-service/.../application.yml`（或既有 services URL 設定）讀 risk／可選 gateway／account／job URL
- Test: `TopologyServiceTest`（MockWebServer 或 mock RestClient）

- [ ] **Step 1: 回應形狀**

```java
public record ServiceHealth(String id, String label, int port, String url, boolean up) {}
public record TopologyResponse(List<ServiceHealth> services, String inferredStage, Instant at) {}
```

services 固定順序：gateway:8080、order:8081、risk:8082、job:8083、account:8084。  
order 自身可直接 `up=true`（本行程）；其餘對 `{base}/actuator/health` GET，2xx 且 body 含 UP 才算。

推斷：

```text
order up only → S1
order+risk → S2
(order+risk) && (gateway || account) → S3
```

- [ ] **Step 2: Controller**

```java
@RestController
@RequestMapping("/api/demo")
public class TopologyController {
    @GetMapping("/topology")
    public TopologyResponse topology() { return topologyService.probe(); }
}
```

需 JWT（與 `/api/**` 相同）。

- [ ] **Step 3: 測試** — risk URL mock 回 UP → inferredStage 至少 S2（order+risk）。

- [ ] **Step 4: Commit**

```bash
git commit -m "feat(order): add GET /api/demo/topology for service lamps"
```

---

### Task 4: Gateway 加 X-Demo-Via-Gateway

**Files:**
- Modify: `gateway/src/main/java/com/fintech/demo/gateway/config/GatewayRouteConfig.java`  
  或 Create filter bean compatible with Spring Cloud Gateway MVC

- [ ] **Step 1: 在轉發前加 request header**

對 `order-api`／`account-api` 路由使用 filter（依專案 Gateway MVC API）：

```java
// 概念：.before((request) -> ServerRequest.from(request)
//   .header("X-Demo-Via-Gateway", "1").build())
```

查現有 Spring Cloud Gateway Server MVC `FilterFunctions`／`before` 用法，以**實際可編譯**程式為準。

- [ ] **Step 2: 手動或整合驗證** — 經 :8080 打 create，order 日誌／回應 `viaGateway=true`。

- [ ] **Step 3: Commit**

```bash
git commit -m "feat(gateway): stamp X-Demo-Via-Gateway on proxied requests"
```

---

### Task 5: 前端剧本＋ merge＋ inferStage（純 JS）

**Files:**
- Create: `frontend/src/demo/processScripts.js`
- Create: `frontend/src/demo/mergeTrace.js`
- Create: `frontend/src/demo/inferStage.js`
- Create: `frontend/src/demo/mergeTrace.test.js`（若無 vitest，改用手動斷言腳本或略過改 Task 6 目視；**優先**加 vitest 僅測 merge——若 `package.json` 無 test runner，則用 Node `assert` 小檔 `node frontend/src/demo/mergeTrace.selftest.mjs`）

- [ ] **Step 1: 劇本表**

```js
export const SCRIPTS = {
  CREATE_ORDER: [
    { service: 'frontend', title: '交易前台', purpose: '送出下單表單', stateHint: 'UI → API' },
    { service: 'gateway', title: 'Gateway :8080', purpose: '統一入口轉發 /api', stateHint: '可選', optional: true },
    { service: 'order-service', title: 'Order :8081', purpose: '建立訂單並落庫', stateHint: '→ PENDING' }
  ],
  EXECUTE: [
    { service: 'frontend', title: '交易前台', purpose: '點擊成交', stateHint: 'UI → API' },
    { service: 'gateway', title: 'Gateway :8080', purpose: '統一入口轉發', stateHint: '可選', optional: true },
    { service: 'order-service', title: 'Order :8081', purpose: '執行成交流程', stateHint: 'PENDING → …' },
    { service: 'risk-service', title: 'Risk :8082', purpose: '名目金額風控（Feign）', stateHint: '通過→ACCEPTED；拒絕→REJECTED' }
  ],
  CANCEL: [ /* frontend, gateway?, order → CANCELLED */ ]
};
```

- [ ] **Step 2: mergeTrace(demoTrace) → steps[]**

規則：取 `SCRIPTS[action]`；若 `!viaGateway` 去掉 gateway；用 hops 依 service 對上 ok／detail；前端步永遠 ok（除非無 trace）。

- [ ] **Step 3: inferStage(services) → 'S1'|'S2'|'S3'**

與後端公式一致（前端可再用 topology.inferredStage 為準，本地函式當備用）。

- [ ] **Step 4: selftest 綠燈**

Run: `node frontend/src/demo/mergeTrace.selftest.mjs`  
Expected: prints OK

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(frontend): add process scripts and demoTrace merge helpers"
```

---

### Task 6: demoStory store ＋ client 擷取

**Files:**
- Create: `frontend/src/stores/demoStory.js`（對齊既有 pinia `auth` store 風格）
- Modify: `frontend/src/api/client.js`

- [ ] **Step 1: store**

```js
// state: lastTrace, topology, pinStage (null|string), error
// actions: setTrace(t), async refreshTopology(), setPinStage(s|null)
```

- [ ] **Step 2: client**

```js
export async function fetchTopology() {
  const { data } = await api.get('/demo/topology');
  return data;
}
```

在 `createOrder`／`executeOrder`／`cancelOrder` 成功後：

```js
if (data?.demoTrace) useDemoStoryStore().setTrace(data.demoTrace);
```

（注意：store 在 interceptor／非 setup 呼叫需確保 pinia 已 install——與 `useAuthStore` 相同模式。）

- [ ] **Step 3: Commit**

```bash
git commit -m "feat(frontend): wire demoTrace capture and topology fetch"
```

---

### Task 7: BackendStoryPanel.vue ＋ 嵌入 Trade／Portal

**Files:**
- Create: `frontend/src/components/BackendStoryPanel.vue`
- Modify: `frontend/src/views/TradeView.vue`
- Modify: `frontend/src/views/PortalView.vue`
- Modify: `frontend/src/styles.css`

- [ ] **Step 1: 面板 UI 結構**

1. 標題「後端過程（PROCESS FLOW）」  
2. 步驟列表：三欄 **誰／做什麼／狀態**；ok=false 紅色  
3. 服務燈列（topology.services）  
4. S1–S3 狀態機＋「釘住敘事」下拉／按鈕  
5. 訂單狀態機：PENDING → ACCEPTED｜REJECTED｜CANCELLED（高亮 lastTrace.orderStatus）

`onMounted`＋`setInterval(5000)` 呼叫 `refreshTopology`；`onUnmounted` clear。

- [ ] **Step 2: Trade／Portal 布局**

外層 `.story-layout`：`display:grid; grid-template-columns: 1fr 1fr;`（`max-width` 時改單欄）。左原內容、右 `<BackendStoryPanel />`。

- [ ] **Step 3: 手動驗收**

1. Order+Risk+frontend：拓撲 S2；下單見 CREATE 流程；成交見 Risk hop  
2. 停 Risk：topology risk 紅；成交失敗／REJECTED 時 risk hop 紅  
3. Portal 同樣看得到面板與上次 trace  

- [ ] **Step 4: Commit**

```bash
git commit -m "feat(frontend): embed BackendStoryPanel on Trade and Portal"
```

---

### Task 8: 整合測＋文件微調

**Files:**
- Modify: `order-service/src/test/java/com/fintech/demo/order/TradingFlowIntegrationTest.java`（jsonPath `$.demoTrace.action`）
- Modify: `order-service/.../StartupInfoLogger.java`（一行提示：Trade／Portal 內建後端過程面板）
- Optional: 規格檔加註 CORS→topology API 偏差

- [ ] **Step 1: MockMvc 斷言 create／execute 含 demoTrace**

- [ ] **Step 2: `.\gradlew.bat :order-service:test` 全綠**

- [ ] **Step 3: Commit**

```bash
git commit -m "test(order): assert demoTrace on trading HTTP flow"
```

---

## Spec coverage checklist

| Spec 項 | Task |
|---------|------|
| Trade／Portal 嵌入 | 7 |
| 三欄 PROCESS FLOW | 5＋7 |
| health 事實燈 | 3＋6＋7（topology，非瀏覽器直 ping） |
| demoTrace | 1＋2＋6 |
| S1–S3＋釘住 | 3＋7 |
| 訂單狀態機（真實四態） | 7 |
| Gateway via 標記 | 4＋2 |
| 不做 APM／K8s API | 全程遵守 |
| 關 Risk 展演 | 2＋7 驗收 |

---

## Execution handoff

Plan complete and saved to `docs/superpowers/plans/2026-08-06-backend-story-panel.md`.

**Two execution options:**

1. **Subagent-Driven（recommended）** — 每 Task 開新 subagent，Task 間審查  
2. **Inline Execution** — 本對話依 executing-plans 連續做完  

Which approach?

/**
 * 【職責】系統運作藍圖的 Mermaid 圖碼與技術版本常數（對齊 gradle／package.json）。
 * 【概念】靜態 Demo 敘事；不呼叫 topology API。
 */

export const TECH_STACK = [
  { layer: '前端', tech: 'Vue', version: '^3.5.13', purpose: '組前端畫面與互動（SPA）', note: 'Composition API；交易前台／會員後台／藍圖頁' },
  { layer: '前端', tech: 'Vue Router', version: '^4.5.0', purpose: '前端頁面路由與導覽守衛', note: 'JWT 守衛（requiresAuth／admin）；公開／blueprint' },
  { layer: '前端', tech: 'Axios', version: '^1.7.9', purpose: '瀏覽器發 HTTP 呼叫後端 API', note: 'Order／Gateway；自動帶 Bearer；401 清 session' },
  { layer: '前端', tech: 'Vite', version: '^6.0.7', purpose: '前端開發伺服器與打包建置', note: ':5173 HMR；npm run build 產 dist' },
  { layer: '前端', tech: 'Mermaid', version: '^11.16', purpose: '把流程／架構圖文字渲染成圖', note: '僅藍圖頁動態載入，不進其他頁主包' },
  { layer: '後端', tech: 'Java', version: '21', purpose: '後端業務邏輯的執行語言', note: 'Gradle toolchain；Lombok；虛擬執行緒可用' },
  { layer: '後端', tech: 'Spring Boot', version: '3.2.2', purpose: '快速啟動 Web／微服務應用', note: 'gateway／order／risk／account／job 各服務基底' },
  { layer: '後端', tech: 'Spring Cloud', version: '2023.0.0', purpose: '統一鎖定微服務相關套件版本（BOM）', note: 'BOM＝Bill of Materials；Gateway／OpenFeign 版本自動對齊，不必手寫版號' },
  { layer: '後端', tech: 'Gateway MVC', version: 'spring-cloud-gateway-server-mvc', purpose: '統一 API 入口、轉發到後端服務', note: ':8080 → Order；RateLimitWebFilter 固定窗口限流（超限 429）' },
  { layer: '後端', tech: 'OpenFeign', version: 'spring-cloud-starter-openfeign', purpose: '服務間用介面做同步 HTTP 呼叫', note: '成交時 Order → Risk :8082 名目風控；通過 ACCEPTED／拒絕 REJECTED' },
  { layer: '後端', tech: 'Spring Security', version: 'Boot starter', purpose: '認證與授權（誰能打哪些 API）', note: '登入、JWT 過濾、ROLE_USER／ROLE_ADMIN（RBAC）' },
  { layer: '後端', tech: 'WebConfig（CORS）', version: 'Spring MVC', purpose: '允許瀏覽器跨域呼叫 API', note: 'Order config/WebConfig；允許 Vue :5173 呼叫 /api/**' },
  { layer: '後端', tech: 'JJWT', version: '0.12.5', purpose: '在 Java 裡簽發／驗證 JWT 字串', note: '≠ JWT 標準本身；HS 簽驗；前端 Token 存 localStorage' },
  { layer: '後端', tech: 'Spring Data JPA', version: 'Boot starter', purpose: '用物件操作資料庫（ORM）', note: 'Order／Account 實體；Repository＋交易邊界' },
  { layer: '後端', tech: 'H2', version: 'runtime', purpose: '本機／測試用輕量資料庫', note: 'jdbc:h2:mem:…；免 Docker 即可 Demo' },
  { layer: '後端', tech: 'springdoc OpenAPI', version: '2.3.0', purpose: '自動產生 API 文件與試打介面', note: 'Order :8081 /swagger-ui.html' },
  { layer: '後端', tech: 'Actuator + Prometheus', version: 'Micrometer', purpose: '健康檢查與監控指標輸出', note: '/actuator/health 拓撲燈；/actuator/prometheus 刮取' },
  { layer: '基建', tech: 'Kafka', version: 'spring-kafka', purpose: '非同步訊息／事件串流', note: 'order／account 事件（可選）；無 broker 時降級' },
  { layer: '基建', tech: 'Redis Cache', version: 'starter-data-redis', purpose: '讀側 cache-aside 快取', note: '僅 account 連 :6379；key account:{id}／positions:{id}；TTL 600s；入帳 evict；本機 Demo 預設開' },
  { layer: '基建', tech: 'PostgreSQL', version: 'prod 敘述', purpose: '正式環境持久化關聯式資料庫', note: 'docker／prod profile；local 改用 H2' },
  { layer: '排程', tech: 'Job Service', version: 'Spring Boot 3.2', purpose: '定時背景工作（排程任務）', note: ':8083 逾時 PENDING→CANCELLED（可選；不進 S 公式）' }
];

/** 技術棧分組順序與框標題說明 */
export const TECH_LAYER_META = [
  { id: '前端', title: '前端', blurb: 'Vue SPA · 瀏覽器 :5173', tone: 'front' },
  { id: '後端', title: '後端', blurb: 'Java 21 · Spring Boot 微服務', tone: 'back' },
  { id: '基建', title: '基建', blurb: '訊息／快取／正式庫（可選或 prod）', tone: 'infra' },
  { id: '排程', title: '排程', blurb: 'Job 服務 · 逾時取消', tone: 'job' }
];

/** 【目的】依層切成框用資料；維持 TECH_LAYER_META 順序。 */
export function groupTechStack(rows = TECH_STACK) {
  return TECH_LAYER_META.map((meta) => ({
    ...meta,
    items: rows.filter((r) => r.layer === meta.id)
  })).filter((g) => g.items.length > 0);
}

export const PORTS = [
  { port: 8080, service: 'Gateway', role: '統一入口＋RateLimitWebFilter（可選）' },
  { port: 8081, service: 'Order', role: '登入／下單／成交／審計／WebConfig CORS' },
  { port: 8082, service: 'Risk', role: '名目金額風控（成交必開）' },
  { port: 8083, service: 'Job', role: '逾時取消排程（可選）' },
  { port: 8084, service: 'Account', role: '餘額／持倉／Redis Cache（本機預設開）／Kafka（可選）' },
  { port: 6379, service: 'Redis', role: 'Account 快取（開啟Demo 預設 docker compose up -d redis）' },
  { port: 9093, service: 'order H2 TCP', role: 'DataGrip：jdbc:h2:tcp://localhost:9093/mem:fintechdemo' },
  { port: 9094, service: 'account H2 TCP', role: 'DataGrip：jdbc:h2:tcp://localhost:9094/mem:accountdb' },
  { port: 5173, service: 'Vue Dev', role: '前端 SPA' },
  { port: 3000, service: 'Grafana', role: '把監控數字畫成圖表；登入 admin／admin' },
  { port: 9090, service: 'Prometheus', role: '定時收集各服務數字；可查詢／看曲線' },
  { port: 8089, service: 'Locust', role: '模擬很多人打 API 做壓測' }
];

/**
 * 【職責】藍圖「邊緣機制」表：限流／CORS／Redis Cache（Demo 可講、可指到類別）。
 * 【概念】對齊 Trading System MVP 架構介紹的「類別｜路徑｜職責」敘事。
 */
export const EDGE_MECHANISMS = [
  {
    name: 'RateLimitWebFilter',
    where: 'gateway/.../filter/RateLimitWebFilter.java',
    what: 'Gateway 入口每秒固定窗口限流；超限回 429 + Retry-After',
    config: 'fintech.gateway.rate-limit.enabled／per-second（預設 80）',
    demo: '開 Gateway :8080 後大量打 /api/** 可見 429；多副本正式版可改 Redis（見 APIGatewayMQ）'
  },
  {
    name: 'WebConfig（CORS）',
    where: 'order-service/.../config/WebConfig.java',
    what: '允許 Vue :5173 跨域呼叫 /api/**（Security 鏈已啟用 .cors）',
    config: 'fintech.cors.allowed-origins',
    demo: '直連 :8081 時瀏覽器不擋跨域；開發也可用 Vite proxy 免 CORS'
  },
  {
    name: 'Redis Cache',
    where: 'account-service/.../AccountQueryService.java',
    what: '讀側 cache-aside；入帳後 evict，避免髒讀',
    config: 'key account:{userId}／positions:{userId}；TTL 600s；fintech.redis.enabled（本機預設 true）',
    demo: '見藍圖 #docker-redis：開啟Demo 起 :6379；種子預熱 key；無 Redis 降級打 H2'
  }
];

/** 分層架構（換行用 &lt;br/&gt;，勿寫字面 \n） */
export const DIAGRAM_LAYERS = `flowchart TB
  subgraph Client["前端層"]
    Vue["Vue 3.5 + Router 4.5 + Axios<br/>Vite 6 · :5173"]
  end

  subgraph Edge["入口層（可選）"]
    GW["Gateway MVC :8080<br/>RateLimitWebFilter<br/>超限 429"]
  end

  subgraph MS["微服務層 · Java 21 · Spring Boot 3.2.2"]
    Order["Order Service :8081<br/>JWT · WebConfig CORS<br/>JPA · Feign · Kafka"]
    Risk["Risk Service :8082<br/>名目金額風控 API"]
    Account["Account Service :8084<br/>JPA · Redis Cache · Kafka"]
    Job["Job Service :8083<br/>逾時取消排程"]
  end

  subgraph Data["資料／訊息層"]
    H2[(H2 / PostgreSQL)]
    Redis[(Redis cache-aside)]
    Kafka[(Kafka)]
  end

  Vue -->|"HTTP + JWT<br/>最短：直連 Order<br/>CORS 允許 :5173"| Order
  Vue -.->|"可選：經 Gateway"| GW
  GW -->|"限流後轉發<br/>X-Demo-Via-Gateway"| Order
  Order -->|"OpenFeign"| Risk
  Order -.->|"事件（可選）"| Kafka
  Order --> H2
  Account --> H2
  Account -->|"TTL 600s / evict"| Redis
  Account -.-> Kafka
  Job -->|"取消 PENDING"| Order
`;

/** 完整運作過程（含技術） */
export const DIAGRAM_FLOW = `flowchart TD
  A["① 登入 Login<br/>Vue → Order :8081<br/>Spring Security + JWT<br/>JJWT 0.12.5 簽驗 · RBAC"]
  B["② 下單 Create<br/>Vue → Order<br/>JPA 持久化 · WebConfig CORS<br/>狀態 PENDING"]
  C{"③ 路徑？"}
  D["③a 經 Gateway :8080<br/>RateLimitWebFilter<br/>超限 429 · 再轉發 Order"]
  E["③b 直連 Order :8081<br/>Demo 最短路徑"]
  F["④ 點擊成交 Execute<br/>Order 啟動成交流程"]
  G["⑤ Feign → Risk :8082<br/>名目金額風控檢查"]
  H{"⑥ 風控結果"}
  I["通過 → ACCEPTED<br/>訂單狀態機終態之一"]
  J["拒絕 → REJECTED<br/>名目超限等"]
  K["⑦ 可選 Account :8084<br/>apply-trade<br/>Redis cache-aside · evict"]
  L["⑧ 可選 Job :8083<br/>逾時未成交<br/>PENDING → CANCELLED"]

  A --> B --> C
  C -->|分散式敘事| D --> F
  C -->|最短可成交| E --> F
  F --> G --> H
  H -->|ok| I --> K
  H -->|fail| J
  B -.-> L
`;

/** 訂單狀態機（flowchart 避免 stateDiagram 長標籤截斷） */
export const DIAGRAM_ORDER_STATE = `flowchart LR
  Start([建立訂單]) -->|"CreateOrder<br/>JPA 寫入"| PENDING
  PENDING -->|"Execute + Risk 通過<br/>OpenFeign"| ACCEPTED
  PENDING -->|"Execute + Risk 拒絕"| REJECTED
  PENDING -->|"使用者取消<br/>或 Job 逾時"| CANCELLED
  ACCEPTED --> Done([結束])
  REJECTED --> Done
  CANCELLED --> Done

  style PENDING fill:#fef3c7,stroke:#b45309,color:#1a2b3c
  style ACCEPTED fill:#d1fae5,stroke:#047857,color:#1a2b3c
  style REJECTED fill:#fee2e2,stroke:#b91c1c,color:#1a2b3c
  style CANCELLED fill:#e5e7eb,stroke:#4b5563,color:#1a2b3c
`;

/**
 * Docker Desktop 映像 → kind load → K8s Pod（#k8s-intellij 教學圖）。
 * 【概念】Desktop 有 Images ≠ Pod Running；需 arch 一致 + kind load + kubectl apply。
 */
export const DIAGRAM_DOCKER_K8S = `flowchart TB
  subgraph L1["Layer 1 · Docker Desktop → Images"]
    direction TB
    IMG1["fintech-demo/gateway:local<br/>Deployment gateway · API :8080"]
    IMG2["fintech-demo/order-service:local<br/>Deployment order-service · :8081"]
    IMG3["fintech-demo/risk-service:local<br/>Deployment risk-service · :8082"]
    IMG4["fintech-demo/account-service:local<br/>Deployment account-service · :8084"]
  end

  LOAD["kind load docker-image<br/>把映像搬進叢集節點"]
  NODE["Layer 2 · kind 節點<br/>trading-local-control-plane<br/>Desktop Containers 可見 1 個"]

  APPLY["kubectl apply -k deploy/k8s/overlays/dev"]
  subgraph L3["Layer 3 · K8s · namespace fintech-demo"]
    direction TB
    POD1["Pod gateway + Service :8080"]
    POD2["Pod order-service + Service :8081"]
    POD3["Pod risk-service + Service :8082"]
    POD4["Pod account-service + Service :8084"]
  end

  PF["port-forward 18080:8080<br/>本機打叢集內 Gateway"]
  VITE["Vite :5173 另開<br/>K8s 不會自動給前端"]

  IMG1 --> LOAD
  IMG2 --> LOAD
  IMG3 --> LOAD
  IMG4 --> LOAD
  LOAD --> NODE
  NODE --> APPLY
  APPLY --> POD1
  APPLY --> POD2
  APPLY --> POD3
  APPLY --> POD4
  POD1 --> PF
  PF -.-> VITE

  NOTE["Desktop 有映像 ≠ Pod Running<br/>還要：kind load 成功 · CPU arch 一致 · apply"]
  L1 -.-> NOTE
`;

/**
 * 兩套 K8s「大樓」：Desktop 內建 vs FinTechDemo kind（Panel 找不到 fintech-demo 的原因）。
 */
export const DIAGRAM_TWO_K8S_BUILDINGS = `flowchart TB
  subgraph DD["🏢 A · Docker Desktop 內建 K8s"]
    direction TB
    DDC["context: docker-desktop"]
    DDN["節點: desktop-control-plane"]
    DDNS["namespace: default · kube-system · kube-public…"]
  end

  subgraph KIND["🏢 B · FinTechDemo kind 叢集"]
    direction TB
    KC["context: kind-trading-local"]
    KN["節點: trading-local-control-plane"]
    KNS["namespace: fintech-demo ← 4 Pod"]
  end

  PANEL["Docker Desktop<br/>Kubernetes Panel"] -->|"預設連這裡"| DD
  KCTL["kubectl · walkthrough<br/>start-k8s-demo.ps1"] -->|"Demo 驗收入口"| KIND

  DD -.->|"Panel 看不到 fintech-demo"| X["❌ 不是 bug"]
  KIND -.->|"apply 建在 B 棟"| OK["✓ 4 Pod Running"]
`;

/**
 * Vue SPA 頁面關係（#frontend-pages）。
 * 【概念】一個 index.html → App.vue 殼；router-view 換頁；/demo/*.html 是 public 靜態頁，不經 Vue Router。
 */
export const DIAGRAM_FRONTEND_PAGES = `flowchart TB
  IDX["index.html<br/>掛載 #app"]
  APP["App.vue 殼<br/>登入後：頂欄 + Demo 快捷"]
  RV["router-view"]

  IDX --> APP --> RV

  subgraph SPA["Vue Router（frontend/src/views）"]
    LOGIN["/login<br/>LoginView.vue<br/>公開"]
    BP["/blueprint<br/>BlueprintView.vue<br/>公開 · 本頁"]
    TRADE["/trade<br/>TradeView.vue<br/>需 JWT"]
    PORTAL["/portal<br/>PortalView.vue<br/>需 JWT"]
    AUDIT["/portal/audit<br/>AuditView.vue<br/>需 ADMIN"]
  end

  RV --> LOGIN
  RV --> BP
  RV --> TRADE
  RV --> PORTAL
  RV --> AUDIT

  LOGIN -->|"登入成功"| TRADE
  LOGIN -->|"可不登入"| BP
  TRADE -->|"頂欄"| PORTAL
  TRADE -->|"頂欄"| BP
  PORTAL -->|"ADMIN"| AUDIT

  subgraph STATIC["public 靜態 HTML（不經 Router）"]
    RC["/demo/risk-check.html"]
    AM["/demo/account-me.html"]
  end

  APP -.->|"另開分頁"| STATIC
`;

/**
 * 前端三種打包／部署（本機 Vite vs Docker 映像 vs kind）。
 */
export const DIAGRAM_FRONTEND_DEPLOY = `flowchart TB
  SRC["frontend/ 原始碼<br/>Vue + Vite"]

  subgraph LOCAL["路線 A · 本機（日常）"]
    NPM["npm run dev :5173"]
    PROXY["vite.config.js<br/>proxy /api → localhost:8081"]
    BOOT["IntelliJ OrderServiceApplication<br/>或 :order-service:bootRun"]
    NPM --> PROXY --> BOOT
  end

  subgraph DOCKER["Docker Desktop 映像"]
    DF["frontend/Dockerfile<br/>node build → nginx:80"]
    IMG["fintech-demo/frontend:local"]
    NGINX["nginx.conf<br/>/api → gateway:8080"]
    COMPOSE["docker compose --profile full<br/>up -d frontend<br/>主機 5173:80"]
    SRC --> DF --> IMG --> NGINX --> COMPOSE
  end

  subgraph K8S["路線 B · kind（目前無 frontend Pod）"]
    PODS["gateway / order / risk / account"]
    PF["port-forward 18080 或 8081"]
    VITE2["仍用 npm run dev 接 PF"]
    PODS --> PF --> VITE2
  end

  SRC --> NPM
`;

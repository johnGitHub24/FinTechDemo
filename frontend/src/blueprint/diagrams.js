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
  { layer: '後端', tech: 'Gateway MVC', version: 'spring-cloud-gateway-server-mvc', purpose: '統一 API 入口、轉發到後端服務', note: ':8080 轉發 Order；可加 X-Demo-Via-Gateway' },
  { layer: '後端', tech: 'OpenFeign', version: 'spring-cloud-starter-openfeign', purpose: '服務間用介面做同步 HTTP 呼叫', note: '成交時 Order → Risk :8082 名目風控；通過 ACCEPTED／拒絕 REJECTED' },
  { layer: '後端', tech: 'Spring Security', version: 'Boot starter', purpose: '認證與授權（誰能打哪些 API）', note: '登入、JWT 過濾、ROLE_USER／ROLE_ADMIN（RBAC）' },
  { layer: '後端', tech: 'JJWT', version: '0.12.5', purpose: '在 Java 裡簽發／驗證 JWT 字串', note: '≠ JWT 標準本身；HS 簽驗；前端 Token 存 localStorage' },
  { layer: '後端', tech: 'Spring Data JPA', version: 'Boot starter', purpose: '用物件操作資料庫（ORM）', note: 'Order／Account 實體；Repository＋交易邊界' },
  { layer: '後端', tech: 'H2', version: 'runtime', purpose: '本機／測試用輕量資料庫', note: 'jdbc:h2:mem:…；免 Docker 即可 Demo' },
  { layer: '後端', tech: 'springdoc OpenAPI', version: '2.3.0', purpose: '自動產生 API 文件與試打介面', note: 'Order :8081 /swagger-ui.html' },
  { layer: '後端', tech: 'Actuator + Prometheus', version: 'Micrometer', purpose: '健康檢查與監控指標輸出', note: '/actuator/health 拓撲燈；/actuator/prometheus 刮取' },
  { layer: '基建', tech: 'Kafka', version: 'spring-kafka', purpose: '非同步訊息／事件串流', note: 'order／account 事件（可選）；無 broker 時降級' },
  { layer: '基建', tech: 'Redis', version: 'starter-data-redis', purpose: '高速快取／暫存狀態', note: 'Account 餘額等（可選）；無 Redis 時降級' },
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
  { port: 8080, service: 'Gateway', role: '統一入口（可選）' },
  { port: 8081, service: 'Order', role: '登入／下單／成交／審計' },
  { port: 8082, service: 'Risk', role: '名目金額風控（成交必開）' },
  { port: 8083, service: 'Job', role: '逾時取消排程（可選）' },
  { port: 8084, service: 'Account', role: '餘額／持倉／Redis／Kafka（可選）' },
  { port: 5173, service: 'Vue Dev', role: '前端 SPA' }
];

/** 分層架構（換行用 &lt;br/&gt;，勿寫字面 \n） */
export const DIAGRAM_LAYERS = `flowchart TB
  subgraph Client["前端層"]
    Vue["Vue 3.5 + Router 4.5 + Axios<br/>Vite 6 · :5173"]
  end

  subgraph Edge["入口層（可選）"]
    GW["Gateway MVC<br/>Spring Cloud Gateway · :8080"]
  end

  subgraph MS["微服務層 · Java 21 · Spring Boot 3.2.2"]
    Order["Order Service :8081<br/>Security JWT · JPA · Feign<br/>Kafka · Actuator · springdoc"]
    Risk["Risk Service :8082<br/>名目金額風控 API"]
    Account["Account Service :8084<br/>JPA · Redis · Kafka"]
    Job["Job Service :8083<br/>逾時取消排程"]
  end

  subgraph Data["資料／訊息層"]
    H2[(H2 / PostgreSQL)]
    Redis[(Redis)]
    Kafka[(Kafka)]
  end

  Vue -->|"HTTP + JWT<br/>最短：直連 Order"| Order
  Vue -.->|"可選：經 Gateway"| GW
  GW -->|"轉發 + X-Demo-Via-Gateway"| Order
  Order -->|"OpenFeign"| Risk
  Order -.->|"事件（可選）"| Kafka
  Order --> H2
  Account --> H2
  Account --> Redis
  Account -.-> Kafka
  Job -->|"取消 PENDING"| Order
`;

/** 完整運作過程（含技術） */
export const DIAGRAM_FLOW = `flowchart TD
  A["① 登入 Login<br/>Vue → Order :8081<br/>Spring Security + JWT<br/>JJWT 0.12.5 簽驗 · RBAC"]
  B["② 下單 Create<br/>Vue → Order<br/>JPA 持久化<br/>狀態 PENDING"]
  C{"③ 路徑？"}
  D["③a 經 Gateway :8080<br/>Gateway MVC 轉發<br/>標頭 X-Demo-Via-Gateway"]
  E["③b 直連 Order :8081<br/>Demo 最短路徑"]
  F["④ 點擊成交 Execute<br/>Order 啟動成交流程"]
  G["⑤ Feign → Risk :8082<br/>名目金額風控檢查"]
  H{"⑥ 風控結果"}
  I["通過 → ACCEPTED<br/>訂單狀態機終態之一"]
  J["拒絕 → REJECTED<br/>名目超限等"]
  K["⑦ 可選 Account :8084<br/>apply-trade<br/>Redis 快取 · Kafka 事件"]
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

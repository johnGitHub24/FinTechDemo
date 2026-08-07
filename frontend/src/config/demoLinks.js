/**
 * 【職責】Demo／簡報用外部連結（與 StartupInfoLogger 橫幅關鍵 URL 對齊）。
 * 【技巧】單一設定檔給 Login 快捷列與 App nav 共用，避免兩處漂移。
 * 【概念】Console 文字無法點；登入頁把同一組 URL 變成可點連結。服務未啟動時會連不上——登入頁拓撲燈會標示。
 */
const env = import.meta.env || {};

export const demoLinks = {
  frontend: 'http://localhost:5173',
  login: 'http://localhost:5173/login',
  trade: 'http://localhost:5173/trade',
  portal: 'http://localhost:5173/portal',
  audit: 'http://localhost:5173/portal/audit',
  blueprint: 'http://localhost:5173/blueprint',
  grafana: env.VITE_GRAFANA_URL || 'http://localhost:3000/d/fintechdemo-overview/fintechdemo-overview?orgId=1',
  prometheusUi: env.VITE_PROMETHEUS_URL || 'http://localhost:9090',
  locust: env.VITE_LOCUST_URL || 'http://localhost:8089',
  gatewayHealth: 'http://localhost:8080/actuator/health',
  orderHealth: 'http://localhost:8081/actuator/health',
  orderSwagger: 'http://localhost:8081/swagger-ui/index.html',
  orderOpenApi: 'http://localhost:8081/v3/api-docs',
  orderH2: 'http://localhost:8081/h2-console/',
  orderPrometheus: 'http://localhost:8081/actuator/prometheus',
  orderLoginApi: 'http://localhost:8081/api/auth/login',
  riskHealth: 'http://localhost:8082/actuator/health',
  // POST API 無法直接用瀏覽器開 → Demo 頁會自動送出並顯示結果
  riskCheck: 'http://localhost:5173/demo/risk-check.html',
  jobHealth: 'http://localhost:8083/actuator/health',
  accountHealth: 'http://localhost:8084/actuator/health',
  // 需 JWT → Demo 頁先 login 再查 /api/accounts/me
  accountMe: 'http://localhost:5173/demo/account-me.html',
  accountPositions: 'http://localhost:8084/api/positions',
  docsIndex: 'http://127.0.0.1:5500/docs/index.html',
  docsDemoFlow: 'http://127.0.0.1:5500/docs/portals/demo-flow.html',
  docsHandbook: 'http://127.0.0.1:5500/docs/portals/handbook.html',
  docsSwagger: 'http://127.0.0.1:5500/docs/portals/swagger.html',
  docsCodeGraphic: 'http://127.0.0.1:5500/docs/portals/codeGraphic.html'
};

/** 登入後頂部 nav：觀測＋壓測 */
export const navDemoButtons = [
  { label: 'Grafana', href: demoLinks.grafana, hint: '需 docker compose --profile monitoring' },
  { label: 'Prometheus', href: demoLinks.prometheusUi, hint: '需 monitoring profile' },
  { label: '壓測 UI', href: demoLinks.locust, hint: '.\\scripts\\run-loadtest.ps1 -WebUi' }
];

/**
 * 登入頁快捷分組 — 對齊 StartupInfoLogger 橫幅。
 * needLogin: 需先登入才能進 SPA 頁（仍提供連結，未登入會被導回）。
 */
export const loginDemoGroups = [
  {
    title: '前端頁面',
    items: [
      { label: 'Login', href: demoLinks.login },
      { label: 'Trade', href: demoLinks.trade, needLogin: true },
      { label: 'Portal', href: demoLinks.portal, needLogin: true },
      { label: 'Audit', href: demoLinks.audit, needLogin: true },
      { label: '藍圖', href: demoLinks.blueprint }
    ]
  },
  {
    title: 'Order ★ :8081',
    items: [
      { label: 'Health', href: demoLinks.orderHealth },
      { label: 'Swagger', href: demoLinks.orderSwagger },
      { label: 'OpenAPI', href: demoLinks.orderOpenApi },
      { label: 'H2 Console', href: demoLinks.orderH2 },
      { label: 'Prometheus', href: demoLinks.orderPrometheus }
    ]
  },
  {
    title: 'Risk ★ :8082（成交必開）',
    items: [
      { label: 'Health', href: demoLinks.riskHealth },
      { label: 'Risk Check', href: demoLinks.riskCheck }
    ]
  },
  {
    title: '其他後端',
    items: [
      { label: 'Gateway Health', href: demoLinks.gatewayHealth },
      { label: 'Job Health', href: demoLinks.jobHealth },
      { label: 'Account Health', href: demoLinks.accountHealth },
      { label: 'Account Me', href: demoLinks.accountMe }
    ]
  },
  {
    title: '觀測／壓測',
    items: [
      { label: 'Grafana', href: demoLinks.grafana },
      { label: 'Prometheus UI', href: demoLinks.prometheusUi },
      { label: 'Locust UI', href: demoLinks.locust }
    ]
  },
  {
    title: '學習文件（需 serve-docs）',
    items: [
      { label: 'Docs 入口', href: demoLinks.docsIndex },
      { label: 'Demo 流程', href: demoLinks.docsDemoFlow },
      { label: '學習手冊', href: demoLinks.docsHandbook },
      { label: 'Swagger 靜態', href: demoLinks.docsSwagger },
      { label: 'codeGraphic', href: demoLinks.docsCodeGraphic }
    ]
  }
];

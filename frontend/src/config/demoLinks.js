/**
 * 【職責】Demo 連結與頂欄／快捷面板設定（單一來源）。
 * 【技巧】nav 分 kind=external｜spa｜panel；SPA 用 path，禁止 target=_blank 開需登入頁。
 * 【概念】「總是出問題」常見於：新分頁無 JWT、Audit 非 ADMIN、觀測服務未起卻無提示。
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
  riskHealth: 'http://localhost:8082/actuator/health',
  riskCheck: 'http://localhost:5173/demo/risk-check.html',
  jobHealth: 'http://localhost:8083/actuator/health',
  accountHealth: 'http://localhost:8084/actuator/health',
  accountMe: 'http://localhost:5173/demo/account-me.html',
  docsIndex: 'http://127.0.0.1:5500/docs/index.html',
  docsDemoFlow: 'http://127.0.0.1:5500/docs/portals/demo-flow.html',
  docsHandbook: 'http://127.0.0.1:5500/docs/portals/handbook.html',
  docsSwagger: 'http://127.0.0.1:5500/docs/portals/swagger.html',
  docsCodeGraphic: 'http://127.0.0.1:5500/docs/portals/codeGraphic.html',
  docsJavadoc: 'http://127.0.0.1:5500/docs/javadoc/index.html',
  docsTestUnit: 'http://127.0.0.1:5500/docs/portals/test-reports.html#unit',
  docsTestIntegration: 'http://127.0.0.1:5500/docs/portals/test-reports.html#integration'
};

/**
 * 頂欄按鈕。
 * kind: external＝先探測再開；spa＝router；panel＝展開 Demo 快捷
 */
export const navDemoButtons = [
  {
    id: 'grafana',
    label: 'Grafana',
    kind: 'external',
    href: demoLinks.grafana,
    probe: 'http://localhost:3000/login',
    hint: '需 docker compose --profile monitoring',
    startHint: '.\\開啟Demo.cmd（Loop：含 monitoring）'
  },
  {
    id: 'prometheus',
    label: 'Prometheus',
    kind: 'external',
    href: demoLinks.prometheusUi,
    probe: 'http://localhost:9090/-/healthy',
    hint: '需 monitoring（ensure 會拉起）',
    startHint: '.\\開啟Demo.cmd'
  },
  {
    id: 'locust',
    label: '壓測 UI',
    kind: 'external',
    href: demoLinks.locust,
    probe: 'http://localhost:8089/',
    hint: 'Locust Web UI',
    startHint: '.\\開啟Demo.cmd'
  },
  {
    id: 'k8s',
    label: 'K8s／IntelliJ',
    kind: 'spa',
    to: '/blueprint',
    hash: 'k8s-intellij',
    hint: 'Services vs kind 部署教學'
  },
  {
    id: 'redis',
    label: 'Redis 指令',
    kind: 'spa',
    to: '/blueprint',
    hash: 'docker-redis',
    hint: 'Docker Desktop／redis-cli 教學'
  },
  {
    id: 'demo',
    label: 'Demo 快捷',
    kind: 'panel',
    hint: '展開下方快捷面板（Trade／Health／Docs）'
  }
];

export const loginDemoGroups = [
  {
    title: '前端頁面',
    items: [
      { label: 'Trade', spaPath: '/trade', needLogin: true },
      { label: 'Portal', spaPath: '/portal', needLogin: true },
      { label: 'Audit', spaPath: '/portal/audit', needLogin: true, needAdmin: true },
      { label: '藍圖', spaPath: '/blueprint' },
      { label: '藍圖·Redis', spaPath: '/blueprint#docker-redis' },
      { label: '藍圖·K8s', spaPath: '/blueprint#k8s-intellij' },
      { label: '藍圖·K8s 驗證', spaPath: '/blueprint#k8s-verify' }
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
    title: 'Risk ★ :8082',
    items: [
      { label: 'Health', href: demoLinks.riskHealth },
      { label: 'Risk Check', href: demoLinks.riskCheck }
    ]
  },
  {
    title: '其他後端',
    items: [
      { label: 'Gateway', href: demoLinks.gatewayHealth },
      { label: 'Job', href: demoLinks.jobHealth },
      { label: 'Account', href: demoLinks.accountHealth },
      { label: 'Account Me', href: demoLinks.accountMe }
    ]
  },
  {
    title: '觀測／壓測',
    items: [
      {
        label: 'Grafana',
        href: demoLinks.grafana,
        probe: 'http://localhost:3000/login',
        startHint: '.\\開啟Demo.cmd'
      },
      {
        label: 'Prometheus',
        href: demoLinks.prometheusUi,
        probe: 'http://localhost:9090/-/healthy',
        startHint: '.\\開啟Demo.cmd'
      },
      {
        label: 'Locust',
        href: demoLinks.locust,
        probe: 'http://localhost:8089/',
        startHint: '.\\開啟Demo.cmd'
      }
    ]
  },
  {
    title: '學習文件',
    items: [
      { label: 'Docs', href: demoLinks.docsIndex, probe: demoLinks.docsIndex },
      { label: 'Demo 流程', href: demoLinks.docsDemoFlow },
      { label: '學習手冊', href: demoLinks.docsHandbook },
      { label: 'Swagger 靜態', href: demoLinks.docsSwagger },
      { label: 'codeGraphic', href: demoLinks.docsCodeGraphic },
      {
        label: 'Javadoc',
        href: demoLinks.docsJavadoc,
        probe: demoLinks.docsJavadoc,
        startHint: '.\\開啟Demo.cmd（會跑 serve-docs／aggregateJavadoc）'
      },
      {
        label: '單元測試',
        href: demoLinks.docsTestUnit,
        probe: 'http://127.0.0.1:5500/docs/portals/test-reports.html',
        startHint: '.\\開啟Demo.cmd（缺報表會自動 gradlew test）'
      },
      {
        label: '整合測試',
        href: demoLinks.docsTestIntegration,
        probe: 'http://127.0.0.1:5500/docs/portals/test-reports.html',
        startHint: '.\\開啟Demo.cmd（缺報表會自動 gradlew test）'
      }
    ]
  }
];

export const NEXT_PATH_KEY = 'fintech_demo_next_path';

export const ENSURE_SERVICES_CMD =
  '.\\開啟Demo.cmd';

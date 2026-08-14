/**
 * 【職責】藍圖圖表／K8s 流程 ↔  repo 內 YAML、腳本、程式路徑對照（IDE 開檔用）。
 * 【概念】路徑皆相對 FinTechDemo 專案根；瀏覽器無法開檔，供 IntelliJ／VS Code 搜尋。
 */

/** K8s 三層流程 ↔ 設定／腳本 */
export const K8S_PIPELINE_REFS = [
  {
    layer: 'L1 建映像',
    diagram: 'gradlew bootJar → docker build',
    yaml: null,
    script: 'demo/start-k8s-demo.ps1',
    config: 'Dockerfile.k8s-local · demo/platform-run.properties（DOCKER_BUILD_PLATFORM）',
    code: '各模組 build.gradle（bootJar → app.jar）'
  },
  {
    layer: 'L2 kind load',
    diagram: 'kind load → trading-local-control-plane',
    yaml: null,
    script: 'demo/start-k8s-demo.ps1 · demo/k8s-walkthrough.ps1',
    config: 'demo/platform-run.properties（K8S_CLUSTER、K8S_KUBECONFIG_REL）',
    code: 'demo/platform-env.ps1'
  },
  {
    layer: 'L3 apply',
    diagram: 'kubectl apply -k overlays/dev',
    yaml: 'deploy/k8s/overlays/dev/kustomization.yaml → deploy/k8s/base/*',
    script: 'demo/start-k8s-demo.ps1 · demo/check-k8s.ps1',
    config: 'deploy/k8s/base/namespace.yaml（name: fintech-demo）',
    code: null
  },
  {
    layer: 'L3 驗證',
    diagram: 'kubectl get pods · port-forward',
    yaml: null,
    script: 'demo/k8s-walkthrough.ps1 · demo/verify-pipeline.ps1',
    config: 'demo/.tools/kubeconfig-kind-trading-local',
    code: null
  }
];

/** 映像 ↔ Deployment ↔ Service ↔ 程式入口 */
export const K8S_SERVICE_REFS = [
  {
    image: 'fintech-demo/gateway:local',
    deployName: 'gateway',
    port: ':8080',
    deployment: 'deploy/k8s/base/gateway-deployment.yaml',
    service: 'deploy/k8s/base/gateway-service.yaml',
    app: 'gateway/src/main/java/com/fintech/demo/gateway/GatewayApplication.java',
    module: 'gateway'
  },
  {
    image: 'fintech-demo/order-service:local',
    deployName: 'order-service',
    port: ':8081',
    deployment: 'deploy/k8s/base/order-deployment.yaml',
    service: 'deploy/k8s/base/order-service.yaml',
    app: 'order-service/src/main/java/com/fintech/demo/order/OrderServiceApplication.java',
    module: 'order-service'
  },
  {
    image: 'fintech-demo/risk-service:local',
    deployName: 'risk-service',
    port: ':8082',
    deployment: 'deploy/k8s/base/risk-deployment.yaml',
    service: 'deploy/k8s/base/risk-service.yaml',
    app: 'risk-service/src/main/java/com/fintech/demo/risk/RiskServiceApplication.java',
    module: 'risk-service'
  },
  {
    image: 'fintech-demo/account-service:local',
    deployName: 'account-service',
    port: ':8084',
    deployment: 'deploy/k8s/base/account-deployment.yaml',
    service: 'deploy/k8s/base/account-service.yaml',
    app: 'account-service/src/main/java/com/fintech/demo/account/AccountServiceApplication.java',
    module: 'account-service'
  }
];

/** 分層架構圖 ↔ 關鍵檔 */
export const LAYERS_FILE_REFS = [
  { node: 'Vue :5173', files: ['frontend/package.json', 'frontend/vite.config.js', 'frontend/src/views/TradeView.vue'] },
  { node: 'Gateway :8080', files: ['gateway/src/main/java/com/fintech/demo/gateway/GatewayApplication.java', 'gateway/src/main/java/com/fintech/demo/gateway/filter/RateLimitWebFilter.java'] },
  { node: 'Order :8081', files: ['order-service/src/main/java/com/fintech/demo/order/OrderServiceApplication.java', 'order-service/src/main/java/com/fintech/demo/order/config/WebConfig.java'] },
  { node: 'Risk :8082', files: ['risk-service/src/main/java/com/fintech/demo/risk/RiskServiceApplication.java'] },
  { node: 'Account :8084', files: ['account-service/src/main/java/com/fintech/demo/account/application/AccountQueryService.java'] },
  { node: 'Compose Redis', files: ['docker-compose.yml', 'demo/platform-run.properties（REDIS_PORT）'] }
];

/** 運作流程圖 ↔ 關鍵檔 */
export const FLOW_FILE_REFS = [
  { step: '① 登入 JWT', files: ['order-service/.../api/AuthController.java', 'order-service/.../config/SecurityConfig.java'] },
  { step: '② 下單 PENDING', files: ['order-service/.../api/OrderController.java', 'order-service/.../domain/OrderStatus.java'] },
  { step: '③ Gateway 限流', files: ['gateway/.../filter/RateLimitWebFilter.java', 'gateway/src/main/resources/application.yml'] },
  { step: '④⑤ 成交 + Feign Risk', files: ['order-service/.../application/TradingService.java', 'order-service/.../client/RiskClient.java'] },
  { step: '⑥ 狀態 ACCEPTED/REJECTED', files: ['order-service/.../application/TradingService.java', 'order-service/.../domain/OrderStatus.java'] },
  { step: '⑦ Account（可選）', files: ['order-service/.../client/AccountClient.java', 'account-service/.../application/AccountQueryService.java'] },
  { step: '⑧ Job 逾時（可選）', files: ['job-service/.../JobServiceApplication.java', 'order-service/.../api/InternalJobController.java'] }
];

/** 訂單狀態機 ↔ 程式 */
export const STATE_FILE_REFS = [
  { state: 'PENDING → ACCEPTED/REJECTED', files: ['order-service/.../application/TradingService.java', 'order-service/.../client/RiskClient.java'] },
  { state: 'PENDING → CANCELLED', files: ['order-service/.../application/TradingService.java', 'job-service/.../（逾時 Job）'] }
];

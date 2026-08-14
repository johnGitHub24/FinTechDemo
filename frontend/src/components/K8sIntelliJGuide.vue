<template>
  <section id="k8s-intellij" class="card k8s-intellij-guide">
    <h2>{{ heading }}</h2>
    <p class="story-meta">
      <strong>Services 裡 Docker 能跑 ≠ K8s 已啟動。</strong>
      Compose 起的是 redis／Kafka 容器；微服務在 K8s 裡要另外 <strong>kind + kubectl apply</strong>。
    </p>

    <h3 class="observe-h3">① IntelliJ Services 各自能做什麼</h3>
    <table>
      <thead>
        <tr><th>Services 裡看到的</th><th>能做什麼</th><th>是不是 K8s</th></tr>
      </thead>
      <tbody>
        <tr>
          <td><strong>Docker Compose</strong>（redis…）</td>
          <td>起 <code>docker-compose.yml</code> 容器（:6379 等）</td>
          <td>否</td>
        </tr>
        <tr>
          <td><strong>Gradle → bootRun</strong></td>
          <td>本機 JVM 跑 Order／Risk…（:8081 等）</td>
          <td>否</td>
        </tr>
        <tr>
          <td><strong>Kubernetes</strong>（需裝插件）</td>
          <td>連<strong>已有</strong>叢集：看 Pod、log、port-forward</td>
          <td>是，但<strong>不會自動建 kind</strong></td>
        </tr>
      </tbody>
    </table>
    <p class="muted small">
      日常教學用 <strong>bootRun + Compose</strong> 即可，不必開 K8s。
      RAM 不足時 Demo 可先停 kind（與 bootRun 搶記憶體）。
    </p>

    <h3 class="observe-h3">② Docker Desktop ↔ K8s 三層（運作模式）</h3>
    <p class="muted small">
      四個映像 = 四個 Spring Boot <code>app.jar</code> 裝進不同盒子。
      <strong>Desktop 有 Images ≠ Pod Running</strong> — 還要 kind load + CPU 架構一致 + kubectl apply。
    </p>
    <div ref="elDockerK8s" class="mermaid-wrap" role="img" aria-label="Docker 映像到 K8s Pod 三層流程"></div>

    <table>
      <thead>
        <tr><th>Docker Desktop 映像</th><th>對應 K8s</th><th>做什麼</th></tr>
      </thead>
      <tbody>
        <tr>
          <td><code>fintech-demo/gateway:local</code></td>
          <td>Deployment <code>gateway</code></td>
          <td>API 入口 :8080</td>
        </tr>
        <tr>
          <td><code>fintech-demo/order-service:local</code></td>
          <td>Deployment <code>order-service</code></td>
          <td>下單 :8081</td>
        </tr>
        <tr>
          <td><code>fintech-demo/risk-service:local</code></td>
          <td>Deployment <code>risk-service</code></td>
          <td>風控 :8082</td>
        </tr>
        <tr>
          <td><code>fintech-demo/account-service:local</code></td>
          <td>Deployment <code>account-service</code></td>
          <td>帳戶 :8084</td>
        </tr>
      </tbody>
    </table>

    <h3 class="observe-h3">③ IntelliJ / Docker Desktop 在哪看什麼</h3>
    <table>
      <thead>
        <tr><th>在哪看</th><th>看什麼（對應 Layer）</th></tr>
      </thead>
      <tbody>
        <tr>
          <td><strong>Docker Desktop → Images</strong></td>
          <td>4 個 <code>fintech-demo/*:local</code>（<strong>Layer 1</strong>）</td>
        </tr>
        <tr>
          <td><strong>Docker Desktop → Containers</strong></td>
          <td>主要是 <code>trading-local-control-plane</code>（<strong>Layer 2</strong> kind 節點）</td>
        </tr>
        <tr>
          <td><strong>kubectl get pods</strong> 或 <strong>IntelliJ → Kubernetes</strong></td>
          <td>4 個 Pod <code>Running 1/1</code>（<strong>Layer 3</strong>）</td>
        </tr>
        <tr>
          <td><strong>IntelliJ context</strong></td>
          <td><code>kind-trading-local</code> · namespace <code>fintech-demo</code></td>
        </tr>
      </tbody>
    </table>
    <p class="muted small">
      導覽腳本：<code>.\demo\k8s-walkthrough.ps1</code>（檢查三層）· 平台設定 <code>demo/platform-run.properties</code>（<code>DOCKER_BUILD_PLATFORM=auto</code>）。
    </p>

    <h3 class="observe-h3">④ kind ≠ Docker Desktop「Enable Kubernetes」</h3>
    <table>
      <thead>
        <tr><th>項目</th><th>本專案 FinTechDemo</th><th>Docker Desktop 內建 K8s</th></tr>
      </thead>
      <tbody>
        <tr>
          <td>叢集</td>
          <td><code>kind</code> · 名稱 <code>trading-local</code></td>
          <td>Desktop 自帶單節點</td>
        </tr>
        <tr>
          <td>kubectl context</td>
          <td><code>kind-trading-local</code></td>
          <td><code>docker-desktop</code></td>
        </tr>
        <tr>
          <td>FinTechDemo Pod</td>
          <td><code>deploy/k8s/overlays/dev</code> apply 到這裡</td>
          <td>預設<strong>沒有</strong>本專案 manifest</td>
        </tr>
      </tbody>
    </table>
    <p class="muted small">驗證前先看：<code>kubectl config current-context</code> 應為 <code>kind-trading-local</code>。</p>

    <h3 class="observe-h3">⑤ 三種層級（由淺到深）</h3>
    <table>
      <thead>
        <tr><th>層級</th><th>做法</th><th>需要活叢集？</th></tr>
      </thead>
      <tbody>
        <tr>
          <td><strong>L1</strong> 驗 YAML</td>
          <td><code>.\demo\check-k8s.ps1</code>（只 kustomize）</td>
          <td>否</td>
        </tr>
        <tr>
          <td><strong>L2</strong> 日常 Demo</td>
          <td>Gradle <code>bootRun</code> + Compose redis + Vite</td>
          <td>否</td>
        </tr>
        <tr>
          <td><strong>L3</strong> 真跑 Pod</td>
          <td>kind 起叢集 → build/load 映像 → <code>kubectl apply -k</code></td>
          <td>是</td>
        </tr>
      </tbody>
    </table>

    <h3 class="observe-h3">⑥ L1：只驗證 manifest（PowerShell · 專案根）</h3>
    <div class="observe-cmd-row">
      <code class="observe-cmd">.\demo\check-k8s.ps1</code>
      <button type="button" class="secondary sm" @click="copyText('.\demo\check-k8s.ps1')">複製</button>
      <span class="muted small">不連 API；CI／日常綠燈常用</span>
    </div>

    <h3 class="observe-h3">⑦ L3：在 K8s 跑四個微服務（PowerShell）</h3>
    <p class="muted small">先 <strong>Docker Desktop Ready</strong>。建議一鍵：<code>.\demo\start-k8s-demo.ps1</code>。</p>
    <div v-for="row in k8sDeployCmds" :key="row.cmd" class="observe-cmd-row">
      <code class="observe-cmd">{{ row.cmd }}</code>
      <button type="button" class="secondary sm" @click="copyText(row.cmd)">複製</button>
      <span class="muted small">{{ row.hint }}</span>
    </div>

    <h3 class="observe-h3">⑧ 部署完：驗證 + 打 API</h3>
    <div v-for="row in k8sVerifyCmds" :key="row.cmd" class="observe-cmd-row">
      <code class="observe-cmd">{{ row.cmd }}</code>
      <button type="button" class="secondary sm" @click="copyText(row.cmd)">複製</button>
      <span class="muted small">{{ row.hint }}</span>
    </div>

    <h3 class="observe-h3">⑨ IntelliJ 連上叢集（Pod 已 Running 之後）</h3>
    <ol class="observe-steps">
      <li>安裝 <strong>Kubernetes</strong> 插件</li>
      <li><strong>Settings → Kubernetes</strong> → context 選 <code>kind-trading-local</code></li>
      <li>Services 出現叢集樹 → namespace <code>fintech-demo</code> → 看 Pod／log／port-forward</li>
    </ol>
    <p class="muted small">這是<strong>管理已部署叢集</strong>，不能取代上面的 kind + apply。</p>

    <aside class="story-note">
      <strong>NOTE · 常見誤解</strong>
      <ul>
        <li>Desktop Images 有 4 個映像，但 Pod <code>ImagePullBackOff</code> → 常見 amd64 映像 + arm64 kind；跑 <code>.\demo\k8s-walkthrough.ps1 -Fix</code></li>
        <li><code>check-k8s.ps1</code> OK 但 <code>kubectl get nodes</code> 掛 → 叢集沒活或 context 錯</li>
        <li><code>connection refused</code> → 先 <code>docker desktop start</code>，再 recreate kind</li>
        <li>詳細故障排除：<code>docs/deploy/k8s-tips.html</code></li>
      </ul>
    </aside>
    <p v-if="copyMsg" class="observe-copy-msg" role="status">{{ copyMsg }}</p>
  </section>
</template>

<script setup>
/**
 * 【職責】IntelliJ Services vs K8s 對照＋Docker↔K8s 三層 Mermaid＋本機 kind 部署指令（藍圖 #k8s-intellij）。
 * 【技巧】與 DockerRedisGuide 同型：表格 + Mermaid + 可複製 PowerShell。
 * 【概念】Compose／bootRun＝日常 Demo；K8s＝S5 進階，需 kind-trading-local。
 */
import { nextTick, onMounted, ref } from 'vue';
import { DIAGRAM_DOCKER_K8S } from '../blueprint/diagrams';

defineProps({
  heading: { type: String, default: 'IntelliJ Services vs K8s（本機 Demo）' }
});

const copyMsg = ref('');
const elDockerK8s = ref(null);

const kubeConfigHint = '.\\demo\\.tools\\kubeconfig-kind-trading-local';

const k8sDeployCmds = [
  {
    cmd: '.\\demo\\k8s-walkthrough.ps1',
    hint: '檢查 Layer 1/2/3 對照（Images · kind · pods）'
  },
  {
    cmd: '.\\demo\\start-k8s-demo.ps1',
    hint: '一鍵 K8s Demo（kind + build + load + apply + 等 Pod）'
  },
  {
    cmd: '.\\demo\\start-k8s-demo.ps1 -RecreateCluster',
    hint: 'kubeconfig/API 壞了時重建 kind'
  },
  {
    cmd: `$env:KUBECONFIG = "${kubeConfigHint}"`,
    hint: '新 PowerShell 視窗先設（再 kubectl）'
  },
  {
    cmd: 'kubectl config use-context kind-trading-local',
    hint: '切到本專案用的 context'
  },
  {
    cmd: 'kubectl get --raw=/readyz',
    hint: '應回 ok'
  },
  {
    cmd: 'kubectl apply -k deploy/k8s/overlays/dev',
    hint: '在 FinTechDemo 根目錄（映像已 load 後）'
  },
  {
    cmd: 'kubectl -n fintech-demo get pods',
    hint: '四 Deployment 應 Running 1/1'
  }
];

const k8sVerifyCmds = [
  { cmd: 'kubectl get nodes', hint: 'STATUS 應 Ready' },
  { cmd: 'kubectl get all -n fintech-demo', hint: '四服務 svc + pod' },
  {
    cmd: 'kubectl -n fintech-demo port-forward svc/gateway 18080:8080',
    hint: '另開終端；瀏覽器 http://localhost:18080/actuator/health'
  }
];

async function copyText(text) {
  try {
    await navigator.clipboard.writeText(text);
    copyMsg.value = '已複製';
  } catch {
    copyMsg.value = '複製失敗，請手動選取';
  }
  window.setTimeout(() => { copyMsg.value = ''; }, 2000);
}

async function renderDockerK8sDiagram() {
  if (!elDockerK8s.value) return;
  const mermaid = (await import('mermaid')).default;
  mermaid.initialize({
    startOnLoad: false,
    theme: 'neutral',
    securityLevel: 'loose',
    flowchart: { htmlLabels: true, curve: 'basis' }
  });
  const id = `k8s-docker-${Date.now()}`;
  const { svg } = await mermaid.render(id, DIAGRAM_DOCKER_K8S.trim());
  elDockerK8s.value.innerHTML = svg;
}

onMounted(async () => {
  await nextTick();
  await renderDockerK8sDiagram();
});
</script>

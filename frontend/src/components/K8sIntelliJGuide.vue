<template>
  <section id="k8s-intellij" class="card k8s-intellij-guide">
    <h2>{{ heading }}</h2>
    <p class="story-meta k8s-doc-link">
      <a :href="docsK8sCompleteGuide" target="_blank" rel="noopener"><strong>K8s 完整教學（單頁 HTML）</strong></a>
      · 頂欄「K8s 教學」同連結 · 需 <code>.\docs\tools\serve-docs.ps1</code>（:5500）
    </p>
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

    <h3 class="observe-h3">②b 流程圖 ↔ 檔案對照（專案根相對路徑）</h3>
    <p class="muted small">在 IntelliJ／Cursor 用 <strong>Ctrl+Shift+N</strong>（或搜尋檔名）開下列檔案，與上方 Mermaid 節點對照。</p>
    <table class="file-ref-table">
      <thead>
        <tr><th>圖上層級</th><th>YAML／設定</th><th>腳本</th><th>程式／建置</th></tr>
      </thead>
      <tbody>
        <tr v-for="row in k8sPipelineRefs" :key="row.layer">
          <td><strong>{{ row.layer }}</strong><br><span class="muted small">{{ row.diagram }}</span></td>
          <td><code v-if="row.yaml">{{ row.yaml }}</code><span v-else class="muted">—</span><br v-if="row.config"><code v-if="row.config" class="file-ref-sub">{{ row.config }}</code></td>
          <td><code v-if="row.script">{{ row.script }}</code><span v-else class="muted">—</span></td>
          <td><code v-if="row.code">{{ row.code }}</code><span v-else class="muted">—</span></td>
        </tr>
      </tbody>
    </table>

    <table class="file-ref-table">
      <thead>
        <tr><th>映像</th><th>Deployment YAML</th><th>Service YAML</th><th>程式入口</th></tr>
      </thead>
      <tbody>
        <tr v-for="row in k8sServiceRefs" :key="row.image">
          <td><code>{{ row.image }}</code></td>
          <td><code>{{ row.deployment }}</code></td>
          <td><code>{{ row.service }}</code></td>
          <td><code>{{ row.app }}</code></td>
        </tr>
      </tbody>
    </table>

    <h3 class="observe-h3">③ Docker Desktop 一句話對照（Builds → Pod）</h3>
    <table>
      <thead>
        <tr><th>在哪看</th><th>一句話</th></tr>
      </thead>
      <tbody>
        <tr>
          <td><strong>Builds</strong>（<code>Dockerfile.k8s-local</code>）</td>
          <td>我建過映像</td>
        </tr>
        <tr>
          <td><strong>Images</strong>（<code>fintech-demo/*:local</code>）</td>
          <td>映像建好了，在等被 K8s 用</td>
        </tr>
        <tr>
          <td><strong>Containers</strong>（<code>trading-local-…</code>）</td>
          <td>kind 節點在跑</td>
        </tr>
        <tr>
          <td><strong><code>kubectl get pods</code></strong></td>
          <td>4 個微服務真的在 K8s 裡跑</td>
        </tr>
      </tbody>
    </table>
    <p class="muted small">
      <strong>Builds ≠ 服務已啟動</strong> — 只是 <code>docker build -f Dockerfile.k8s-local</code> 的建置紀錄；Pod 是否 Running 看最後一行。
    </p>

    <table>
      <thead>
        <tr><th>Docker Desktop 映像</th><th>Deployment</th><th>埠</th><th>YAML</th></tr>
      </thead>
      <tbody>
        <tr v-for="row in k8sServiceRefs" :key="'map-' + row.image">
          <td><code>{{ row.image }}</code></td>
          <td><code>{{ row.deployName }}</code></td>
          <td>{{ row.port }}</td>
          <td><code>{{ row.deployment }}</code></td>
        </tr>
      </tbody>
    </table>

    <h3 class="observe-h3">②c 核心原因：兩套 K8s「大樓」（Panel 找不到 fintech-demo）</h3>
    <p class="muted small">
      <strong>walkthrough 全 OK 但 Panel 沒有 <code>fintech-demo</code> = 正常</strong>，不是少設 namespace，是連到<strong>不同叢集</strong>。
    </p>
    <div ref="elTwoK8s" class="mermaid-wrap" role="img" aria-label="兩套 K8s 大樓對照"></div>

    <table class="file-ref-table">
      <thead>
        <tr><th></th><th>Docker Desktop 內建 K8s（A 棟）</th><th>FinTechDemo kind（B 棟）</th></tr>
      </thead>
      <tbody>
        <tr v-for="row in twoK8sClusterCompare" :key="row.label">
          <td><strong>{{ row.label }}</strong></td>
          <td><code v-if="row.desktop">{{ row.desktop }}</code><span v-else>{{ row.desktopText }}</span></td>
          <td><code v-if="row.kind">{{ row.kind }}</code><span v-else>{{ row.kindText }}</span></td>
        </tr>
      </tbody>
    </table>

    <table class="file-ref-table">
      <thead>
        <tr><th>Panel 看到的 namespace</th><th>實際連到</th></tr>
      </thead>
      <tbody>
        <tr>
          <td><code>kube-public</code>、<code>kube-system</code>、<code>default</code>…</td>
          <td><strong>A 棟</strong> Desktop 內建（空叢集）</td>
        </tr>
        <tr>
          <td><strong>沒有</strong> <code>fintech-demo</code></td>
          <td>4 Pod 在 <strong>B 棟</strong> kind；<code>kubectl apply</code> 建在 B，不是 Settings 開關</td>
        </tr>
      </tbody>
    </table>

    <h4 class="observe-h3">Settings 為什麼也沒有 fintech-demo？</h4>
    <table class="file-ref-table">
      <thead>
        <tr><th>Settings → Kubernetes 只管</th><th>不在 Settings（在 repo）</th></tr>
      </thead>
      <tbody>
        <tr>
          <td>Enable 內建 K8s、Reset、資源</td>
          <td>namespace <code>fintech-demo</code>、Deployment、<code>deploy/k8s/</code></td>
        </tr>
        <tr>
          <td>—</td>
          <td>一鍵 <code>demo/start-k8s-demo.ps1</code></td>
        </tr>
      </tbody>
    </table>

    <h4 class="observe-h3">Panel 各分頁 vs Kubernetes Panel</h4>
    <table class="file-ref-table">
      <thead>
        <tr><th>Docker Desktop 分頁</th><th>FinTechDemo</th></tr>
      </thead>
      <tbody>
        <tr v-for="row in dockerDesktopTabCompare" :key="row.tab">
          <td><strong>{{ row.tab }}</strong></td>
          <td>{{ row.relation }}</td>
        </tr>
      </tbody>
    </table>

    <aside class="story-note">
      <strong>NOTE · 一句話</strong>
      <p><strong>Kubernetes Panel</strong> = A 棟接待處 · <strong>fintech-demo</strong> = B 棟 kind · <strong>kubectl / walkthrough</strong> = 正確驗收入口。</p>
      <p class="muted small">全文：<code>docs/guides/docker-desktop-k8s.md</code> · Panel 若有 <code>kind-trading-local</code> 可試選 → namespace <code>fintech-demo</code>；否則用 kubectl。</p>
    </aside>

    <h3 class="observe-h3">④ IntelliJ / Docker Desktop 在哪看什麼（細部）</h3>
    <table>
      <thead>
        <tr><th>在哪看</th><th>看什麼（對應 Layer）</th></tr>
      </thead>
      <tbody>
        <tr>
          <td><strong>Docker Desktop → Builds</strong></td>
          <td><code>Dockerfile.k8s-local</code> 建置紀錄（<strong>不是</strong> Pod 在跑）</td>
        </tr>
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

    <h3 class="observe-h3">⑤ kind ≠ Docker Desktop「Enable Kubernetes」</h3>
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

    <h3 class="observe-h3">⑥ 三種層級（由淺到深）</h3>
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

    <h3 class="observe-h3">⑦ L1：只驗證 manifest（PowerShell · 專案根）</h3>
    <div class="observe-cmd-row">
      <code class="observe-cmd">.\demo\check-k8s.ps1</code>
      <button type="button" class="secondary sm" @click="copyText('.\demo\check-k8s.ps1')">複製</button>
      <span class="muted small">不連 API；CI／日常綠燈常用</span>
    </div>

    <h3 class="observe-h3">⑧ L3：在 K8s 跑四個微服務（PowerShell）</h3>
    <p class="muted small">先 <strong>Docker Desktop Ready</strong>。建議一鍵：<code>.\demo\start-k8s-demo.ps1</code>。</p>
    <div v-for="row in k8sDeployCmds" :key="row.cmd" class="observe-cmd-row">
      <code class="observe-cmd">{{ row.cmd }}</code>
      <button type="button" class="secondary sm" @click="copyText(row.cmd)">複製</button>
      <span class="muted small">{{ row.hint }}</span>
    </div>

    <h3 class="observe-h3">⑨ 部署完：驗證 + 打 API</h3>
    <div v-for="row in k8sVerifyCmds" :key="row.cmd" class="observe-cmd-row">
      <code class="observe-cmd">{{ row.cmd }}</code>
      <button type="button" class="secondary sm" @click="copyText(row.cmd)">複製</button>
      <span class="muted small">{{ row.hint }}</span>
    </div>

    <h3 class="observe-h3">⑩ IntelliJ 連上叢集（Pod 已 Running 之後）</h3>
    <p class="muted small">
      靜態書櫃：<code>docs/guides/intellij-k8s.md</code>（:5500）· 插件需 <strong>Kubernetes</strong>，不是 Docker。
    </p>

    <table class="file-ref-table">
      <thead>
        <tr><th>位置</th><th>能不能選 context／看 Pod</th></tr>
      </thead>
      <tbody>
        <tr v-for="row in intellijPluginCompare" :key="row.where">
          <td><strong>{{ row.where }}</strong></td>
          <td>{{ row.can }}</td>
        </tr>
      </tbody>
    </table>

    <h4 class="observe-h3">步驟 1 · 確認插件</h4>
    <p class="muted small"><strong>File → Settings → Plugins</strong> → 搜尋 <strong>Kubernetes</strong> → Installed 且 Enabled。</p>

    <h4 class="observe-h3">步驟 2 · 設 kubeconfig（最重要）</h4>
    <p class="muted small"><strong>Settings → Build, Execution, Deployment → Kubernetes</strong> → Kubeconfig 按 <strong>+</strong>：</p>
    <div class="observe-cmd-row">
      <code class="observe-cmd">{{ kubeConfigAbsHint }}</code>
      <button type="button" class="secondary sm" @click="copyText(kubeConfigAbsHint)">複製</button>
      <span class="muted small">可保留 C:\Users\…\.kube\config · Apply → OK</span>
    </div>

    <h4 class="observe-h3">步驟 3 · Services 加 Kubernetes</h4>
    <ol class="observe-steps">
      <li><strong>View → Tool Windows → Services</strong></li>
      <li>左上角 <strong>+</strong> → <strong>Kubernetes</strong>（不是 Docker Compose）</li>
      <li>空白時右鍵 → <strong>Configure Kubernetes…</strong> → 確認 kubeconfig 含上一步路徑</li>
      <li>展開 <code>kind-trading-local</code> → <code>fintech-demo</code> → 四 Deployment</li>
      <li><code>fintech-demo</code> 右鍵 → <strong>Set as Current Namespace</strong></li>
    </ol>
    <pre class="intellij-tree muted small" aria-label="Services 樹狀預期">Kubernetes
 └─ kind-trading-local
     └─ fintech-demo
         ├─ gateway
         ├─ order-service
         ├─ risk-service
         └─ account-service</pre>

    <h4 class="observe-h3">步驟 4 · 仍只有 docker-desktop</h4>
    <ol class="observe-steps">
      <li><strong>Settings → Kubernetes</strong> → Context 改 <code>kind-trading-local</code></li>
      <li>Services → Kubernetes 旁按 <strong>Refresh</strong></li>
    </ol>

    <h4 class="observe-h3">步驟 5 · 終端機自測</h4>
    <div v-for="row in intellijVerifyCmds" :key="row.cmd" class="observe-cmd-row">
      <code class="observe-cmd">{{ row.cmd }}</code>
      <button type="button" class="secondary sm" @click="copyText(row.cmd)">複製</button>
      <span class="muted small">{{ row.hint }}</span>
    </div>

    <p class="muted small">這是<strong>管理已部署叢集</strong>，不能取代 kind + apply。Docker Desktop Kubernetes Panel 常看不到 <code>fintech-demo</code>，屬 UI 連錯叢集，以 kubectl 為準。</p>

    <aside class="story-note">
      <strong>NOTE · 常見誤解</strong>
      <ul>
        <li>Desktop Images 有 4 個映像，但 Pod <code>ImagePullBackOff</code> → 常見 amd64 映像 + arm64 kind；跑 <code>.\demo\k8s-walkthrough.ps1 -Fix</code></li>
        <li><code>check-k8s.ps1</code> OK 但 <code>kubectl get nodes</code> 掛 → 叢集沒活或 context 錯</li>
        <li><code>connection refused</code> → 先 <code>docker desktop start</code>，再 recreate kind</li>
        <li>完整教學：<a :href="docsK8sCompleteGuide" target="_blank" rel="noopener">k8s-complete-guide.html</a> · 故障：<code>docs/deploy/k8s-tips.html</code></li>
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
import { DIAGRAM_DOCKER_K8S, DIAGRAM_TWO_K8S_BUILDINGS } from '../blueprint/diagrams';
import { K8S_PIPELINE_REFS, K8S_SERVICE_REFS } from '../blueprint/fileRefs';
import { demoLinks } from '../config/demoLinks';

const docsK8sCompleteGuide = demoLinks.docsK8sCompleteGuide;

defineProps({
  heading: { type: String, default: 'IntelliJ Services vs K8s（本機 Demo）' }
});

const copyMsg = ref('');
const elDockerK8s = ref(null);
const elTwoK8s = ref(null);
const k8sPipelineRefs = K8S_PIPELINE_REFS;
const k8sServiceRefs = K8S_SERVICE_REFS;

const twoK8sClusterCompare = [
  { label: 'context', desktop: 'docker-desktop', kind: 'kind-trading-local' },
  { label: '節點', desktop: 'desktop-control-plane', kind: 'trading-local-control-plane' },
  { label: 'namespace', desktopText: 'default、kube-system…', kind: 'fintech-demo ← 4 Pod' },
  { label: '誰在用', desktopText: 'Kubernetes Panel 預設', kindText: 'kubectl / walkthrough' }
];

const dockerDesktopTabCompare = [
  { tab: 'Images', relation: '✓ 4× fintech-demo/*:local' },
  { tab: 'Containers', relation: '✓ trading-local-control-plane（kind 節點）' },
  { tab: 'Builds', relation: '✓ Dockerfile.k8s-local 建置紀錄' },
  { tab: 'Kubernetes Panel', relation: '✗ 常連 A 棟，看不到 fintech-demo' }
];

const kubeConfigHint = '.\\demo\\.tools\\kubeconfig-kind-trading-local';
const kubeConfigAbsHint = 'D:\\SouceDemo\\RemoteSpringBoot\\FinTechDemo\\demo\\.tools\\kubeconfig-kind-trading-local';

const intellijPluginCompare = [
  { where: 'Services → Docker', can: '✗ 只有 Images／Containers，沒有 context' },
  { where: 'Settings → Kubernetes', can: '✓ 設 kubeconfig、選 kind-trading-local' },
  { where: 'Services → Kubernetes', can: '✓ 看 fintech-demo、Pod、log' },
  { where: 'Docker Desktop Kubernetes Panel', can: '✗ 常連 docker-desktop，看不到 fintech-demo' }
];

const intellijVerifyCmds = [
  { cmd: 'cd D:\\SouceDemo\\RemoteSpringBoot\\FinTechDemo', hint: '專案根' },
  { cmd: '$env:KUBECONFIG = ".\\demo\\.tools\\kubeconfig-kind-trading-local"', hint: '新 PowerShell 先設' },
  { cmd: 'kubectl config use-context kind-trading-local', hint: '切 context' },
  { cmd: 'kubectl -n fintech-demo get pods', hint: '四行 Running 1/1 = 叢集 OK' }
];

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

async function renderMermaidDiagram(el, diagram, prefix) {
  if (!el) return;
  const mermaid = (await import('mermaid')).default;
  mermaid.initialize({
    startOnLoad: false,
    theme: 'neutral',
    securityLevel: 'loose',
    flowchart: { htmlLabels: true, curve: 'basis' }
  });
  const id = `${prefix}-${Date.now()}`;
  const { svg } = await mermaid.render(id, diagram.trim());
  el.innerHTML = svg;
}

async function renderDockerK8sDiagram() {
  await renderMermaidDiagram(elDockerK8s.value, DIAGRAM_DOCKER_K8S, 'k8s-docker');
}

async function renderTwoK8sDiagram() {
  await renderMermaidDiagram(elTwoK8s.value, DIAGRAM_TWO_K8S_BUILDINGS, 'k8s-two');
}

onMounted(async () => {
  await nextTick();
  await renderDockerK8sDiagram();
  await renderTwoK8sDiagram();
});
</script>

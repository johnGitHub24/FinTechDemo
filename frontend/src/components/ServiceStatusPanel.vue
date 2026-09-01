<template>
  <div class="login-topo" aria-live="polite">
    <div class="login-topo-head">
      <div class="login-topo-title">
        <strong>服務狀態</strong>
        <span class="mode-badge" :class="k8sMode ? 'k8s' : 'local'">
          {{ k8sMode ? 'K8s 模式' : '本機 Demo' }}
        </span>
      </div>
      <div class="login-topo-actions">
        <button
          type="button"
          class="ensure-up-btn"
          :disabled="ensuring"
          @click="onEnsure"
          title="複製一鍵啟動指令並開始自動刷新"
        >{{ ensuring ? '等待 UP…' : '一鍵確保 UP' }}</button>
        <button class="secondary sm" type="button" @click="refresh" :disabled="loading">刷新</button>
      </div>
    </div>

    <p v-if="k8sMode" class="mode-lead muted small">
      K8s 只起 <strong>4 個 Pod</strong>（Gateway／Order／Risk／Account），無 Job。
      本機應亮 <code>:{{ k8sPfPort }}</code>（port-forward）＋叢集內 Order／Risk；<strong>勿看</strong>本機 <code>:8080</code>、<code>:8083</code>。
    </p>
    <p v-else class="mode-lead muted small">
      本機五服務應在 <code>:8080</code>～<code>:8084</code> 全亮；與 <code>開啟K8sDemo.cmd</code> 擇一。
    </p>

    <p v-if="ensureHint" class="warn-banner">{{ ensureHint }}</p>
    <p v-if="loading && !ensuring" class="muted small">探測中…</p>

    <template v-if="k8sMode">
      <p class="topo-section-title">本機入口（K8s 要看這些埠）</p>
      <ul class="topo-list">
        <li
          v-for="s in k8sView.localRows"
          :key="s.id"
          :class="rowClass(s)"
          :title="s.hint"
        >
          <span class="dot" />
          <span>{{ s.label }} :{{ s.port }}</span>
          <span v-if="s.required" class="req-tag">要亮</span>
          <span class="badge">{{ rowBadge(s) }}</span>
        </li>
      </ul>

      <p class="topo-section-title">叢集內 Pod（kind）</p>
      <ul class="topo-list">
        <li
          v-for="s in k8sView.clusterRows"
          :key="s.id"
          :class="rowClass(s)"
          :title="s.hint"
        >
          <span class="dot" />
          <span>{{ s.label }} :{{ s.port }}</span>
          <span v-if="s.required" class="req-tag">要亮</span>
          <span class="badge">{{ rowBadge(s) }}</span>
        </li>
      </ul>

      <p class="topo-section-title muted">可忽略（K8s 模式不會亮，屬正常）</p>
      <ul class="topo-list topo-list-ignored">
        <li
          v-for="s in k8sView.ignoredRows"
          :key="s.id"
          class="ignored"
          :title="s.hint"
        >
          <span class="dot" />
          <span>{{ s.label }} :{{ s.port }}</span>
          <span class="badge">忽略</span>
        </li>
      </ul>

      <p class="muted small topo-summary">
        必要項 {{ k8sSummary.up }}/{{ k8sSummary.total }} 就緒
        · 驗證 Gateway：<code>curl http://127.0.0.1:{{ k8sPfPort }}/actuator/health</code>
      </p>
    </template>

    <template v-else>
      <p class="topo-section-title">本機 :8080～:8084（皆應 UP）</p>
      <ul class="topo-list">
        <li
          v-for="s in localRows"
          :key="s.id"
          :class="rowClass(s)"
        >
          <span class="dot" />
          <span>{{ s.label }} :{{ s.port }}</span>
          <span v-if="s.id === 'order' || s.id === 'risk'" class="req-tag">要亮</span>
          <span class="badge">{{ rowBadge(s) }}</span>
        </li>
      </ul>

      <p class="muted small topo-summary">
        {{ localSummary.up }}/{{ localSummary.total }} UP
        · <strong>Demo 最短</strong>：Order＋Risk 即可登入成交
        · 全綠需足夠 RAM：雙擊 <code>開啟Demo.cmd</code>（會先停 kind）
      </p>
    </template>

    <p v-if="demoReady" class="ok-banner">
      <template v-if="k8sMode">
        K8s 展演可開始：Gateway :{{ k8sPfPort }} ＋ Order ＋ Risk 已 UP → 可登入下單。
      </template>
      <template v-else>
        展演可開始：Order＋Risk 已 UP → 登入後下單／成交。
      </template>
    </p>
    <p v-else-if="k8sMode && gatewayPfDown" class="warn-banner">
      Gateway (port-forward) :{{ k8sPfPort }} DOWN → 無法登入。確認 <code>開啟K8sDemo.cmd</code> 的 port-forward 視窗仍開著。
    </p>
    <p v-else-if="riskDown" class="warn-banner">
      Risk(:8082) DOWN → 無法成交。點「一鍵確保 UP」或執行 <code>{{ ensureCmdLabel }}</code>。
    </p>
    <p v-if="orderDown" class="warn-banner">
      Order(:8081) DOWN → 無法登入。點「一鍵確保 UP」或執行 <code>{{ ensureCmdLabel }}</code>。
    </p>
  </div>
</template>

<script setup>
/**
 * 【職責】探測各後端 health，並提供「一鍵確保 UP」引導（複製腳本＋輪詢）。
 * 【技巧】K8s／本機兩套埠位規則見 serviceStatusMode.js；勿用 :8080 判斷 K8s Gateway。
 */
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { fetchTopology } from '../api/client';
import { getEnsureServicesCmd, isK8sFrontendMode } from '../config/demoLinks';
import {
  buildK8sStatusView,
  buildLocalStatusRows,
  getK8sGatewayPfPort,
  summarizeK8sReady,
  summarizeLocalReady
} from '../config/serviceStatusMode';

const k8sMode = computed(() => isK8sFrontendMode());
const k8sPfPort = computed(() => getK8sGatewayPfPort());
const ensureCmdLabel = computed(() => (k8sMode.value ? '開啟K8sDemo.cmd' : '開啟Demo.cmd'));

const localRows = ref(buildLocalStatusRows({}));
const k8sView = ref(buildK8sStatusView({ gatewayPfUp: false, topologyServices: [] }));
const loading = ref(false);
const ensuring = ref(false);
const ensureHint = ref('');
let pollTimer = null;

const k8sSummary = computed(() =>
  summarizeK8sReady([...k8sView.value.localRows, ...k8sView.value.clusterRows])
);
const localSummary = computed(() => summarizeLocalReady(localRows.value));

const orderDown = computed(() => {
  if (k8sMode.value) {
    return !k8sView.value.clusterRows.find((s) => s.id === 'order')?.up;
  }
  return !localRows.value.find((s) => s.id === 'order')?.up;
});
const riskDown = computed(() => {
  if (k8sMode.value) {
    return !k8sView.value.clusterRows.find((s) => s.id === 'risk')?.up;
  }
  return !localRows.value.find((s) => s.id === 'risk')?.up;
});
const gatewayPfDown = computed(() => {
  if (!k8sMode.value) return false;
  return !k8sView.value.localRows.find((s) => s.id === 'gateway-pf')?.up;
});
const demoReady = computed(() => {
  if (k8sMode.value) {
    const pf = k8sView.value.localRows.find((s) => s.id === 'gateway-pf')?.up;
    return Boolean(pf && !orderDown.value && !riskDown.value);
  }
  return localSummary.value.demoReady;
});

function rowClass(s) {
  if (s.na) return 'na';
  return s.up ? 'up' : 'down';
}

function rowBadge(s) {
  if (s.na) return '未部署';
  return s.up ? 'UP' : 'DOWN';
}

async function probeProxy(path) {
  try {
    const res = await fetch(path, { method: 'GET' });
    if (!res.ok) return false;
    const body = await res.json();
    return String(body?.status || '').toUpperCase() === 'UP';
  } catch {
    return false;
  }
}

async function probeGatewayPf() {
  return probeProxy('/proxy/k8s-gateway-pf-health');
}

async function probeViaViteProxy() {
  const results = await Promise.all([
    probeProxy('/proxy/gateway-health'),
    probeProxy('/proxy/order-health'),
    probeProxy('/proxy/risk-health'),
    probeProxy('/proxy/job-health'),
    probeProxy('/proxy/account-health')
  ]);
  const upById = {
    gateway: results[0],
    order: results[1],
    risk: results[2],
    job: results[3],
    account: results[4]
  };
  localRows.value = buildLocalStatusRows(upById);
}

async function refresh() {
  loading.value = true;
  try {
    if (k8sMode.value) {
      const [gatewayPfUp, topology] = await Promise.all([
        probeGatewayPf(),
        fetchTopology().catch(() => null)
      ]);
      k8sView.value = buildK8sStatusView({
        gatewayPfUp,
        topologyServices: topology?.services
      });
      return;
    }

    try {
      const data = await fetchTopology();
      if (data?.services?.length) {
        const upById = Object.fromEntries(data.services.map((s) => [s.id, s.up]));
        localRows.value = buildLocalStatusRows(upById);
        return;
      }
    } catch {
      /* fall through */
    }
    await probeViaViteProxy();
  } finally {
    loading.value = false;
  }
}

function stopPoll() {
  if (pollTimer) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
  ensuring.value = false;
}

function pollSuccess() {
  if (k8sMode.value) {
    return demoReady.value;
  }
  return localSummary.value.allUp;
}

async function onEnsure() {
  const k8s = k8sMode.value;
  const cmd = getEnsureServicesCmd();
  try {
    await navigator.clipboard.writeText(cmd);
    ensureHint.value = k8s
      ? `已複製 開啟K8sDemo.cmd。K8s 看本機 :${k8sPfPort.value}（port-forward）＋叢集 Order／Risk；:8080／:8083 可忽略。`
      : '已複製啟動指令 → 到 PowerShell 貼上 Enter（約 1～3 分鐘）。本頁會自動刷新直到 UP。也可雙擊 開啟Demo.cmd';
  } catch {
    ensureHint.value = `請手動執行：${cmd}`;
  }
  ensuring.value = true;
  stopPoll();
  ensuring.value = true;
  let ticks = 0;
  pollTimer = setInterval(async () => {
    ticks += 1;
    await refresh();
    if (pollSuccess()) {
      ensureHint.value = k8s
        ? `K8s 就緒：Gateway :${k8sPfPort.value} ＋ Order／Risk 已 UP。`
        : '全部 UP，可以登入／成交了。';
      stopPoll();
      return;
    }
    if (!orderDown.value && !riskDown.value && ticks >= 6) {
      if (k8s) {
        const s = k8sSummary.value;
        ensureHint.value = `Order＋Risk 已 UP（必要項 ${s.up}/${s.total}）。若 Gateway :${k8sPfPort.value} 仍 DOWN，檢查 port-forward 視窗。`;
      } else {
        ensureHint.value = `Order＋Risk 已 UP（${localSummary.value.up}/5）。其餘可稍候或再跑一次腳本。`;
      }
    }
    if (ticks >= 36) {
      ensureHint.value = k8s
        ? `逾時。請確認 開啟K8sDemo.cmd、port-forward :${k8sPfPort.value} 視窗仍開、kubectl get pods -n fintech-demo 皆 Running。`
        : '逾時仍有 DOWN。看 logs\\*.err.log 或再執行 開啟Demo.cmd';
      stopPoll();
    }
  }, 5000);
  await refresh();
}

onMounted(() => {
  refresh();
});
onUnmounted(() => {
  stopPoll();
});

defineExpose({ refresh, localRows, k8sView, demoReady });
</script>

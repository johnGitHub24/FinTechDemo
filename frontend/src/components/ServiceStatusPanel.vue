<template>
  <div class="login-topo" aria-live="polite">
    <div class="login-topo-head">
      <strong>服務狀態</strong>
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

    <p v-if="ensureHint" class="warn-banner">{{ ensureHint }}</p>
    <p v-if="loading && !ensuring" class="muted small">探測中…</p>

    <ul class="topo-list">
      <li v-for="s in services" :key="s.id" :class="s.up ? 'up' : 'down'">
        <span class="dot" />
        <span>{{ s.label }} :{{ s.port }}</span>
        <span class="badge">{{ s.up ? 'UP' : 'DOWN' }}</span>
      </li>
    </ul>

    <p class="muted small topo-summary">
      {{ upCount }}/{{ services.length }} UP
      · <strong>Demo 最短</strong>：Order＋Risk＋本頁即可登入成交
      · 全綠需足夠 RAM：雙擊 <code>開啟Demo.cmd</code>（會先停 kind 省記憶體）
    </p>

    <p v-if="demoReady" class="ok-banner">
      展演可開始：Order＋Risk 已 UP → 登入後下單／成交。
    </p>
    <p v-else-if="riskDown" class="warn-banner">
      Risk(:8082) DOWN → 無法成交。點「一鍵確保 UP」或執行 <code>開啟Demo.cmd</code>。
    </p>
    <p v-if="orderDown" class="warn-banner">
      Order(:8081) DOWN → 無法登入。點「一鍵確保 UP」或執行 <code>開啟Demo.cmd</code>。
    </p>
  </div>
</template>

<script setup>
/**
 * 【職責】探測各後端 health，並提供「一鍵確保 UP」引導（複製腳本＋輪詢）。
 * 【技巧】瀏覽器不能直接 bootRun；複製 doctor-demo -Fix，背景輪詢直到全 UP 或逾時。
 * 【概念】燈號 DOWN＝本機行程沒聽埠，不是前端壞掉。
 */
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { fetchTopology } from '../api/client';
import { ENSURE_SERVICES_CMD } from '../config/demoLinks';

const services = ref([
  { id: 'gateway', label: 'Gateway', port: 8080, up: false },
  { id: 'order', label: 'Order', port: 8081, up: false },
  { id: 'risk', label: 'Risk', port: 8082, up: false },
  { id: 'job', label: 'Job', port: 8083, up: false },
  { id: 'account', label: 'Account', port: 8084, up: false }
]);
const loading = ref(false);
const ensuring = ref(false);
const ensureHint = ref('');
let pollTimer = null;

const upCount = computed(() => services.value.filter((s) => s.up).length);
const riskDown = computed(() => !services.value.find((s) => s.id === 'risk')?.up);
const orderDown = computed(() => !services.value.find((s) => s.id === 'order')?.up);
const demoReady = computed(() => !orderDown.value && !riskDown.value);

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

async function probeViaViteProxy() {
  const results = await Promise.all([
    probeProxy('/proxy/gateway-health'),
    probeProxy('/proxy/order-health'),
    probeProxy('/proxy/risk-health'),
    probeProxy('/proxy/job-health'),
    probeProxy('/proxy/account-health')
  ]);
  services.value = [
    { id: 'gateway', label: 'Gateway', port: 8080, up: results[0] },
    { id: 'order', label: 'Order', port: 8081, up: results[1] },
    { id: 'risk', label: 'Risk', port: 8082, up: results[2] },
    { id: 'job', label: 'Job', port: 8083, up: results[3] },
    { id: 'account', label: 'Account', port: 8084, up: results[4] }
  ];
}

async function refresh() {
  loading.value = true;
  try {
    try {
      const data = await fetchTopology();
      if (data?.services?.length) {
        services.value = data.services;
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

async function onEnsure() {
  const cmd = ENSURE_SERVICES_CMD;
  try {
    await navigator.clipboard.writeText(cmd);
    ensureHint.value =
      '已複製啟動指令 → 到 PowerShell 貼上 Enter（約 1～3 分鐘）。本頁會自動刷新直到 UP。也可雙擊 開啟Demo.cmd';
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
    if (upCount.value >= 5) {
      ensureHint.value = '全部 UP，可以登入／成交了。';
      stopPoll();
      return;
    }
    // Risk+Order 最短成交也提示
    if (!orderDown.value && !riskDown.value && ticks >= 6) {
      ensureHint.value = `Order＋Risk 已 UP（${upCount.value}/5）。其餘可稍候或再跑一次腳本。`;
    }
    if (ticks >= 36) {
      ensureHint.value = '逾時仍有 DOWN。看 logs\\*.err.log 或再執行 開啟Demo.cmd';
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

defineExpose({ refresh, services });
</script>

<template>
  <div class="login-page">
    <div class="card login-card">
      <h1>登入</h1>
      <p class="muted">trader1 / admin · 密碼 password</p>
      <label>帳號</label>
      <input v-model="username" autocomplete="username" />
      <label>密碼</label>
      <input v-model="password" type="password" autocomplete="current-password" @keyup.enter="onSubmit" />
      <button type="button" @click="onSubmit" :disabled="loading">登入</button>
      <p v-if="error" class="error">{{ error }}</p>

      <div class="login-topo" aria-live="polite">
        <div class="login-topo-head">
          <strong>服務狀態</strong>
          <button class="secondary sm" type="button" @click="refreshTopology" :disabled="topoLoading">刷新</button>
        </div>
        <p v-if="topoLoading" class="muted small">探測中…</p>
        <ul class="topo-list">
          <li v-for="s in services" :key="s.id" :class="s.up ? 'up' : 'down'">
            <span class="dot" />
            <span>{{ s.label }} :{{ s.port }}</span>
            <span class="badge">{{ s.up ? 'UP' : 'DOWN' }}</span>
          </li>
        </ul>
        <p v-if="frontendOk === false" class="warn-banner">
          若本頁能開表示 Vite 已起；但請確認右側連結對應的後端已啟動。
        </p>
        <p v-if="riskDown" class="warn-banner">
          Risk(:8082) 未啟動 → 無法「成交」。請在 IntelliJ 再開 <code>RiskServiceApplication</code>。
        </p>
        <p v-if="orderDown" class="warn-banner">
          Order(:8081) 未啟動 → 無法登入。請開 <code>OrderServiceApplication</code>。
        </p>
        <p class="muted small">最短可成交：Risk UP + Order UP + 本頁（Vite）。Console 橫幅 URL 請改點右側快捷。</p>
      </div>

      <p class="login-blueprint-link">
        <router-link to="/blueprint">系統運作藍圖</router-link>
        <span> — 技術架構與運作過程（可不登入）</span>
      </p>
    </div>

    <aside class="card login-demo-panel" aria-label="Demo 快捷入口">
      <h2>Demo 快捷入口</h2>
      <p class="muted small">
        Console 橫幅無法點；這裡可點。一鍵確保服務：
        <code>.\scripts\ensure-demo-links.ps1</code>；
        觀測（免 Docker）：
        <code>.\scripts\start-monitoring-local.ps1</code>。
        Risk Check／Account Me 會開 Demo 頁自動呼叫 API。
      </p>
      <div v-for="group in loginDemoGroups" :key="group.title" class="demo-group">
        <h3>{{ group.title }}</h3>
        <div class="demo-link-row">
          <a
            v-for="item in group.items"
            :key="item.href + item.label"
            class="demo-chip"
            :href="item.href"
            :title="item.needLogin ? '需先登入' : undefined"
            target="_blank"
            rel="noopener noreferrer"
          >{{ item.label }}</a>
        </div>
      </div>
    </aside>
  </div>
</template>

<script setup>
/**
 * 【職責】登入表單 + 服務燈號 + 橫幅對齊的 Demo 快捷連結。
 * 【技巧】優先 /api/demo/topology；失敗則走 Vite /proxy/*-health（免 CORS、免 JWT）。
 * 【概念】診斷「為什麼 Console 功能無法執行」：通常是 Risk／前端未開，或點了未啟動埠。
 */
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { login, fetchTopology } from '../api/client';
import { useAuthStore } from '../stores/auth';
import { loginDemoGroups } from '../config/demoLinks';

const router = useRouter();
const auth = useAuthStore();
const username = ref('trader1');
const password = ref('password');
const error = ref('');
const loading = ref(false);

const services = ref([
  { id: 'gateway', label: 'Gateway', port: 8080, up: false },
  { id: 'order', label: 'Order', port: 8081, up: false },
  { id: 'risk', label: 'Risk', port: 8082, up: false },
  { id: 'job', label: 'Job', port: 8083, up: false },
  { id: 'account', label: 'Account', port: 8084, up: false }
]);
const topoLoading = ref(false);
const frontendOk = ref(true);

const riskDown = computed(() => {
  const risk = services.value.find((s) => s.id === 'risk');
  return risk ? !risk.up : true;
});
const orderDown = computed(() => {
  const order = services.value.find((s) => s.id === 'order');
  return order ? !order.up : true;
});

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

/**
 * 【目的】刷新各後端 UP/DOWN。
 */
async function refreshTopology() {
  topoLoading.value = true;
  try {
    try {
      const data = await fetchTopology();
      if (data?.services?.length) {
        services.value = data.services;
        return;
      }
    } catch {
      /* fall through → Vite proxy */
    }
    await probeViaViteProxy();
  } finally {
    topoLoading.value = false;
  }
}

/**
 * 【目的】送出登入並進入交易前台。
 */
async function onSubmit() {
  error.value = '';
  loading.value = true;
  try {
    const data = await login(username.value.trim(), password.value);
    auth.setSession({ token: data.token, username: data.username, roles: data.roles });
    router.push('/trade');
  } catch (e) {
    error.value = e.response?.data?.error || '登入失敗（請確認 Order :8081 已啟動）';
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  topoLoading.value = true;
  try {
    await refreshTopology();
  } finally {
    topoLoading.value = false;
  }
});
</script>

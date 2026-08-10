<template>
  <div class="story-panel card">
    <div class="row" style="align-items:center;margin-bottom:0.5rem">
      <h3 style="margin:0;flex:2">後端過程（PROCESS FLOW）</h3>
      <button
        class="secondary"
        type="button"
        :class="{ 'is-refreshing': topoRefreshing }"
        :disabled="topoRefreshing"
        :aria-busy="topoRefreshing"
        @click="refreshManual"
      >{{ topoRefreshing ? '刷新中…' : topoDone ? '已更新' : '刷新拓撲' }}</button>
    </div>
    <p v-if="topoHint" class="refresh-hint" :class="topoHintOk ? 'ok' : ''" role="status">{{ topoHint }}</p>
    <p class="story-meta" v-if="story.state.lastTrace">
      動作 <strong>{{ story.state.lastTrace.action }}</strong>
      <span v-if="story.state.lastTrace.orderId"> · 訂單 #{{ story.state.lastTrace.orderId }}</span>
      <span v-if="story.state.lastTrace.orderStatus"> · 狀態 {{ story.state.lastTrace.orderStatus }}</span>
      <span v-if="story.state.lastTrace.viaGateway"> · via Gateway</span>
    </p>
    <p class="story-meta" v-else>尚無操作 trace — 請先下單／成交／取消。</p>

    <ol class="flow-list">
      <li
        v-for="(step, idx) in story.flowSteps.value"
        :key="idx"
        :class="{ fail: !step.ok }"
      >
        <div class="flow-who">{{ step.title }}</div>
        <div class="flow-purpose">{{ step.purpose }}</div>
        <div class="flow-state">{{ step.stateHint }}</div>
      </li>
    </ol>
    <p v-if="!story.flowSteps.value.length" class="story-meta">（等待第一次帶 demoTrace 的 API）</p>

    <h4 class="story-sub">服務儀表板</h4>
    <div class="lamp-row" :class="{ 'is-refreshing': topoRefreshing }">
      <div
        v-for="s in services"
        :key="s.id"
        class="lamp"
        :class="{ up: s.up, down: !s.up }"
        :title="s.url"
      >
        <span class="dot"></span>
        {{ s.label }}:{{ s.port }}
      </div>
    </div>
    <p v-if="story.state.topologyError" class="error">{{ story.state.topologyError }}</p>

    <h4 class="story-sub">部署階梯（敘事）</h4>
    <div class="stage-row">
      <button
        v-for="s in stages"
        :key="s"
        type="button"
        class="stage-chip"
        :class="[
          `stage-${s.toLowerCase()}`,
          { active: displayStage === s, pinned: story.state.pinStage === s }
        ]"
        @click="togglePin(s)"
      >
        {{ s }}
      </button>
      <button class="secondary sm" type="button" @click="story.setPinStage(null)">清除釘住</button>
    </div>
    <p class="story-meta">
      目前顯示：<strong>{{ displayStage }}</strong>
      <span v-if="story.state.pinStage">（已釘住講解，真實拓撲仍是 {{ factStage }}）</span>
      <span v-else>（自動＝跟上方綠燈；點 S1–S3 可暫時釘住講解）</span>
    </p>
    <p class="story-why" v-if="stageWhy">{{ stageWhy }}</p>
    <aside class="story-note">
      <strong>NOTE · 誰決定 S1／S2／S3？</strong>
      <p>
        <strong>決定者＝程式公式</strong>（不是你點選業務結果，也不是訂單狀態）。
        Order 後端 <code>TopologyService.inferStage</code> 探測各服務
        <code>/actuator/health</code>，算出 <code>inferredStage</code>；
        前端每 5 秒拉 <code>GET /api/demo/topology</code> 顯示。
        你點 S1／S2／S3 只是「釘住講解」；清釘住後又聽公式的。
      </p>
      <p><strong>判定公式（取最高符合）</strong></p>
      <ol class="stage-formula">
        <li><strong>S3</strong>＝ Order 綠 <strong>且</strong> Risk 綠 <strong>且</strong>（Gateway 綠 <em>或</em> Account 綠）</li>
        <li><strong>S2</strong>＝ Order 綠 <strong>且</strong> Risk 綠（還沒 Gateway／Account）</li>
        <li><strong>S1</strong>＝ 只有 Order 綠</li>
        <li><strong>S0</strong>＝ Order 也紅</li>
      </ol>
      <p><strong>為什麼要分階？</strong></p>
      <ul>
        <li><strong>S1</strong>：能登入、建 PENDING；成交缺 Risk</li>
        <li><strong>S2</strong>：最短可成交（Feign → Risk）</li>
        <li><strong>S3</strong>：能講統一入口（Gateway）或帳務／Redis（Account）</li>
      </ul>
      <p><code>Job</code> 不進公式。釘住不改真實 health。</p>
    </aside>

    <h4 class="story-sub">訂單狀態機</h4>
    <div class="stage-row order-state-row">
      <span
        v-for="st in orderStates"
        :key="st"
        class="order-state-chip"
        :class="[`st-${st}`, { active: currentOrderStatus === st }]"
      >{{ st }}</span>
    </div>
    <aside class="story-note">
      <strong>NOTE · 這一筆單子到哪？</strong>
      <ul>
        <li><strong>PENDING</strong>：已建立，尚未成交／取消（藍）</li>
        <li><strong>ACCEPTED</strong>：點「成交」且 Risk 通過 → 接受（青）</li>
        <li><strong>REJECTED</strong>：成交時風控拒絕（紅）</li>
        <li><strong>CANCELLED</strong>：使用者取消或排程逾時取消（靛）</li>
      </ul>
      <p>與部署階梯無關：訂單狀態跟<strong>這一筆業務結果</strong>；S 階跟<strong>哪些服務在跑</strong>。目前態會高亮放大。</p>
    </aside>
  </div>
</template>

<script setup>
/**
 * 【職責】展演用後端過程儀表板：PROCESS FLOW／服務燈／S 階／訂單狀態。
 * 【頁面角色】嵌在 Trade／Portal，不另開路由。
 * 【與後端關係】讀 demoTrace（經 API client）與 GET /api/demo/topology。
 */
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useDemoStoryStore } from '../stores/demoStory';
import { inferStage } from '../demo/inferStage.js';

const story = useDemoStoryStore();
const stages = ['S1', 'S2', 'S3'];
const orderStates = ['PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED'];
const topoRefreshing = ref(false);
const topoDone = ref(false);
const topoHint = ref('');
const topoHintOk = ref(false);
let topoDoneTimer = null;

const services = computed(() => story.state.topology?.services || []);
const displayStage = computed(() => story.displayStage.value);
const factStage = computed(() => {
  if (story.state.topology?.inferredStage) return story.state.topology.inferredStage;
  return inferStage(story.state.topology?.services);
});
const stageWhy = computed(() => {
  const list = services.value || [];
  if (!list.length) return '尚未取得拓撲：按「刷新拓撲」或等自動輪詢。';
  const lamp = (id) => {
    const s = list.find((x) => x.id === id);
    if (!s) return `${id}?`;
    return `${s.label}:${s.up ? '綠' : '紅'}`;
  };
  const bits = [lamp('order'), lamp('risk'), lamp('gateway'), lamp('account')].join(' · ');
  if (story.state.pinStage) {
    return `釘住中：顯示 ${story.state.pinStage}；公式事實仍是 ${factStage.value}（${bits}）`;
  }
  return `公式結果 ${factStage.value} ← ${bits}（Job 不計）`;
});
const currentOrderStatus = computed(() => story.state.lastTrace?.orderStatus || '');

let timer = null;

async function refresh() {
  await story.refreshTopology();
}

/**
 * 【目的】手動刷新拓撲燈號，顯示 loading／完成狀態（與背景輪詢分開）。
 */
async function refreshManual() {
  if (topoRefreshing.value) return;
  topoRefreshing.value = true;
  topoDone.value = false;
  topoHint.value = '探測服務中…';
  topoHintOk.value = false;
  try {
    await refresh();
    const list = story.state.topology?.services || [];
    const up = list.filter((s) => s.up).length;
    topoHint.value = `拓撲已更新 · ${up}/${list.length || 0} UP`;
    topoHintOk.value = true;
    topoDone.value = true;
    if (topoDoneTimer) clearTimeout(topoDoneTimer);
    topoDoneTimer = setTimeout(() => {
      topoDone.value = false;
      if (topoHintOk.value) topoHint.value = '';
    }, 1800);
  } catch (e) {
    topoHint.value = e?.message || story.state.topologyError || '拓撲刷新失敗';
    topoHintOk.value = false;
  } finally {
    topoRefreshing.value = false;
  }
}

function togglePin(s) {
  if (story.state.pinStage === s) story.setPinStage(null);
  else story.setPinStage(s);
}

onMounted(async () => {
  await refresh();
  timer = setInterval(refresh, 5000);
});

onUnmounted(() => {
  if (timer) clearInterval(timer);
});
</script>

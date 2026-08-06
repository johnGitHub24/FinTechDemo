<template>
  <div class="story-layout">
    <div>
      <h1>會員後台</h1>
      <div class="card">
        <h3>餘額</h3>
        <p v-if="account">{{ account.cashBalance }} {{ account.currency }}</p>
      </div>
      <div class="card">
        <h3>持倉</h3>
        <table>
          <thead><tr><th>Symbol</th><th>Qty</th><th>Avg</th></tr></thead>
          <tbody>
            <tr v-for="p in positions" :key="p.symbol">
              <td>{{ p.symbol }}</td>
              <td>{{ p.quantity }}</td>
              <td>{{ p.avgPrice }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="card">
        <div class="row" style="align-items:center">
          <h3 style="margin:0;flex:2">交易歷史（分頁）</h3>
          <button class="secondary" type="button" @click="prev" :disabled="page<=0">上一頁</button>
          <button class="secondary" type="button" @click="next" :disabled="page>=totalPages-1">下一頁</button>
        </div>
        <p style="color:#8b9cb3">page {{ page + 1 }} / {{ totalPages }} · total {{ total }}</p>
        <table>
          <thead><tr><th>ID</th><th>Symbol</th><th>Side</th><th>Qty</th><th>Price</th><th>Status</th></tr></thead>
          <tbody>
            <tr v-for="o in orders" :key="o.id">
              <td>{{ o.id }}</td>
              <td>{{ o.symbol }}</td>
              <td>{{ o.side }}</td>
              <td>{{ o.quantity }}</td>
              <td>{{ o.price }}</td>
              <td>{{ o.status }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    <BackendStoryPanel />
  </div>
</template>

<script setup>
/**
 * 【職責】會員後台，彙整帳戶餘額、持倉及分頁交易歷史。
 * 【頁面角色】資產查閱頁；右側嵌 BackendStoryPanel。
 * 【與後端關係】從帳戶、持倉與訂單 API 讀取；故事面板與 Trade 共用 store。
 */
import { computed, onMounted, ref } from 'vue';
import { fetchAccount, fetchOrders, fetchPositions } from '../api/client';
import BackendStoryPanel from '../components/BackendStoryPanel.vue';

const account = ref(null);
const positions = ref([]);
const orders = ref([]);
const page = ref(0);
const size = 5;
const total = ref(0);
const totalPages = computed(() => Math.ceil(total.value / size) || 1);

async function load() {
  account.value = await fetchAccount();
  positions.value = await fetchPositions();
  const data = await fetchOrders({ page: page.value, size });
  orders.value = data.data || [];
  total.value = data.meta?.total || 0;
}

function prev() {
  if (page.value > 0) {
    page.value -= 1;
    load();
  }
}

function next() {
  if (page.value < totalPages.value - 1) {
    page.value += 1;
    load();
  }
}

onMounted(load);
</script>

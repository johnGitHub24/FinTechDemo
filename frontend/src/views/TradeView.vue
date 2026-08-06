<template>
  <div class="story-layout">
    <div>
      <h1>交易前台</h1>
      <div class="card">
        <h3>下單</h3>
        <div class="form-row">
          <div>
            <label>標的</label>
            <select v-model="form.symbol" @change="onSymbol">
              <option v-for="s in symbols" :key="s.symbol" :value="s.symbol">
                {{ s.symbol }} ({{ s.refPrice }})
              </option>
            </select>
          </div>
          <div>
            <label>方向</label>
            <select v-model="form.side">
              <option value="BUY">BUY</option>
              <option value="SELL">SELL</option>
            </select>
          </div>
          <div>
            <label>數量</label>
            <input v-model.number="form.quantity" type="number" min="1" />
          </div>
          <div>
            <label>價格</label>
            <input v-model.number="form.price" type="number" step="0.01" min="0.01" />
          </div>
        </div>
        <button type="button" @click="submit">送出 PENDING</button>
        <p v-if="msg" class="ok">{{ msg }}</p>
        <p v-if="error" class="error">{{ error }}</p>
      </div>

      <div class="card">
        <div class="row" style="align-items:center">
          <h3 style="margin:0;flex:2">進行中／我的訂單</h3>
          <button class="secondary" type="button" @click="load">刷新</button>
        </div>
        <table>
          <thead>
            <tr><th>ID</th><th>Symbol</th><th>Side</th><th>Qty</th><th>Price</th><th>Status</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="o in orders" :key="o.id">
              <td>{{ o.id }}</td>
              <td>{{ o.symbol }}</td>
              <td>{{ o.side }}</td>
              <td>{{ o.quantity }}</td>
              <td>{{ o.price }}</td>
              <td>{{ o.status }}</td>
              <td>
                <button v-if="o.status==='PENDING'" type="button" @click="exec(o.id)">成交</button>
                <button v-if="o.status==='PENDING'" class="danger" type="button" @click="cxl(o.id)">取消</button>
              </td>
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
 * 【職責】交易前台，讓登入使用者建立、執行、取消並查看訂單。
 * 【頁面角色】核心下單操作頁；右側嵌 BackendStoryPanel 展演後端過程。
 * 【與後端關係】透過市場與訂單 API 讀寫；demoTrace 由 client 寫入故事 store。
 */
import { onMounted, reactive, ref } from 'vue';
import { cancelOrder, createOrder, executeOrder, fetchOrders, fetchSymbols } from '../api/client';
import BackendStoryPanel from '../components/BackendStoryPanel.vue';

const symbols = ref([]);
const orders = ref([]);
const msg = ref('');
const error = ref('');
const form = reactive({ symbol: 'AAPL', side: 'BUY', quantity: 1, price: 150 });

async function load() {
  const page = await fetchOrders({ page: 0, size: 20 });
  orders.value = page.data || [];
}

async function loadSymbols() {
  symbols.value = await fetchSymbols();
  onSymbol();
}

function onSymbol() {
  const hit = symbols.value.find((s) => s.symbol === form.symbol);
  if (hit) form.price = Number(hit.refPrice);
}

async function submit() {
  msg.value = '';
  error.value = '';
  try {
    await createOrder({
      clientOrderId: `WEB-${Date.now()}`,
      symbol: form.symbol,
      side: form.side,
      quantity: form.quantity,
      price: form.price
    });
    msg.value = '已建立 PENDING 訂單';
    await load();
  } catch (e) {
    error.value = e.response?.data?.error || '下單失敗';
  }
}

async function exec(id) {
  try {
    await executeOrder(id);
    await load();
  } catch (e) {
    error.value = e.response?.data?.error || '成交失敗';
  }
}

async function cxl(id) {
  try {
    await cancelOrder(id);
    await load();
  } catch (e) {
    error.value = e.response?.data?.error || '取消失敗';
  }
}

onMounted(async () => {
  await loadSymbols();
  await load();
});
</script>

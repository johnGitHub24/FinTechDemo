<template>
  <div class="story-layout">
    <div>
      <h1>交易前台</h1>
      <p v-if="isAdmin" class="warn-banner demo-scope" role="note">
        你是 ADMIN：訂單列表是<strong>全站</strong>（含 trader1）。
        成交會記在<strong>下單者</strong>帳上；admin 本人餘額請到後台看（種子 100000、無持倉）。
      </p>
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
        <div class="pager-row">
          <h3>{{ isAdmin ? '進行中（全站 PENDING）' : '進行中（我的 PENDING）' }}</h3>
          <div class="pager-actions">
            <button class="secondary sm" type="button" @click="prevPending" :disabled="pendingPage<=0">上一頁</button>
            <button class="secondary sm" type="button" @click="nextPending" :disabled="pendingPage>=pendingTotalPages-1">下一頁</button>
            <button
              class="secondary sm"
              type="button"
              :class="{ 'is-refreshing': refreshing }"
              :disabled="refreshing"
              @click="onRefresh"
            >{{ refreshing ? '刷新中…' : refreshDone ? '已更新' : '刷新' }}</button>
          </div>
        </div>
        <p class="muted small">page {{ pendingPage + 1 }} / {{ pendingTotalPages }} · total {{ pendingTotal }}（與後台同 API／每頁 {{ pageSize }}）</p>
        <p v-if="refreshHint" class="refresh-hint" :class="{ ok: refreshDone }" role="status">{{ refreshHint }}</p>
        <p v-if="!pendingOrders.length" class="muted">目前沒有待成交訂單</p>
        <table v-else :class="{ 'table-refreshing': refreshing }">
          <thead>
            <tr>
              <th>擁有者</th>
              <th>ID</th>
              <th>Symbol</th>
              <th>Side</th>
              <th>Qty</th>
              <th>Price</th>
              <th>Status</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="o in pendingOrders"
              :key="o.id"
              :class="{ 'order-other': isAdmin && !isOwnOrder(o, auth.username) }"
            >
              <td>
                <span class="owner-chip" :class="isOwnOrder(o, auth.username) ? 'mine' : 'other'">
                  {{ orderOwnerName(o) }}
                </span>
              </td>
              <td>{{ o.id }}</td>
              <td>{{ o.symbol }}</td>
              <td>{{ o.side }}</td>
              <td>{{ o.quantity }}</td>
              <td>{{ o.price }}</td>
              <td>{{ o.status }}</td>
              <td>
                <button v-if="canAct(o)" type="button" @click="exec(o.id)">成交</button>
                <button v-if="canAct(o)" class="danger" type="button" @click="cxl(o.id)">取消</button>
                <span v-else class="muted">他人訂單</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="card">
        <div class="pager-row">
          <h3>{{ isAdmin ? '全站訂單（含結果）' : '我的訂單（含結果）' }}</h3>
          <div class="pager-actions">
            <button class="secondary sm" type="button" @click="prevAll" :disabled="allPage<=0">上一頁</button>
            <button class="secondary sm" type="button" @click="nextAll" :disabled="allPage>=allTotalPages-1">下一頁</button>
          </div>
        </div>
        <p class="muted small">page {{ allPage + 1 }} / {{ allTotalPages }} · total {{ allTotal }}（與後台交易歷史同一列表）</p>
        <table :class="{ 'table-refreshing': refreshing }">
          <thead>
            <tr>
              <th>擁有者</th>
              <th>ID</th>
              <th>Symbol</th>
              <th>Side</th>
              <th>Qty</th>
              <th>Price</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="o in orders"
              :key="'all-' + o.id"
              :class="{ 'order-other': isAdmin && !isOwnOrder(o, auth.username) }"
            >
              <td>
                <span class="owner-chip" :class="isOwnOrder(o, auth.username) ? 'mine' : 'other'">
                  {{ orderOwnerName(o) }}
                  <template v-if="isOwnOrder(o, auth.username)"> ·本人</template>
                </span>
              </td>
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
 * 【職責】交易前台，讓登入使用者建立、執行、取消並查看訂單。
 * 【技巧】訂單列表與後台同契約：伺服器分頁、每頁 5；PENDING 另帶 status 篩選。
 * 【概念】壓測後大表不能前端 filter 假分頁，否則與 Portal total 對不齊。
 */
import { computed, onMounted, reactive, ref } from 'vue';
import { cancelOrder, createOrder, executeOrder, fetchOrders, fetchSymbols } from '../api/client';
import { useAuthStore } from '../stores/auth';
import { isOwnOrder, orderOwnerName } from '../utils/orderOwner';
import BackendStoryPanel from '../components/BackendStoryPanel.vue';

/** 與 PortalView 相同，前後台換頁才對得上同一批列。 */
const pageSize = 5;

const auth = useAuthStore();
const isAdmin = computed(() => auth.isAdmin);
const symbols = ref([]);
const pendingOrders = ref([]);
const orders = ref([]);
const pendingPage = ref(0);
const allPage = ref(0);
const pendingTotal = ref(0);
const allTotal = ref(0);
const pendingTotalPages = computed(() => Math.ceil(pendingTotal.value / pageSize) || 1);
const allTotalPages = computed(() => Math.ceil(allTotal.value / pageSize) || 1);
const msg = ref('');
const error = ref('');
const refreshing = ref(false);
const refreshDone = ref(false);
const refreshHint = ref('');
const form = reactive({ symbol: 'AAPL', side: 'BUY', quantity: 1, price: 150 });
let refreshDoneTimer = null;

function canAct(order) {
  return order.status === 'PENDING' && isOwnOrder(order, auth.username);
}

async function loadPending() {
  const res = await fetchOrders({ status: 'PENDING', page: pendingPage.value, size: pageSize });
  pendingOrders.value = res.data || [];
  pendingTotal.value = res.meta?.total || 0;
}

async function loadAll() {
  const res = await fetchOrders({ page: allPage.value, size: pageSize });
  orders.value = res.data || [];
  allTotal.value = res.meta?.total || 0;
}

async function load() {
  await Promise.all([loadPending(), loadAll()]);
}

function prevPending() {
  if (pendingPage.value > 0) {
    pendingPage.value -= 1;
    loadPending();
  }
}

function nextPending() {
  if (pendingPage.value < pendingTotalPages.value - 1) {
    pendingPage.value += 1;
    loadPending();
  }
}

function prevAll() {
  if (allPage.value > 0) {
    allPage.value -= 1;
    loadAll();
  }
}

function nextAll() {
  if (allPage.value < allTotalPages.value - 1) {
    allPage.value += 1;
    loadAll();
  }
}

async function onRefresh() {
  if (refreshing.value) return;
  refreshing.value = true;
  refreshDone.value = false;
  refreshHint.value = '載入訂單中…';
  error.value = '';
  try {
    await load();
    refreshHint.value = `已更新 · PENDING total ${pendingTotal.value}／全部 total ${allTotal.value}`;
    refreshDone.value = true;
    if (refreshDoneTimer) clearTimeout(refreshDoneTimer);
    refreshDoneTimer = setTimeout(() => {
      refreshDone.value = false;
      refreshHint.value = '';
    }, 1800);
  } catch (e) {
    refreshHint.value = '';
    error.value = e.response?.data?.error || '刷新失敗';
  } finally {
    refreshing.value = false;
  }
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
    pendingPage.value = 0;
    allPage.value = 0;
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

<template>
  <div class="story-layout">
    <div>
      <h1>會員後台</h1>
      <p v-if="isAdmin" class="warn-banner demo-scope" role="note">
        Demo：餘額／持倉是 <strong>{{ auth.username }} 本人</strong>（種子 100000、無持倉）。
        下方歷史是 <strong>全站訂單</strong>，AAPL 成交單屬於 trader1，不會扣 admin 的錢。
      </p>
      <p v-else class="page-sub">餘額、持倉、歷史都是你自己的帳（種子：85000 TWD、AAPL 100）。</p>
      <div class="card">
        <h3>{{ isAdmin ? '餘額（本人帳戶）' : '餘額' }}</h3>
        <p v-if="account">{{ account.cashBalance }} {{ account.currency }}</p>
      </div>
      <div class="card">
        <h3>{{ isAdmin ? '持倉（本人帳戶）' : '持倉' }}</h3>
        <p v-if="isAdmin && !positions.length" class="muted">
          admin 種子沒有持倉。要對到 AAPL 100，請改用 trader1 登入。
        </p>
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
        <div class="pager-row">
          <h3>{{ isAdmin ? '交易歷史（全站監察）' : '交易歷史（我的訂單）' }}</h3>
          <div class="pager-actions">
            <button class="secondary sm" type="button" @click="prev" :disabled="page<=0">上一頁</button>
            <button class="secondary sm" type="button" @click="next" :disabled="page>=totalPages-1">下一頁</button>
          </div>
        </div>
        <p style="color:#8b9cb3">page {{ page + 1 }} / {{ totalPages }} · total {{ total }}（每頁 {{ size }}，與交易前台「含結果」同一 API）</p>
        <table>
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
              :key="o.id"
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
 * 【職責】會員後台，彙整帳戶餘額、持倉及分頁交易歷史。
 * 【頁面角色】資產查閱頁；ADMIN 須把「本人帳戶」與「全站訂單」分開標示，避免 Demo 對錯帳。
 * 【與後端關係】從帳戶、持倉與訂單 API 讀取；訂單列含 username 供擁有者對帳。
 */
import { computed, onMounted, ref } from 'vue';
import { fetchAccount, fetchOrders, fetchPositions } from '../api/client';
import { useAuthStore } from '../stores/auth';
import { isOwnOrder, orderOwnerName } from '../utils/orderOwner';
import BackendStoryPanel from '../components/BackendStoryPanel.vue';

const auth = useAuthStore();
const isAdmin = computed(() => auth.isAdmin);
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

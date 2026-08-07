<template>
  <div>
    <h1>審計（ADMIN）</h1>
    <div class="card">
      <table>
        <thead><tr><th>Time</th><th>User</th><th>Action</th><th>Resource</th><th>Detail</th></tr></thead>
        <tbody>
          <tr v-for="a in rows" :key="a.id">
            <td class="audit-time">{{ formatTime(a.createdAt) }}</td>
            <td>{{ a.username }}</td>
            <td>{{ a.action }}</td>
            <td>{{ a.resource }}</td>
            <td>{{ a.detail }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
/**
 * 【職責】管理員審計頁面，列出後端記錄的操作軌跡。
 * 【頁面角色】只由 router 的管理員守衛開放；本頁負責顯示資料，不負責授權判斷。
 * 【與後端關係】掛載後呼叫 /api/audit-logs，讀取第一頁審計紀錄。
 * 【技巧】時間顯示本機 yyyy-mm-dd hh:mm:ss，不露 ISO／毫秒／Z。
 */
import { onMounted, ref } from 'vue';
import { fetchAuditLogs } from '../api/client';

const rows = ref([]);

/**
 * 【目的】把後端 ISO 時間轉成 yyyy-mm-dd hh:mm:ss（本機時區）。
 */
function formatTime(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return String(iso);
  const p = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`;
}

onMounted(async () => {
  const data = await fetchAuditLogs({ page: 0, size: 20 });
  rows.value = data.data || [];
});
</script>

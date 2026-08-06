<template>
  <div>
    <h1>審計（ADMIN）</h1>
    <div class="card">
      <table>
        <thead><tr><th>Time</th><th>User</th><th>Action</th><th>Resource</th><th>Detail</th></tr></thead>
        <tbody>
          <tr v-for="a in rows" :key="a.id">
            <td>{{ a.createdAt }}</td>
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
 */
import { onMounted, ref } from 'vue';
import { fetchAuditLogs } from '../api/client';

const rows = ref([]);

/**
 * 【目的】元件首次掛載時載入最多 20 筆審計紀錄。
 * 【副作用】發出 HTTP 請求並更新 rows；401 會由 API client 清除工作階段與轉址。
 */
onMounted(async () => {
  const data = await fetchAuditLogs({ page: 0, size: 20 });
  rows.value = data.data || [];
});
</script>

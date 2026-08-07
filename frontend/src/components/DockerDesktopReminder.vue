<template>
  <aside class="docker-reminder" :class="{ compact, open }" role="note" aria-label="本機前置">
    <div class="docker-reminder-head">
      <p class="docker-reminder-one">
        <strong>展演：</strong>雙擊 <code>開啟Demo.cmd</code>（Order＋Risk＋Vite）→
        <code>http://localhost:5173/login</code> · trader1 / password
        <span class="muted"> · 全開加 -Full（需 RAM）</span>
      </p>
      <button type="button" class="secondary sm" :aria-expanded="String(open)" @click="open = !open">
        {{ open ? '收合' : '詳情' }}
      </button>
    </div>
    <ul v-show="open" class="docker-reminder-steps">
      <li>燈號 DOWN／記憶體不足 → 關 kind／多餘 Docker，再跑 <code>開啟Demo.cmd</code></li>
      <li>Compose／kubectl 才需要 Docker Desktop Ready</li>
      <li>面試劇本：登入 → Trade 下單 → 成交 → Portal 歷史</li>
    </ul>
  </aside>
</template>

<script setup>
/**
 * 【職責】精簡本機前置提醒（Docker／一鍵 Demo），對齊 SPEC §3.1。
 * 【技巧】預設一行摘要；詳情可展開。dismissible 保留相容（等同可展開）。
 * 【概念】引擎未 Ready／行程未起 ≠ 業務 bug。
 */
import { ref } from 'vue';

defineProps({
  compact: { type: Boolean, default: false },
  /** 相容舊用法：允許展開詳情 */
  dismissible: { type: Boolean, default: true }
});

/** 預設收合，只留一行操作摘要 */
const open = ref(false);
</script>

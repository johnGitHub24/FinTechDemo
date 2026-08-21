<template>
  <section
    :id="sectionId"
    class="docker-redis-guide"
    :class="{ card: !embedded, 'docker-redis-embedded': embedded }"
  >
    <h2 v-if="!embedded">{{ heading }}</h2>
    <h3 v-else class="observe-h3" style="margin-top:0">{{ heading }}</h3>
    <p class="story-meta">
      先看<strong>提示字元</strong>再下指令。
      <code>Docker Desktop.exe</code> 只開 GUI；真正指令在 <strong>Windows PowerShell</strong>。
    </p>

    <h3 class="observe-h3">① 你現在在哪（打錯地方就 not found）</h3>
    <table>
      <thead>
        <tr><th>提示字元</th><th>這是什麼</th><th>可以打</th></tr>
      </thead>
      <tbody>
        <tr>
          <td><code>PS D:\...\FinTechDemo&gt;</code></td>
          <td>Windows PowerShell（專案根）</td>
          <td><code>docker</code>／<code>docker compose</code>／<code>docker desktop</code></td>
        </tr>
        <tr>
          <td><code>127.0.0.1:6379&gt;</code></td>
          <td>redis-cli（已連上 Redis）</td>
          <td><code>PING</code>／<code>KEYS *</code>／<code>GET</code>（不能打 docker）</td>
        </tr>
        <tr>
          <td><code>/data #</code></td>
          <td>容器裡的 Linux shell</td>
          <td>先打 <code>redis-cli</code>；這裡沒有 docker</td>
        </tr>
      </tbody>
    </table>
    <p class="muted small">
      看到 <code>/bin/sh: PING: not found</code>＝人在 <code>/data #</code>，還沒進 redis-cli。
      先 <code>redis-cli</code>，出現 <code>127.0.0.1:6379&gt;</code> 再打 <code>PING</code>。
    </p>

    <h3 class="observe-h3">② Docker Desktop（開關引擎）</h3>
    <p class="muted small">IntelliJ 連 <code>127.0.0.1:6379</code> 失敗、compose 報 <code>dockerDesktopLinuxEngine</code>＝Desktop 沒開。</p>
    <div v-for="row in desktopCmds" :key="row.cmd" class="observe-cmd-row">
      <code class="observe-cmd">{{ row.cmd }}</code>
      <button type="button" class="secondary sm" @click="copyText(row.cmd)">複製</button>
      <span class="muted small">{{ row.hint }}</span>
    </div>

    <h3 class="observe-h3">③ 起 Redis 容器（PowerShell）</h3>
    <p class="muted small">目錄必須是專案根 <code>FinTechDemo</code>（有 <code>docker-compose.yml</code>）。</p>
    <div v-for="row in composeCmds" :key="row.cmd" class="observe-cmd-row">
      <code class="observe-cmd">{{ row.cmd }}</code>
      <button type="button" class="secondary sm" @click="copyText(row.cmd)">複製</button>
      <span class="muted small">{{ row.hint }}</span>
    </div>

    <h3 class="observe-h3">④ 進 redis-cli（PowerShell 打這一行就好）</h3>
    <div class="observe-cmd-row">
      <code class="observe-cmd">{{ redisCliEnter }}</code>
      <button type="button" class="secondary sm" @click="copyText(redisCliEnter)">複製</button>
    </div>
    <p class="muted small">出現 <code>127.0.0.1:6379&gt;</code> 後再貼下面（不要跟 docker 指令混在同一段）。</p>
    <div v-for="row in redisCmds" :key="row.cmd" class="observe-cmd-row">
      <code class="observe-cmd">{{ row.cmd }}</code>
      <button type="button" class="secondary sm" @click="copyText(row.cmd)">複製</button>
      <span class="muted small">{{ row.hint }}</span>
    </div>
    <p class="muted small"><code>exit</code> 離開 redis-cli，回到 <code>PS&gt;</code> 才能再打 docker。</p>

    <h3 class="observe-h3">⑤ IntelliJ Database → Redis</h3>
    <table>
      <thead>
        <tr><th>欄位</th><th>值</th></tr>
      </thead>
      <tbody>
        <tr><td>Host</td><td><code>127.0.0.1</code></td></tr>
        <tr><td>Port</td><td><code>6379</code></td></tr>
        <tr><td>User／Password</td><td>空白（本 Demo 無密碼）</td></tr>
        <tr><td>Database</td><td><code>0</code></td></tr>
        <tr><td>SSL</td><td>關</td></tr>
      </tbody>
    </table>
    <p class="muted small">右鍵資料源 → Open Redis Console，指令與 redis-cli 相同。</p>

    <aside class="story-note">
      <strong>NOTE · 為什麼 KEYS * 是空的</strong>
      <ul>
        <li><code>PING</code>＝<code>PONG</code> 代表連線成功；<code>DBSIZE 0</code>＝庫裡還沒有 key</li>
        <li>Account 預設 <code>fintech.redis.enabled=false</code>，查帳戶只打 H2，不寫 Redis</li>
        <li>要看程式寫入：account 加 <code>--spring.profiles.active=demo</code>，再打 <code>GET /api/accounts/me</code></li>
        <li>key：<code>account:&#123;userId&#125;</code>／<code>positions:&#123;userId&#125;</code>，TTL 約 60 秒</li>
        <li>練習可先 <code>SET account:1 "demo"</code>，刷新 IntelliJ database 0 就看得到</li>
      </ul>
    </aside>
    <p v-if="copyMsg" class="observe-copy-msg" role="status">{{ copyMsg }}</p>
  </section>
</template>

<script setup>
/**
 * 【職責】Docker Desktop／compose／redis-cli 教學指令（藍圖＋交易頁共用）。
 * 【技巧】複製鈕寫入剪貼簿；指令分「提示字元」避免 /data # 誤打 PING。
 * 【概念】exe＝GUI；docker＝主機 CLI；redis-cli＝已連上的 Redis 指令列。
 */
import { ref } from 'vue';

defineProps({
  heading: { type: String, default: 'Docker／Redis 指令（本機 Demo）' },
  /** 嵌在 Demo 面板時去掉外層 card，避免雙重框 */
  embedded: { type: Boolean, default: false },
  sectionId: { type: String, default: 'docker-redis' }
});

const copyMsg = ref('');
const redisCliEnter = 'docker exec -it fintech-demo-redis redis-cli';

const desktopCmds = [
  { cmd: 'docker desktop start', hint: '啟動引擎（等 Ready）' },
  { cmd: 'docker desktop status', hint: '看是否 Running' },
  { cmd: 'docker desktop stop', hint: '關閉引擎' }
];

const composeCmds = [
  { cmd: 'docker compose up -d redis', hint: '只起 Redis :6379' },
  { cmd: 'docker compose up -d redpanda redis', hint: 'Redis + Kafka' },
  { cmd: 'docker ps', hint: '看容器是否在跑' },
  { cmd: 'docker exec fintech-demo-redis redis-cli ping', hint: '應回 PONG' },
  { cmd: 'docker compose down', hint: '停掉本專案 compose 容器' }
];

const redisCmds = [
  { cmd: 'PING', hint: '應回 PONG' },
  { cmd: 'KEYS *', hint: '列出全部 key（空＝還沒寫入）' },
  { cmd: 'DBSIZE', hint: 'key 數量' },
  { cmd: 'SET account:1 demo', hint: '練習寫一筆（不必等程式）' },
  { cmd: 'GET account:1', hint: '讀回字串' },
  { cmd: 'TTL account:1', hint: '-1 永不過期；-2 不存在' },
  { cmd: 'DEL account:1', hint: '刪練習 key' },
  { cmd: 'exit', hint: '離開 redis-cli' }
];

async function copyText(text) {
  try {
    await navigator.clipboard.writeText(text);
    copyMsg.value = '已複製';
  } catch {
    copyMsg.value = '複製失敗，請手動選取';
  }
  window.setTimeout(() => { copyMsg.value = ''; }, 2000);
}
</script>

<template>
  <div class="blueprint">
    <header v-if="!loggedIn" class="blueprint-guest-bar">
      <span class="brand">FinTech<span>Demo</span></span>
      <router-link to="/login">回登入</router-link>
    </header>

    <h1>系統運作藍圖</h1>
    <p class="blueprint-lead">
      本頁用 HTML 說明「導入了哪些技術、系統怎麼跑」——Demo 時點導覽即可講，不必另開文件。
    </p>

    <aside class="blueprint-howto" aria-label="Demo 建議講法">
      <div class="howto-frame">
        <header class="howto-frame-title">
          <span class="howto-frame-badge">Demo 腳本</span>
          <strong>建議講法（約 3 分鐘）</strong>
          <span class="howto-frame-hint">點目錄跳轉 · 邊框區＝講解順序</span>
        </header>
        <ol class="howto-steps">
          <li class="howto-step">
            <span class="howto-n" aria-hidden="true">1</span>
            <div class="howto-body">
              <a href="#stack">技術棧表</a>
              <p>前後端語言／框架版本</p>
            </div>
          </li>
          <li class="howto-step">
            <span class="howto-n" aria-hidden="true">2</span>
            <div class="howto-body">
              <a href="#layers">分層架構圖</a>
              <p>誰連誰（實線＝主路徑，虛線＝可選）</p>
            </div>
          </li>
          <li class="howto-step">
            <span class="howto-n" aria-hidden="true">3</span>
            <div class="howto-body">
              <a href="#flow">運作過程</a>
              <p>Login → PENDING → Feign Risk → ACCEPTED／REJECTED</p>
            </div>
          </li>
          <li class="howto-step">
            <span class="howto-n" aria-hidden="true">4</span>
            <div class="howto-body">
              <a href="#states">訂單狀態機</a> · <a href="#stages">S1–S3</a>
              <p>環境開到哪 ≠ 訂單狀態</p>
            </div>
          </li>
          <li class="howto-step">
            <span class="howto-n" aria-hidden="true">5</span>
            <div class="howto-body">
              <span class="howto-label">Compose／K8s</span>
              <p>開 Docker → <code>開啟Demo.cmd</code>（SPEC §3.1）</p>
            </div>
          </li>
          <li class="howto-step">
            <span class="howto-n" aria-hidden="true">6</span>
            <div class="howto-body">
              <span class="howto-label">即時綠紅燈</span>
              <p>回「交易前台／會員後台」右側 PROCESS FLOW</p>
            </div>
          </li>
          <li class="howto-step howto-step-bonus">
            <span class="howto-n" aria-hidden="true">+</span>
            <div class="howto-body">
              <a href="#observe">觀測／壓測</a>
              <p>Locust 打 → Prometheus 記 → Grafana 畫</p>
            </div>
          </li>
        </ol>
      </div>
    </aside>

    <nav class="blueprint-toc card" aria-label="頁內目錄">
      <span class="toc-label">目錄</span>
      <a href="#stack">技術棧</a>
      <span class="toc-sep" aria-hidden="true">·</span>
      <a href="#layers">分層架構</a>
      <span class="toc-sep" aria-hidden="true">·</span>
      <a href="#flow">運作過程</a>
      <span class="toc-sep" aria-hidden="true">·</span>
      <a href="#states">訂單狀態</a>
      <span class="toc-sep" aria-hidden="true">·</span>
      <a href="#stages">S1–S3</a>
      <span class="toc-sep" aria-hidden="true">·</span>
      <a href="#ports">埠對照</a>
      <span class="toc-sep" aria-hidden="true">·</span>
      <a href="#observe">觀測／壓測</a>
      <span class="toc-sep" aria-hidden="true">·</span>
      <a href="#k8s-verify">K8s 驗證</a>
    </nav>

    <section id="stack" class="card">
      <h2>1. 技術棧與版本</h2>
      <p class="story-meta">
        權威來源：<code>frontend/package.json</code>、<code>build.gradle</code>（Java 21 · Boot 3.2.2 · Cloud 2023.0.0）。
        「Boot starter／Cloud BOM」表示版本跟著官方清單走，不必逐一手鎖。同一層收在同一個框。
        <strong>BOM</strong>＝Bill of Materials（依賴版本總表）。
      </p>
      <div class="stack-groups">
        <div
          v-for="g in techGroups"
          :key="g.id"
          class="stack-group"
          :class="'tone-' + g.tone"
        >
          <header class="stack-group-head">
            <strong>{{ g.title }}</strong>
            <span>{{ g.blurb }}</span>
          </header>
          <table>
            <thead>
              <tr><th>技術</th><th>版本</th><th>主要目的</th><th>本專案怎麼用</th></tr>
            </thead>
            <tbody>
              <tr v-for="(row, i) in g.items" :key="i">
                <td>{{ row.tech }}</td>
                <td><code>{{ row.version }}</code></td>
                <td class="col-purpose">{{ row.purpose }}</td>
                <td class="col-note">{{ row.note }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>

    <section id="layers" class="card">
      <h2>2. 分層架構</h2>
      <p class="story-meta">
        由上而下：前端 →（可選 Gateway）→ 微服務 → 資料／訊息。
        <strong>實線</strong>＝最短可成交主路徑；<strong>虛線</strong>＝可選（Gateway／Kafka／Job）。
      </p>
      <div ref="elLayers" class="mermaid-wrap" role="img" aria-label="分層架構圖"></div>
      <aside class="story-note">
        <strong>NOTE · 圖怎麼讀</strong>
        <ul>
          <li>現場 Demo 最短：Vue <code>:5173</code> → Order <code>:8081</code> → Feign Risk <code>:8082</code></li>
          <li>講「統一入口」時再開 Gateway <code>:8080</code>（轉發並可帶 <code>X-Demo-Via-Gateway</code>）</li>
          <li>Account／Redis／Kafka、Job 屬加分敘事，不擋最短成交</li>
        </ul>
      </aside>
    </section>

    <section id="flow" class="card">
      <h2>3. 完整運作過程</h2>
      <p class="story-meta">
        每個節點標註埠與框架。分支「最短可成交」vs「分散式敘事（經 Gateway）」都匯入同一成交流程。
      </p>
      <div ref="elFlow" class="mermaid-wrap" role="img" aria-label="完整運作過程圖"></div>
      <aside class="story-note">
        <strong>NOTE · 過程怎麼講</strong>
        <ol>
          <li><strong>Login</strong>：Vue → Order <code>:8081</code>；
            <strong>JWT</strong>＝權杖標準、<strong>JJWT</strong>＝Java 簽驗函式庫（0.12.5）；
            Spring Security 過濾器驗 Token 後做 RBAC</li>
          <li><strong>下單</strong>：JPA 寫入，狀態 <code>PENDING</code></li>
          <li><strong>入口</strong>：最短直連 Order；完整敘事可經 Gateway <code>:8080</code></li>
          <li><strong>成交</strong>：Order 以 <strong>OpenFeign</strong>（宣告式 HTTP 客戶端）同步呼叫 Risk <code>:8082</code> 名目風控</li>
          <li><strong>結果</strong>：通過 <code>ACCEPTED</code>／拒絕 <code>REJECTED</code></li>
          <li><strong>帳務（可選）</strong>：Account <code>:8084</code> + Redis + Kafka</li>
          <li><strong>逾時（可選）</strong>：Job <code>:8083</code> 久未成交的 PENDING → <code>CANCELLED</code></li>
        </ol>
      </aside>
    </section>

    <section id="states" class="card">
      <h2>4. 訂單狀態機</h2>
      <p class="story-meta">回答「這一筆單子到哪」——與下方 S1–S3（環境開到哪）是兩件事。箭頭旁文字為轉換條件（含用到的技術）。</p>
      <div ref="elStates" class="mermaid-wrap" role="img" aria-label="訂單狀態機"></div>
      <aside class="story-note">
        <strong>NOTE · 四態</strong>
        <ul>
          <li><strong>PENDING</strong>：已建立，尚未成交／取消</li>
          <li><strong>ACCEPTED</strong>：成交且 Risk 通過</li>
          <li><strong>REJECTED</strong>：成交時風控拒絕（名目超限等）</li>
          <li><strong>CANCELLED</strong>：使用者取消，或 Job 逾時取消</li>
        </ul>
      </aside>
    </section>

    <section id="stages" class="card">
      <h2>5. 部署階梯（敘事）S1–S3</h2>
      <p class="story-meta">
        依「哪些服務有起來」講環境完整度。Trade／Portal 的 PROCESS FLOW 會依 health 推斷並可釘住講解。
      </p>
      <table>
        <thead>
          <tr><th>階</th><th>條件</th><th>意義</th></tr>
        </thead>
        <tbody>
          <tr><td><strong>S1</strong></td><td>Order :8081</td><td>可登入／建 PENDING</td></tr>
          <tr><td><strong>S2</strong></td><td>Order + Risk :8082</td><td>最短可成交（必開這兩個）</td></tr>
          <tr><td><strong>S3</strong></td><td>S2 + Gateway <em>或</em> Account</td><td>統一入口／帳務敘事</td></tr>
          <tr><td>—</td><td>Job :8083</td><td>可選排程；<strong>不進</strong> S 公式</td></tr>
          <tr><td>S4+</td><td>K8s 等</td><td>僅文件／手動講解，本頁不自動宣稱</td></tr>
        </tbody>
      </table>
    </section>

    <section id="ports" class="card">
      <h2>6. 埠對照</h2>
      <p class="story-meta">IntelliJ／compose 對照用；健康檢查路徑為各服務 <code>/actuator/health</code>。</p>
      <table>
        <thead>
          <tr><th>Port</th><th>服務</th><th>角色</th></tr>
        </thead>
        <tbody>
          <tr v-for="p in PORTS" :key="p.port">
            <td><code>{{ p.port }}</code></td>
            <td>{{ p.service }}</td>
            <td>{{ p.role }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section id="observe" class="card">
      <h2>7. 觀測／壓測怎麼用（簡易）</h2>
      <p class="story-meta">
        記法：<strong>Locust＝製造壓力</strong> → <strong>Prometheus＝記帳本</strong> → <strong>Grafana＝報表畫圖</strong>。
        日常 Demo 看 Grafana；Prometheus 用來確認 Targets UP 與臨時查詢。
      </p>

      <aside class="story-note">
        <strong>怎麼運作</strong>
        <ol>
          <li>各服務暴露 <code>/actuator/prometheus</code>（數字出口）</li>
          <li>Prometheus 每 15s 刮取（Targets 全 UP＝帳本有在收）</li>
          <li>Grafana 連 Prometheus，畫成 Overview 儀表板</li>
          <li>Locust（或你手動下單）打 API → 曲線才會動</li>
        </ol>
      </aside>

      <h3 class="observe-h3">① 先啟動</h3>
      <div class="observe-cmd-row">
        <code class="observe-cmd">docker compose --profile monitoring up -d</code>
        <button type="button" class="secondary sm" @click="copyText('docker compose --profile monitoring up -d')">複製</button>
      </div>
      <p class="muted small">Grafana <code>:3000</code>（帳密 <code>admin</code>／<code>admin</code>）· Prometheus <code>:9090</code></p>
      <div class="observe-cmd-row">
        <code class="observe-cmd">.\scripts\run-loadtest.ps1 -WebUi</code>
        <button type="button" class="secondary sm" @click="copyText('.\\scripts\\run-loadtest.ps1 -WebUi')">複製</button>
      </div>
      <p class="muted small">Locust UI <code>:8089</code>（虛擬帳用系統的 <code>trader1</code>／<code>password</code>）</p>

      <h3 class="observe-h3">② Prometheus（查詢範本）</h3>
      <p class="muted small">開 <a :href="links.prometheusUi" target="_blank" rel="noopener">http://localhost:9090</a> → Graph → 貼查詢 → Execute → 切 Graph 看曲線。</p>
      <ul class="observe-queries">
        <li v-for="q in promQueries" :key="q.expr">
          <div class="observe-cmd-row">
            <code class="observe-cmd">{{ q.expr }}</code>
            <button type="button" class="secondary sm" @click="copyText(q.expr)">複製</button>
          </div>
          <p class="muted small">→ {{ q.hint }}</p>
        </li>
      </ul>

      <h3 class="observe-h3">③ Grafana（看圖）</h3>
      <ol class="observe-steps">
        <li>開 <a :href="links.grafana" target="_blank" rel="noopener">FinTechDemo Overview</a>（或 Dashboards 搜尋 FinTechDemo）</li>
        <li>登入 <code>admin</code>／<code>admin</code></li>
        <li>看：Targets UP、HTTP RPS、5xx、JVM heap</li>
        <li>一邊 Locust／Trade 操作，一邊看線圖往上</li>
      </ol>

      <h3 class="observe-h3">④ Locust（壓測）</h3>
      <ol class="observe-steps">
        <li>開 <a :href="links.locust" target="_blank" rel="noopener">http://localhost:8089</a></li>
        <li>Users <code>5</code> · Spawn rate <code>1</code> · Host <code>http://localhost:8081</code>（經 Gateway 改 <code>:8080</code>）</li>
        <li>Start swarming → 看 RPS／Failures（門檻：錯誤率 &lt; 1%）</li>
        <li>無畫面報告：<code>.\scripts\run-loadtest.ps1</code> → <code>loadtest/reports/*.html</code></li>
      </ol>

      <p class="observe-links">
        快捷：
        <a :href="links.grafana" target="_blank" rel="noopener">Grafana</a> ·
        <a :href="links.prometheusUi" target="_blank" rel="noopener">Prometheus</a> ·
        <a :href="links.locust" target="_blank" rel="noopener">Locust</a> ·
        <a :href="links.orderPrometheus" target="_blank" rel="noopener">Order 原始指標</a>
      </p>
      <p v-if="copyMsg" class="observe-copy-msg" role="status">{{ copyMsg }}</p>
    </section>

    <section id="k8s-verify" class="card">
      <h2>8. K8s 驗證（精簡）</h2>
      <p class="story-meta">一鍵複製四條指令即可：Docker → readyz → nodes → fintech-demo。</p>
      <K8sVerifyPanel initially-open />
    </section>
  </div>
</template>

<script setup>
/**
 * 【職責】以 HTML＋Mermaid 展示完整技術架構與系統運作過程，供 Demo 講解。
 * 【頁面角色】公開藍圖頁（可不登入）；與 Trade／Portal 的即時 PROCESS FLOW 互補。
 * 【與後端關係】無 API；版本與圖碼為靜態常數。
 * 【技巧】用 mermaid.render 寫入容器，避免 pre 文字殘留與 HMR 舊 SVG。
 * 【概念】§7 觀測／壓測＝怕忘記時的簡易備忘（Locust→Prom→Grafana）。
 */
import { computed, nextTick, onMounted, ref } from 'vue';
import { useAuthStore } from '../stores/auth';
import K8sVerifyPanel from '../components/K8sVerifyPanel.vue';
import { demoLinks } from '../config/demoLinks';
import {
  DIAGRAM_FLOW,
  DIAGRAM_LAYERS,
  DIAGRAM_ORDER_STATE,
  PORTS,
  groupTechStack
} from '../blueprint/diagrams';

const auth = useAuthStore();
const loggedIn = computed(() => !!auth.isLoggedIn);
const techGroups = groupTechStack();
const links = demoLinks;
const copyMsg = ref('');

/** Prometheus 教學範本（與 Targets UP 後下一步對齊） */
const promQueries = [
  { expr: 'up{job=~"fintech-.*"}', hint: '五個服務是否活著（應看到多條 =1）' },
  {
    expr: 'sum(rate(http_server_requests_seconds_count{job="fintech-order"}[1m]))',
    hint: 'Order 近 1 分鐘大約每秒請求數（壓測／下單時會動）'
  },
  {
    expr: 'sum by (job) (http_server_requests_seconds_count)',
    hint: '各服務 HTTP 請求累積次數'
  }
];

const elLayers = ref(null);
const elFlow = ref(null);
const elStates = ref(null);

async function copyText(text) {
  try {
    await navigator.clipboard.writeText(text);
    copyMsg.value = '已複製';
  } catch {
    copyMsg.value = '複製失敗，請手動選取';
  }
  window.setTimeout(() => { copyMsg.value = ''; }, 2000);
}

async function renderDiagram(el, code, prefix) {
  if (!el) return;
  const mermaid = (await import('mermaid')).default;
  const id = `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
  const { svg } = await mermaid.render(id, code.trim());
  el.innerHTML = svg;
}

onMounted(async () => {
  const mermaid = (await import('mermaid')).default;
  mermaid.initialize({
    startOnLoad: false,
    theme: 'neutral',
    securityLevel: 'loose',
    flowchart: { htmlLabels: true, curve: 'basis' }
  });
  await nextTick();
  await renderDiagram(elLayers.value, DIAGRAM_LAYERS, 'bp-layers');
  await renderDiagram(elFlow.value, DIAGRAM_FLOW, 'bp-flow');
  await renderDiagram(elStates.value, DIAGRAM_ORDER_STATE, 'bp-states');
});
</script>

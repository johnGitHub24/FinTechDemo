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

    <aside class="story-note blueprint-howto">
      <strong>Demo 建議講法（約 3 分鐘）</strong>
      <ol>
        <li>先指<strong>技術棧表</strong>：前後端語言／框架版本</li>
        <li>再指<strong>分層架構圖</strong>：誰連誰（實線＝主路徑，虛線＝可選）</li>
        <li>用<strong>運作過程</strong>走一遍：Login → 下單 PENDING → 成交 Feign Risk → ACCEPTED／REJECTED</li>
        <li>對照<strong>訂單狀態機</strong>與<strong>S1–S3</strong>（環境開到哪 ≠ 訂單狀態）</li>
        <li>要看<strong>即時綠紅燈</strong>：回「交易前台／會員後台」右側 PROCESS FLOW</li>
      </ol>
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
    </nav>

    <section id="stack" class="card">
      <h2>1. 技術棧與版本</h2>
      <p class="story-meta">
        權威來源：<code>frontend/package.json</code>、<code>build.gradle</code>（Java 21 · Boot 3.2.2 · Cloud 2023.0.0）。
        「Boot starter」表示版本跟隨 Spring Boot BOM，不另鎖號。同一層收在同一個框。
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
              <tr><th>技術</th><th>版本</th><th>說明</th></tr>
            </thead>
            <tbody>
              <tr v-for="(row, i) in g.items" :key="i">
                <td>{{ row.tech }}</td>
                <td><code>{{ row.version }}</code></td>
                <td>{{ row.note }}</td>
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
          <li><strong>成交</strong>：Order OpenFeign → Risk <code>:8082</code> 名目風控</li>
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
  </div>
</template>

<script setup>
/**
 * 【職責】以 HTML＋Mermaid 展示完整技術架構與系統運作過程，供 Demo 講解。
 * 【頁面角色】公開藍圖頁（可不登入）；與 Trade／Portal 的即時 PROCESS FLOW 互補。
 * 【與後端關係】無 API；版本與圖碼為靜態常數。
 * 【技巧】用 mermaid.render 寫入容器，避免 pre 文字殘留與 HMR 舊 SVG。
 */
import { computed, nextTick, onMounted, ref, unref } from 'vue';
import { useAuthStore } from '../stores/auth';
import {
  DIAGRAM_FLOW,
  DIAGRAM_LAYERS,
  DIAGRAM_ORDER_STATE,
  PORTS,
  groupTechStack
} from '../blueprint/diagrams';

const auth = useAuthStore();
const loggedIn = computed(() => !!unref(auth.isLoggedIn));
const techGroups = groupTechStack();

const elLayers = ref(null);
const elFlow = ref(null);
const elStates = ref(null);

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

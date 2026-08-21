<template>
  <div class="app-shell">
    <nav v-if="auth.isLoggedIn" class="app-nav">
      <span class="brand">FinTech<span>Demo</span></span>
      <router-link to="/trade">交易前台</router-link>
      <router-link to="/blueprint">系統運作藍圖</router-link>
      <router-link to="/portal">會員後台</router-link>
      <router-link v-if="isAdmin" to="/portal/audit">審計</router-link>
      <span class="nav-demo-links" aria-label="觀測與快捷">
        <button
          v-for="btn in navDemoButtons"
          :key="btn.id"
          type="button"
          class="demo-nav-btn"
          :title="btn.hint"
          :disabled="navBusy === btn.id"
          @click="onNavClick(btn)"
        >{{ btn.label }}</button>
      </span>
      <span class="user-chip">{{ auth.username }}{{ isAdmin ? ' ·ADMIN' : '' }}</span>
      <button class="secondary sm" type="button" @click="logout">登出</button>
    </nav>

    <p v-if="auth.isLoggedIn && navMsg" class="nav-status" :class="navMsgOk ? 'ok' : 'warn'" role="status">
      {{ navMsg }}
    </p>

    <DockerDesktopReminder class="app-docker-reminder" />

    <DemoShortcutsPanel
      v-if="auth.isLoggedIn"
      ref="demoPanel"
      class="app-demo-shortcuts"
      title="Demo 快捷（登入後可測）"
      collapsible
      :initially-open="false"
    />

    <main class="page">
      <router-view />
    </main>
  </div>
</template>

<script setup>
/**
 * 【職責】導覽列：SPA／外部／Demo 面板按鈕皆有明確行為與失敗提示。
 * 【技巧】Grafana 等先 probe；K8s 用 goSpa+hash；Demo 快捷 expand+scroll。
 * 【概念】loop-engineering：按鈕失敗要告訴下一步，不能靜默 connection refused。
 */
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from './stores/auth';
import { navDemoButtons } from './config/demoLinks';
import { goSpa, openDemoPanel, openExternal } from './utils/demoNav';
import DockerDesktopReminder from './components/DockerDesktopReminder.vue';
import DemoShortcutsPanel from './components/DemoShortcutsPanel.vue';

const auth = useAuthStore();
const router = useRouter();
const demoPanel = ref(null);
const navMsg = ref('');
const navMsgOk = ref(true);
const navBusy = ref('');

const isAdmin = computed(() => auth.isAdmin);

function logout() {
  auth.clearSession();
  router.push('/login');
}

function setMsg(text, ok = false) {
  navMsg.value = text;
  navMsgOk.value = ok;
}

async function onNavClick(btn) {
  navBusy.value = btn.id;
  setMsg('');
  try {
    if (btn.kind === 'external') {
      setMsg(`探測 ${btn.label}…`, true);
      const err = await openExternal(btn.href, btn.probe, btn.startHint);
      if (err) setMsg(err, false);
      else setMsg(`已開啟 ${btn.label}`, true);
    } else if (btn.kind === 'spa') {
      await goSpa(router, btn.to, btn.hash);
      setMsg(`已前往 ${btn.label}`, true);
    } else if (btn.kind === 'panel') {
      openDemoPanel(demoPanel, btn.panelTab);
      setMsg(btn.panelTab === 'docker' ? '已展開 Docker 分頁' : '已展開 Demo 快捷面板', true);
    }
  } finally {
    navBusy.value = '';
    if (navMsgOk.value) {
      setTimeout(() => {
        if (navMsgOk.value) navMsg.value = '';
      }, 2500);
    }
  }
}
</script>

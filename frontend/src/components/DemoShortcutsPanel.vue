<template>
  <aside id="demo-shortcuts" class="card demo-shortcuts-panel" aria-label="Demo 快捷入口">
    <div class="demo-shortcuts-head">
      <h2>{{ title }}</h2>
      <button v-if="collapsible" type="button" class="secondary sm" @click="open = !open">
        {{ open ? '收合' : '展開' }}
      </button>
    </div>

    <div v-show="!collapsible || open">
      <div class="demo-panel-tabs" role="tablist" aria-label="Demo 面板分頁">
        <button
          type="button"
          role="tab"
          class="demo-panel-tab"
          :class="{ active: tab === 'links' }"
          :aria-selected="tab === 'links'"
          @click="tab = 'links'"
        >快捷</button>
        <button
          type="button"
          role="tab"
          class="demo-panel-tab"
          :class="{ active: tab === 'docker' }"
          :aria-selected="tab === 'docker'"
          @click="tab = 'docker'"
        >Docker</button>
      </div>

      <div v-show="tab === 'links'" role="tabpanel">
        <p class="muted small demo-shortcuts-lead">
          <template v-if="loggedIn">
            點 Trade／Portal／藍圖＝同頁切換（已帶 JWT）。
            Audit 需用 <code>admin</code> 登入。觀測／Docs／Javadoc／測試報告會先探測埠。
          </template>
          <template v-else>
            點需登入頁會<strong>記住目標</strong>，登入後自動前往。
          </template>
        </p>
        <p v-if="statusMsg" class="warn-banner" role="status">{{ statusMsg }}</p>
        <p v-if="pendingLabel" class="ok-banner">
          已選「{{ pendingLabel }}」→ 請登入，成功後自動進入。
        </p>

        <div v-for="group in groups" :key="group.title" class="demo-group">
          <h3>{{ group.title }}</h3>
          <div class="demo-link-row">
            <template v-for="item in group.items" :key="itemKey(item)">
              <button
                v-if="item.spaPath"
                type="button"
                class="demo-chip"
                :class="spaClass(item)"
                :title="chipTitle(item)"
                @click="onSpaClick(item)"
              >{{ spaLabel(item) }}</button>
              <button
                v-else-if="item.panelTab"
                type="button"
                class="demo-chip"
                :title="item.hint || item.label"
                @click="openTab(item.panelTab)"
              >{{ item.label }}</button>
              <button
                v-else
                type="button"
                class="demo-chip"
                :title="item.href"
                @click="onExternalClick(item)"
              >{{ item.label }}</button>
            </template>
          </div>
        </div>
      </div>

      <div v-show="tab === 'docker'" id="demo-docker" role="tabpanel" class="demo-docker-pane">
        <DockerRedisGuide heading="Docker／Redis 指令（本機 Demo）" embedded section-id="demo-docker-guide" />
      </div>
    </div>
  </aside>
</template>

<script setup>
/**
 * 【職責】Demo 快捷：SPA／外部連結；另開 Docker 分頁嵌 Docker／Redis 教學。
 * 【技巧】expose expand(tab)；頂欄「Docker」可直接展開並切到 docker 分頁。
 * 【概念】交易頁不塞基建教學；Docker 說明集中在 Demo 面板，避免下單區過長。
 */
import { computed, nextTick, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { loginDemoGroups, NEXT_PATH_KEY } from '../config/demoLinks';
import { goSpa, openExternal } from '../utils/demoNav';
import { scrollToId } from '../utils/jqueryDom';
import DockerRedisGuide from './DockerRedisGuide.vue';

const props = defineProps({
  title: { type: String, default: 'Demo 快捷入口' },
  collapsible: { type: Boolean, default: false },
  initiallyOpen: { type: Boolean, default: true }
});

const open = ref(props.initiallyOpen);
const tab = ref('links');

defineExpose({
  /**
   * 【目的】展開面板；可選切到 docker／links 分頁並捲到錨點。
   */
  expand(nextTab) {
    open.value = true;
    if (nextTab === 'docker' || nextTab === 'links') {
      tab.value = nextTab;
    }
    nextTick(() => {
      if (tab.value === 'docker') scrollToId('demo-docker');
      else scrollToId('demo-shortcuts');
    });
  }
});

const router = useRouter();
const auth = useAuthStore();
const groups = loginDemoGroups;
const pendingPath = ref(sessionStorage.getItem(NEXT_PATH_KEY) || '');
const statusMsg = ref('');

const loggedIn = computed(() => !!auth.isLoggedIn);
const isAdmin = computed(() => auth.isAdmin);

const pendingLabel = computed(() => {
  if (!pendingPath.value || loggedIn.value) return '';
  for (const g of groups) {
    for (const it of g.items) {
      if (it.spaPath && stripHash(it.spaPath) === pendingPath.value) return it.label;
    }
  }
  return pendingPath.value;
});

function itemKey(item) {
  return item.spaPath || item.panelTab || item.href || item.label;
}

function stripHash(path) {
  return (path || '').split('#')[0];
}

function hashOf(path) {
  if (!path?.includes('#')) return '';
  return path.slice(path.indexOf('#') + 1);
}

function spaLabel(item) {
  if (item.needAdmin) return `${item.label}${isAdmin.value ? '' : ' ·需admin'}`;
  return item.label;
}

function spaClass(item) {
  return {
    locked: item.needLogin && !loggedIn.value,
    blocked: item.needAdmin && loggedIn.value && !isAdmin.value,
    pending: pendingPath.value === stripHash(item.spaPath)
  };
}

function chipTitle(item) {
  if (item.needAdmin && loggedIn.value && !isAdmin.value) {
    return '目前是 USER。登出後用 admin / password 再進 Audit';
  }
  if (item.needLogin && !loggedIn.value) return '點一下記住，再登入';
  return item.spaPath;
}

function openTab(name) {
  tab.value = name === 'docker' ? 'docker' : 'links';
  statusMsg.value = name === 'docker' ? '已切到 Docker 分頁' : '';
  nextTick(() => {
    if (tab.value === 'docker') scrollToId('demo-docker');
  });
  if (statusMsg.value) {
    setTimeout(() => { if (statusMsg.value.startsWith('已切到')) statusMsg.value = ''; }, 2000);
  }
}

async function onSpaClick(item) {
  statusMsg.value = '';
  const path = stripHash(item.spaPath);
  const hash = hashOf(item.spaPath);

  if (item.needLogin && !loggedIn.value) {
    sessionStorage.setItem(NEXT_PATH_KEY, path);
    pendingPath.value = path;
    statusMsg.value = `已記住「${item.label}」。請登入（成功後自動前往）。`;
    return;
  }
  if (item.needAdmin && !isAdmin.value) {
    statusMsg.value = 'Audit 需要 ADMIN。請登出，用 admin / password 登入後再點。';
    return;
  }
  sessionStorage.removeItem(NEXT_PATH_KEY);
  pendingPath.value = '';
  await goSpa(router, path, hash || undefined);
  statusMsg.value = `已前往 ${item.label}`;
  setTimeout(() => { if (statusMsg.value.startsWith('已前往')) statusMsg.value = ''; }, 2000);
}

async function onExternalClick(item) {
  statusMsg.value = '探測中…';
  const err = await openExternal(item.href, item.probe, item.startHint);
  statusMsg.value = err || `已開啟 ${item.label}`;
  if (!err) setTimeout(() => { if (statusMsg.value.startsWith('已開啟')) statusMsg.value = ''; }, 2000);
}
</script>

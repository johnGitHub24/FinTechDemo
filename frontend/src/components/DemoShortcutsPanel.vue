<template>
  <aside id="demo-shortcuts" class="card demo-shortcuts-panel" aria-label="Demo 快捷入口">
    <div class="demo-shortcuts-head">
      <h2>{{ title }}</h2>
      <button v-if="collapsible" type="button" class="secondary sm" @click="open = !open">
        {{ open ? '收合' : '展開' }}
      </button>
    </div>

    <div v-show="!collapsible || open">
      <p class="muted small demo-shortcuts-lead">
        <template v-if="loggedIn">
          點 Trade／Portal／藍圖＝同頁切換（已帶 JWT）。
          Audit 需用 <code>admin</code> 登入。觀測按鈕會先探測埠。
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
  </aside>
</template>

<script setup>
/**
 * 【職責】Demo 快捷：SPA 同頁導向；外部先探測再開；可被頂欄「Demo 快捷」展開。
 * 【技巧】expose expand()；hash 用 goSpa；Admin 不足給明確 statusMsg。
 * 【概念】禁止用 target=_blank 開 /trade（新分頁像「沒登入」）。
 */
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { loginDemoGroups, NEXT_PATH_KEY } from '../config/demoLinks';
import { goSpa, openExternal } from '../utils/demoNav';

const props = defineProps({
  title: { type: String, default: 'Demo 快捷入口' },
  collapsible: { type: Boolean, default: false },
  initiallyOpen: { type: Boolean, default: true }
});

defineExpose({
  expand() {
    open.value = true;
  }
});

const router = useRouter();
const auth = useAuthStore();
const open = ref(props.initiallyOpen);
const groups = loginDemoGroups;
const pendingPath = ref(sessionStorage.getItem(NEXT_PATH_KEY) || '');
const statusMsg = ref('');

const loggedIn = computed(() => !!auth.isLoggedIn);
const isAdmin = computed(() => {
  const roles = auth.roles || [];
  return roles.includes('ROLE_ADMIN') || roles.includes('ADMIN');
});

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
  return item.spaPath || item.href || item.label;
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

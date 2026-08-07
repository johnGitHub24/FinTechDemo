<template>
  <aside class="k8s-verify" :class="{ open: panelOpen }" aria-label="K8s 驗證指令">
    <div class="k8s-verify-bar">
      <button type="button" class="k8s-verify-toggle" @click="panelOpen = !panelOpen">
        {{ panelOpen ? '收合 K8s 驗證指令' : 'K8s 驗證指令' }}
      </button>
      <button type="button" class="secondary sm" @click.stop="copyAll" :title="copyHint">
        {{ copied ? '已複製' : '一鍵複製全部' }}
      </button>
    </div>

    <div v-show="panelOpen" class="k8s-verify-body">
      <p class="k8s-verify-lead muted small">
        PowerShell 貼上即可。期望：<code>ok</code>／<code>Ready</code>／四 Pod Running。
        {{ browserNote }}
      </p>

      <section v-for="g in groups" :key="g.title" class="k8s-verify-group">
        <h3>{{ g.title }}</h3>
        <ul>
          <li v-for="item in g.items" :key="item.cmd">
            <div class="k8s-cmd-row">
              <code class="k8s-cmd">{{ item.cmd }}</code>
              <button type="button" class="secondary sm" @click="copyOne(item.cmd)">複製</button>
            </div>
            <p class="k8s-expect muted small">→ {{ item.expect }}</p>
          </li>
        </ul>
      </section>
    </div>
  </aside>
</template>

<script setup>
/**
 * 【職責】網頁按鈕／面板：展示並複製本機 K8s 測試驗證指令。
 * 【技巧】指令來源 k8sVerify.js；clipboard 失敗時 fallback 選取提示。
 * 【概念】kubectl 需 kubeconfig；瀏覽器匿名打 API＝403 不代表叢集壞。
 */
import { ref } from 'vue';
import {
  K8S_BROWSER_NOTE,
  K8S_VERIFY_SCRIPT,
  k8sVerifyGroups
} from '../config/k8sVerify';

const props = defineProps({
  /** 進入頁面時是否預設展開 */
  initiallyOpen: { type: Boolean, default: false }
});

const panelOpen = ref(props.initiallyOpen);
const copied = ref(false);
const copyHint = ref('複製完整 PowerShell 腳本');
const groups = k8sVerifyGroups;
const browserNote = K8S_BROWSER_NOTE;

async function writeClipboard(text) {
  try {
    await navigator.clipboard.writeText(text);
    return true;
  } catch {
    return false;
  }
}

async function copyAll() {
  const ok = await writeClipboard(K8S_VERIFY_SCRIPT.trim() + '\n');
  copied.value = ok;
  copyHint.value = ok ? '已複製到剪貼簿' : '複製失敗，請手動選取下方指令';
  if (ok) setTimeout(() => { copied.value = false; }, 2000);
  if (!panelOpen.value) panelOpen.value = true;
}

async function copyOne(cmd) {
  await writeClipboard(cmd);
}
</script>

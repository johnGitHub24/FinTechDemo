<template>
  <div class="login-page">
    <div class="card login-card">
      <h1>登入</h1>
      <p class="muted">trader1＝對帳（85000＋AAPL）；admin＝全站監察（本人 100000、無持倉）。密碼 password</p>
      <label>帳號</label>
      <input v-model="username" autocomplete="username" />
      <label>密碼</label>
      <div class="password-field">
        <input
          v-model="password"
          :type="showPassword ? 'text' : 'password'"
          autocomplete="current-password"
          @keyup.enter="onSubmit"
        />
        <button
          type="button"
          class="password-toggle"
          :aria-pressed="showPassword"
          :aria-label="showPassword ? '隱藏密碼' : '顯示密碼'"
          :title="showPassword ? '隱藏密碼' : '顯示密碼'"
          @click="showPassword = !showPassword"
        >
          <!-- 簡化眼睛圖示：開＝可見，關＝隱藏 -->
          <svg v-if="!showPassword" viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
            <path fill="currentColor" d="M12 5c-5 0-9.3 3.1-11 7 1.7 3.9 6 7 11 7s9.3-3.1 11-7c-1.7-3.9-6-7-11-7zm0 12a5 5 0 1 1 0-10 5 5 0 0 1 0 10zm0-8a3 3 0 1 0 0 6 3 3 0 0 0 0-6z"/>
          </svg>
          <svg v-else viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
            <path fill="currentColor" d="M3.1 4.5 4.5 3.1 20.9 19.5 19.5 20.9l-3.2-3.2A11.6 11.6 0 0 1 12 19c-5 0-9.3-3.1-11-7a12.5 12.5 0 0 1 4.4-5.1L3.1 4.5zM12 7a5 5 0 0 1 4.9 6.1l-1.6-1.6A3 3 0 0 0 12 9V7zm9.9 5c-.5 1.1-1.2 2.1-2.1 3l-1.5-1.5c.6-.7 1.1-1.5 1.4-2.5-1.7-3.9-6-7-11-7-.8 0-1.5.1-2.2.2L5.3 2.9C7.3 2.3 9.6 2 12 2c5 0 9.3 3.1 11 7z"/>
          </svg>
        </button>
      </div>
      <button type="button" @click="onSubmit" :disabled="loading">登入</button>
      <p v-if="error" class="error">{{ error }}</p>

      <ServiceStatusPanel />

      <p class="login-blueprint-link">
        <router-link to="/blueprint">系統運作藍圖</router-link>
        <span> — 可不登入</span>
      </p>
    </div>

    <DemoShortcutsPanel />
  </div>
</template>

<script setup>
/**
 * 【職責】登入 + 服務燈號（可一鍵確保 UP）+ Demo 快捷（登入後導向）。
 * 【技巧】登入成功讀 NEXT_PATH_KEY，進使用者先前點的 Trade／Portal／Audit。
 * 【概念】燈號 DOWN＝bootRun 沒開；用 doctor-demo -Fix／開啟Demo.cmd 拉起。
 */
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { login } from '../api/client';
import { useAuthStore } from '../stores/auth';
import { NEXT_PATH_KEY } from '../config/demoLinks';
import ServiceStatusPanel from '../components/ServiceStatusPanel.vue';
import DemoShortcutsPanel from '../components/DemoShortcutsPanel.vue';

const router = useRouter();
const auth = useAuthStore();
const username = ref('trader1');
const password = ref('password');
const showPassword = ref(false);
const error = ref('');
const loading = ref(false);

async function onSubmit() {
  error.value = '';
  loading.value = true;
  try {
    const data = await login(username.value.trim(), password.value);
    auth.setSession({ token: data.token, username: data.username, roles: data.roles });
    const next = sessionStorage.getItem(NEXT_PATH_KEY) || '/trade';
    sessionStorage.removeItem(NEXT_PATH_KEY);
    router.push(next);
  } catch (e) {
    error.value = e.response?.data?.error || '登入失敗（請確認 Order :8081 已 UP）';
  } finally {
    loading.value = false;
  }
}
</script>

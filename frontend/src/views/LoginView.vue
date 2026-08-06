<template>
  <div class="card" style="max-width:420px;margin:3rem auto">
    <h1>登入</h1>
    <p style="color:#8b9cb3">trader1 / admin · 密碼 password</p>
    <label>帳號</label>
    <input v-model="username" autocomplete="username" />
    <label>密碼</label>
    <input v-model="password" type="password" autocomplete="current-password" @keyup.enter="onSubmit" />
    <button type="button" @click="onSubmit" :disabled="loading">登入</button>
    <p v-if="error" class="error">{{ error }}</p>
    <p class="login-blueprint-link">
      <router-link to="/blueprint">系統運作藍圖</router-link>
      <span> — 技術架構與運作過程（可不登入）</span>
    </p>
  </div>
</template>

<script setup>
/**
 * 【職責】提供帳號密碼登入表單與登入失敗提示。
 * 【頁面角色】訪客入口；已登入者會由 router 守衛導往交易前台。
 * 【與後端關係】呼叫 /api/auth/login，並將後端回傳的 JWT 與角色交給認證 store。
 */
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { login } from '../api/client';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const auth = useAuthStore();
const username = ref('trader1');
const password = ref('password');
const error = ref('');
const loading = ref(false);

/**
 * 【目的】送出登入資料並在成功後進入交易前台。
 * 【副作用】發出登入 HTTP 請求；成功時寫入 localStorage（透過 auth.setSession）並切換路由，
 * 失敗時將後端錯誤訊息顯示在頁面上。
 */
async function onSubmit() {
  error.value = '';
  loading.value = true;
  try {
    const data = await login(username.value.trim(), password.value);
    auth.setSession({ token: data.token, username: data.username, roles: data.roles });
    router.push('/trade');
  } catch (e) {
    error.value = e.response?.data?.error || '登入失敗';
  } finally {
    loading.value = false;
  }
}
</script>

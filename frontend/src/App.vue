<template>
  <div class="app-shell">
    <nav v-if="auth.isLoggedIn" class="app-nav">
      <span class="brand">FinTech<span>Demo</span></span>
      <router-link to="/trade">交易前台</router-link>
      <router-link to="/blueprint">系統運作藍圖</router-link>
      <router-link to="/portal">會員後台</router-link>
      <router-link v-if="isAdmin" to="/portal/audit">審計</router-link>
      <span class="user-chip">{{ auth.username }}</span>
      <button class="secondary sm" type="button" @click="logout">登出</button>
    </nav>
    <main class="page">
      <router-view />
    </main>
  </div>
</template>

<script setup>
/**
 * 【職責】應用程式根元件，提供登入後的導覽列與路由頁面容器。
 * 【頁面角色】依使用者角色顯示交易、會員後台與管理員審計入口。
 * 【與後端關係】透過認證 store 的 JWT／角色資訊決定畫面，不直接呼叫後端。
 */
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from './stores/auth';

const auth = useAuthStore();
const router = useRouter();
const isAdmin = computed(() => (auth.roles || []).includes('ROLE_ADMIN') || auth.roles?.includes('ADMIN'));

/**
 * 【目的】結束目前登入工作階段並回到登入頁。
 * 【副作用】呼叫 store 清除記憶體與 localStorage 的認證資料，接著改變前端路由。
 */
function logout() {
  auth.clearSession();
  router.push('/login');
}
</script>

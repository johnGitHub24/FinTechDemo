/**
 * 【職責】定義 SPA 頁面路由與登入、角色授權守衛。
 * 【主要匯出】已設定的 Vue Router 實例。
 * 【與後端關係】守衛依認證 store 保存的 JWT 與後端登入回傳角色，控制可進入的畫面。
 */
import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import LoginView from '../views/LoginView.vue';
import TradeView from '../views/TradeView.vue';
import PortalView from '../views/PortalView.vue';
import AuditView from '../views/AuditView.vue';
import BlueprintView from '../views/BlueprintView.vue';
import { scrollToId } from '../utils/jqueryDom';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/trade' },
    { path: '/login', name: 'login', component: LoginView, meta: { guest: true } },
    { path: '/blueprint', name: 'blueprint', component: BlueprintView },
    { path: '/trade', name: 'trade', component: TradeView, meta: { requiresAuth: true } },
    { path: '/portal', name: 'portal', component: PortalView, meta: { requiresAuth: true } },
    { path: '/portal/history', redirect: '/portal' },
    { path: '/portal/positions', redirect: '/portal' },
    { path: '/portal/audit', name: 'audit', component: AuditView, meta: { requiresAuth: true, admin: true } }
  ]
});

/**
 * 【目的】在頁面切換前驗證登入狀態與管理員角色。
 * 【副作用】未登入者會被重新導向登入頁；已登入者不可再進入登入頁；
 * 非管理員造訪審計頁時會被導回會員後台。
 */
router.beforeEach((to) => {
  const auth = useAuthStore();
  const loggedIn = !!auth.isLoggedIn;
  if (to.meta.requiresAuth && !loggedIn) {
    if (to.path && to.path !== '/login') {
      sessionStorage.setItem('fintech_demo_next_path', to.path);
    }
    return '/login';
  }
  if (to.meta.guest && loggedIn) return '/trade';
  if (to.meta.admin) {
    const roles = auth.roles || [];
    if (!roles.includes('ROLE_ADMIN') && !roles.includes('ADMIN')) return '/portal';
  }
});

router.afterEach((to) => {
  if (!to.hash) return;
  const id = to.hash.replace(/^#/, '');
  requestAnimationFrame(() => {
    scrollToId(id);
  });
});

export default router;

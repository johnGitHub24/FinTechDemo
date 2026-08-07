/**
 * 【職責】提供全應用共用的登入工作階段狀態。
 * 【主要匯出】useAuthStore composable，以及登入狀態的讀取與更新操作。
 * 【與後端關係】保存後端登入 API 回傳的 JWT、使用者名稱與角色，不自行驗證 Token。
 */
import { reactive } from 'vue';

// 【概念】固定鍵名讓重新整理頁面後可從 localStorage 還原登入工作階段。
const TOKEN_KEY = 'fintech_demo_token';
const USER_KEY = 'fintech_demo_user';
const ROLES_KEY = 'fintech_demo_roles';

const state = reactive({
  token: localStorage.getItem(TOKEN_KEY) || '',
  username: localStorage.getItem(USER_KEY) || '',
  roles: JSON.parse(localStorage.getItem(ROLES_KEY) || '[]')
});

/**
 * 【目的】以登入 API 的回應建立目前使用者工作階段。
 * 【副作用】同步更新 Vue 響應式狀態，並把 Token、帳號及角色寫入 localStorage。
 */
function setSession({ token, username, roles }) {
  state.token = token;
  state.username = username;
  state.roles = roles || [];
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, username);
  localStorage.setItem(ROLES_KEY, JSON.stringify(state.roles));
}

/**
 * 【目的】清除目前使用者的登入工作階段。
 * 【副作用】重設 Vue 響應式狀態，並移除 localStorage 中的 Token、帳號與角色；
 * API client 在收到非登入請求的 401 時也會呼叫此操作。
 */
function clearSession() {
  state.token = '';
  state.username = '';
  state.roles = [];
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  localStorage.removeItem(ROLES_KEY);
}

/**
 * 【目的】取得共用認證狀態與可操作工作階段的 actions。
 * 【副作用】本函式本身不寫入資料；回傳的 setSession／clearSession 會更新 localStorage。
 */
export function useAuthStore() {
  return {
    get token() { return state.token; },
    get username() { return state.username; },
    get roles() { return state.roles; },
    /** 布林 getter（勿回傳 ComputedRef，避免模板誤判為永遠 truthy） */
    get isLoggedIn() { return !!state.token; },
    setSession,
    clearSession
  };
}

import axios from 'axios';
import { useAuthStore } from '../stores/auth';
import { useDemoStoryStore } from '../stores/demoStory';

/**
 * 【職責】集中封裝前端呼叫後端 REST API 的方式。
 * 【主要匯出】登入、訂單、帳戶、持倉、審計與市場標的查詢函式。
 * 【與後端關係】所有請求以 /api 開頭，開發時由 Vite proxy 轉送至 Spring Boot 後端。
 */
const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' }
});

/**
 * 【目的】若回應含 demoTrace，寫入 Demo 故事 store 供 PROCESS FLOW 面板使用。
 */
function captureTrace(data) {
  if (data?.demoTrace) {
    useDemoStoryStore().setTrace(data.demoTrace);
  }
  return data;
}
/**
 * 【目的】在每個 API 請求送出前，自動附加目前登入者的 JWT Bearer Token。
 * 【副作用】讀取認證 store；未登入時不寫入 Authorization 標頭。
 */
api.interceptors.request.use((config) => {
  const auth = useAuthStore();
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`;
  }
  return config;
});

/**
 * 【目的】集中處理後端回傳的未授權（401）情況。
 * 【副作用】非登入 API 收到 401 時會清除 localStorage 中的工作階段，
 * 並把瀏覽器導向登入頁；錯誤仍會交回原呼叫端處理。
 */
api.interceptors.response.use(
  (r) => r,
  (error) => {
    const status = error.response?.status;
    const url = error.config?.url || '';
    const isLogin = url.includes('/auth/login');
    if (status === 401 && !isLogin) {
      useAuthStore().clearSession();
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

/** 【目的】以帳號密碼呼叫後端登入 API，回傳 JWT 與使用者資訊。 */
export async function login(username, password) {
  const { data } = await api.post('/auth/login', { username, password });
  return data;
}

/** 【目的】依分頁或篩選參數取得目前使用者可見的訂單。 */
export async function fetchOrders(params = {}) {
  const { data } = await api.get('/orders', { params });
  return data;
}

/** 【目的】將下單表單資料送往後端，建立一筆 PENDING 訂單。 */
export async function createOrder(payload) {
  const { data } = await api.post('/orders', payload);
  return captureTrace(data);
}

/** 【目的】要求後端執行指定訂單；成交結果與風控規則由後端決定。 */
export async function executeOrder(id) {
  const { data } = await api.post(`/orders/${id}/execute`);
  return captureTrace(data);
}

/** 【目的】要求後端取消指定訂單。 */
export async function cancelOrder(id) {
  const { data } = await api.delete(`/orders/${id}`);
  return captureTrace(data);
}

/** 【目的】取得 Demo 拓撲服務燈（order 代 ping health）。 */
export async function fetchTopology() {
  const { data } = await api.get('/demo/topology');
  return data;
}

/** 【目的】取得目前登入者的帳戶餘額與幣別。 */
export async function fetchAccount() {
  const { data } = await api.get('/accounts/me');
  return data;
}

/** 【目的】取得目前登入者的持倉清單。 */
export async function fetchPositions() {
  const { data } = await api.get('/positions');
  return data;
}

/** 【目的】以分頁或篩選參數取得後端提供的審計紀錄。 */
export async function fetchAuditLogs(params = {}) {
  const { data } = await api.get('/audit-logs', { params });
  return data;
}

/** 【目的】取得可下單的市場標的與其參考價格。 */
export async function fetchSymbols() {
  const { data } = await api.get('/market/symbols');
  return data;
}

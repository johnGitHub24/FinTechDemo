/**
 * 【職責】前端應用程式進入點。
 * 【主要匯出／頁面角色】建立 Vue 根實例、註冊路由，並載入全域樣式。
 * 【與後端關係】本檔不直接呼叫後端；頁面透過 router 與 API client 進行互動。
 */
import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import './styles.css';

createApp(App).use(router).mount('#app');

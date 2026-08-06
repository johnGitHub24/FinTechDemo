/**
 * 【職責】Vite 開發伺服器與前端建置設定。
 * 【主要匯出】Vue 外掛、開發埠、後端 API proxy 與 dist 輸出目錄。
 * 【與後端關係】開發時將 /api 請求代理到單體後端 :8081，
 * 或以 VITE_API_TARGET 指向分散式 Gateway（例如 :8080），避免瀏覽器跨來源問題。
 */
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  server: {
    // 【概念】前端開發伺服器固定使用 5173，啟動後直接開啟登入頁。
    port: 5173,
    open: '/login',
    proxy: {
      // 【技巧】保留 /api 路徑並轉送至後端；環境變數可切換為 Gateway。
      '/api': {
        target: process.env.VITE_API_TARGET || 'http://localhost:8081',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    emptyOutDir: true
  }
});

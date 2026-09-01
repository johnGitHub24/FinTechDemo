/**
 * 【職責】Vite 開發伺服器與前端建置設定。
 * 【主要匯出】Vue 外掛、開發埠、後端 API／Demo proxy。
 * 【與後端關係】/api → order；/proxy/* → 各服務 health 與 Demo API（免 CORS）。
 */
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    strictPort: true,
    host: '127.0.0.1',
    open: '/login',
    proxy: {
      '/api': {
        target: process.env.VITE_API_TARGET || 'http://localhost:8081',
        changeOrigin: true
      },
      '/proxy/order-health': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        rewrite: () => '/actuator/health'
      },
      '/proxy/risk-health': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        rewrite: () => '/actuator/health'
      },
      '/proxy/gateway-health': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: () => '/actuator/health'
      },
      // K8s：port-forward Gateway（VITE_API_TARGET 預設 http://127.0.0.1:18080）
      '/proxy/k8s-gateway-pf-health': {
        target: process.env.VITE_API_TARGET || 'http://127.0.0.1:18080',
        changeOrigin: true,
        rewrite: () => '/actuator/health'
      },
      '/proxy/job-health': {
        target: 'http://localhost:8083',
        changeOrigin: true,
        rewrite: () => '/actuator/health'
      },
      '/proxy/account-health': {
        target: 'http://localhost:8084',
        changeOrigin: true,
        rewrite: () => '/actuator/health'
      },
      // Demo Risk Check 頁：POST JSON
      '/proxy/risk-check': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        rewrite: () => '/api/risk/check'
      },
      // Demo Account Me：轉發 /api/**
      '/proxy/account-api': {
        target: 'http://localhost:8084',
        changeOrigin: true,
        rewrite: (p) => p.replace(/^\/proxy\/account-api/, '/api')
      }
    }
  },
  build: {
    outDir: 'dist',
    emptyOutDir: true
  }
});

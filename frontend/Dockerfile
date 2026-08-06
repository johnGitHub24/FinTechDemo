# =============================================================================
# FinTechDemo frontend — Vue 靜態站 Dockerfile
# 【職責】把 Vue 3 專案編成靜態檔，再用 nginx 提供 HTML／JS，並把 /api 反代到 gateway
# 【技巧】同樣是 multi-stage：node 負責 npm run build → nginx 只服務 dist
# 【概念】瀏覽器只跟「前端容器」講話；業務 API 經 nginx proxy_pass 進內網 gateway（同源／簡化 CORS）
# =============================================================================

# ---------------------------------------------------------------------------
# Stage 1：前端建置
# 【職責】安裝依賴並產出 Vite／Vue 的 dist/
# 【技巧】先 COPY package*.json 再 npm ci：依賴沒變時可重用 layer
# ---------------------------------------------------------------------------
FROM node:20-alpine AS build
WORKDIR /app

# 【技巧】package-lock.json* 的 *：沒有 lock 檔時也不讓 COPY 失敗（仍建議提交 lock）
COPY package.json package-lock.json* ./
# 【技巧】npm ci：依 lock 乾淨安裝，適合 CI／Docker（比 npm install 可重現）
RUN npm ci

COPY . .
# 【概念】build 產物通常在 /app/dist（Vite 預設）
RUN npm run build

# ---------------------------------------------------------------------------
# Stage 2：nginx 靜態托管 + 反代
# 【職責】listen 80；靜態檔 + /api → gateway（設定見同目錄 nginx.conf）
# 【技巧】把自訂 conf 蓋掉 nginx 預設 default.conf
# ---------------------------------------------------------------------------
FROM nginx:1.27-alpine

# 【技巧】路徑 /etc/nginx/conf.d/default.conf 是 alpine 官方映像慣用的 site config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 【技巧】只帶編譯結果，不帶 node_modules／原始碼
COPY --from=build /app/dist /usr/share/nginx/html

# 【概念】compose 常把主機 5173 映射到容器 80（見 docker-compose frontend ports）
EXPOSE 80

# FinTechDemo deploy（P10）

本機基礎設施：根目錄 `docker-compose.yml`（Redpanda :19092、Redis :6379）。

## 跑通規則／故障排除（必讀）

完整驗證分層、kind 連線 refused、Compose vs K8s、映像 load／apply 技巧：

→ **[`K8s跑通與驗證技巧`](k8s-tips.html)**

統一學習入口（設定／K8s／教學／API 連結）：

→ **[`docs/index.html`](../index.html)**

逐層打通到上線（S0→S6）：

→ **[`上線部署階段層次（MD）`](stages-doc.html)** · [互動 HTML](../portals/stages.html)

通用解法（為什麼這樣執行、故障→修復）：

→ **[`部署跑通 · Loop Engineering`](../portals/loop-guide.html)**

## K8s overlay（模組化示意）

Deployment＋Service：**gateway、order-service、risk-service、account-service**（三業務 MS + 入口）。

映像為 placeholder（`fintech-demo/*:local`）；本機 build 後再 apply。

```powershell
.\scripts\check-k8s.ps1
kubectl kustomize deploy/k8s/overlays/dev
kubectl apply -k deploy/k8s/overlays/dev
```

```text
deploy/k8s/
  base/
    gateway-* / order-* / risk-* / account-*
  overlays/dev/
```

架構說明：`docs/architecture.md`。

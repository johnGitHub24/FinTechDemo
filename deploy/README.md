# FinTechDemo deploy（P10）

本機基礎設施：根目錄 `docker-compose.yml`（Redpanda :19092、Redis :6379）。

## 文件（依完整度地圖）

| 主題 | 連結 |
|------|------|
| **文件完整度／單一真相** | [docs/文件完整度.md](../docs/文件完整度.md) |
| **Docker ↔ K8s 三層（Mermaid）** | http://localhost:5173/blueprint#k8s-intellij |
| K8s 故障排除 L0～L4 | [docs/deploy/k8s-tips.html](../docs/deploy/k8s-tips.html) |
| 統一學習入口 | [docs/index.html](../docs/index.html) |
| S0→S6 | [docs/deploy/stages-doc.html](../docs/deploy/stages-doc.html) |

## K8s 本機 Demo（L4，一鍵）

```powershell
# FinTechDemo 根目錄
.\demo\start-k8s-demo.ps1
# 或先檢查三層：.\demo\k8s-walkthrough.ps1

$env:KUBECONFIG = ".\demo\.tools\kubeconfig-kind-trading-local"
kubectl -n fintech-demo get pods
kubectl -n fintech-demo port-forward svc/gateway 18080:8080
```

平台設定（CPU 架構 auto）：`demo/platform-run.properties` · EOS：`EngineeringOS/eos-minimal/knowledge/k8s-local-docker-build.md`

## K8s overlay（manifest）

Deployment＋Service：**gateway、order-service、risk-service、account-service**。

映像 tag：`fintech-demo/*:local`（由 `start-k8s-demo.ps1` build／load）。

```powershell
.\demo\check-k8s.ps1
kubectl kustomize deploy/k8s/overlays/dev
kubectl apply -k deploy/k8s/overlays/dev
```

```text
deploy/k8s/
  base/
  overlays/dev/
```

架構：`docs/architecture/architecture.html`。

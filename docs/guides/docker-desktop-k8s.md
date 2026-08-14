# Docker Desktop 與 FinTechDemo K8s — 兩套「大樓」

> **互動圖**：http://localhost:5173/blueprint#k8s-intellij §②c  
> **前提**：`kubectl -n fintech-demo get pods` 四行 Running = Demo 已成功；Panel 找不到 namespace **不是失敗**。

---

## 核心原因：兩套 K8s「大樓」

```text
┌─────────────────────────────┐     ┌─────────────────────────────┐
│ A · Docker Desktop 內建 K8s  │     │ B · FinTechDemo kind 叢集    │
│ context: docker-desktop      │     │ context: kind-trading-local  │
│ 節點: desktop-control-plane  │     │ 節點: trading-local-control-plane │
│ namespace: default, kube-system… │ │ namespace: fintech-demo ← 4 Pod │
└─────────────────────────────┘     └─────────────────────────────┘
         ↑                                      ↑
   Kubernetes Panel 預設連這裡              kubectl / walkthrough 連這裡
```

| | A 棟 · Desktop 內建 | B 棟 · FinTechDemo kind |
|--|---------------------|-------------------------|
| context | `docker-desktop` | `kind-trading-local` |
| 節點 | `desktop-control-plane` | `trading-local-control-plane` |
| namespace | `default`、`kube-system`… | **`fintech-demo`** |
| 驗收入口 | Panel（常空） | `kubectl`、`k8s-walkthrough.ps1` |

### Panel 看到的 vs 實際

| Panel namespace 下拉 | 實際 |
|----------------------|------|
| `kube-public`、`kube-system`、`default`… | 連到 **A 棟** 內建空叢集 |
| **沒有** `fintech-demo` | Pod 在 **B 棟** kind，不在 Panel 預設那棟 |

`fintech-demo` 是 `kubectl apply -k deploy/k8s/overlays/dev` 建在 **B 棟** 上的，**不是** Settings 裡某個開關能「生出來」的。

---

## Settings 裡為什麼也沒有 fintech-demo？

**Settings → Kubernetes** 只管：

- 要不要 **Enable Kubernetes**（Desktop **自己**那套 A 棟）
- Reset、記憶體／CPU

**不會**列出：

- 專案 namespace（`fintech-demo`）
- kind 的 Deployment／Pod
- FinTechDemo 的 YAML

那些在：

| 類型 | 路徑 |
|------|------|
| namespace | `deploy/k8s/base/namespace.yaml` |
| 四服務 manifest | `deploy/k8s/base/*-deployment.yaml` |
| dev overlay | `deploy/k8s/overlays/dev/kustomization.yaml` |
| 一鍵腳本 | `demo/start-k8s-demo.ps1` |

---

## Panel 各分頁 vs Kubernetes Panel

| Docker Desktop 分頁 | FinTechDemo |
|---------------------|-------------|
| **Images** | ✓ 4× `fintech-demo/*:local` |
| **Containers** | ✓ `trading-local-control-plane`（B 棟 kind 節點） |
| **Builds** | ✓ `Dockerfile.k8s-local` 建置紀錄 |
| **Kubernetes Panel** | ✗ 常連 **A 棟**，看不到 `fintech-demo` |

**映像在 Desktop、Pod 在 kind 裡面** — 正常架構，不是少設 namespace。

---

## 一句話

```text
Kubernetes Panel  =  A 棟內建 K8s 接待處
fintech-demo 四 Pod =  B 棟 kind-trading-local
kubectl walkthrough =  正確驗收入口 ✓
```

---

## 正確驗證（重開機後乾淨跑）

```powershell
cd FinTechDemo
.\demo\start-k8s-demo.ps1 -RecreateCluster
.\demo\k8s-walkthrough.ps1
$env:KUBECONFIG = ".\demo\.tools\kubeconfig-kind-trading-local"
kubectl -n fintech-demo get pods
```

---

## Panel 若一定要試

1. Panel **Cluster** 下拉 → 若有 **`kind-trading-local`** → 選 B 棟
2. **Namespace** → **`fintech-demo`**
3. 若只有 `desktop-control-plane`、改不了 → **用 kubectl**（或 [IntelliJ Kubernetes](./intellij-k8s.md)）

---

## 「Waiting for node to be ready」卡住（A 棟 Enable 時）

| 步驟 | 動作 |
|------|------|
| 1 | Settings → Resources：Memory **≥ 8 GB** |
| 2 | 首次 Enable 等 **10～15 分鐘** |
| 3 | 仍卡 → Reset Kubernetes Cluster → Restart Desktop |
| 4 | **只做 FinTechDemo** → 可 **關閉** Enable Kubernetes，只跑 `start-k8s-demo.ps1` |

```powershell
Remove-Item Env:KUBECONFIG -ErrorAction SilentlyContinue
kubectl get nodes --context docker-desktop
```

`desktop-control-plane Ready` = A 棟修好（仍與 B 棟 fintech-demo 無關）。

---

## 相關文件

- **[K8s 完整教學（總入口）](./k8s-complete-guide.html)**
- [IntelliJ 連 K8s](./intellij-k8s.md)
- [k8s-tips.html](../deploy/k8s-tips.html)
- [文件完整度.md](../文件完整度.md)

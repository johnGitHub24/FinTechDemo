# IntelliJ 連上 FinTechDemo K8s（kind-trading-local）

> **互動版（複製指令）**：http://localhost:5173/blueprint#k8s-intellij §⑩  
> **前提**：已跑 `.\demo\start-k8s-demo.ps1`，終端機 `kubectl -n fintech-demo get pods` 為 4× Running。

## 常見誤會：Docker 插件 ≠ Kubernetes 插件

| 位置 | 能不能選 context / 看 Pod |
|------|----------------------------|
| Services → **Docker** | ✗ 只有 Images／Containers／Compose |
| Settings → **Kubernetes** | ✓ 設 kubeconfig、選 context |
| Services → **Kubernetes** | ✓ 看 namespace、Pod、log |

Docker Desktop **Kubernetes Panel** 常連 `docker-desktop` 空叢集，**看不到** `fintech-demo` — 以 `kubectl` 或 IntelliJ **Kubernetes** 插件為準。

---

## 1. 確認插件

**File → Settings → Plugins** → 搜尋 **Kubernetes** → **Installed** 且 **Enabled**（僅 Docker 不夠）。

---

## 2. 設 kubeconfig（最重要）

**File → Settings → Build, Execution, Deployment → Kubernetes**

**Kubeconfig** 按 **+** 新增（專案根相對路徑）：

```text
demo\.tools\kubeconfig-kind-trading-local
```

Windows 絕對路徑範例：

```text
D:\SouceDemo\RemoteSpringBoot\FinTechDemo\demo\.tools\kubeconfig-kind-trading-local
```

可一併保留 `C:\Users\<你>\.kube\config`。**Apply → OK**。

---

## 3. Services 加 Kubernetes

1. **View → Tool Windows → Services**
2. 左上角 **+** → **Kubernetes**（不是 Docker Compose）
3. 若空白 → 右鍵 → **Configure Kubernetes…** → 確認 kubeconfig 含上一步路徑

展開應類似：

```text
Kubernetes
 └─ kind-trading-local
     └─ fintech-demo
         ├─ gateway
         ├─ order-service
         ├─ risk-service
         └─ account-service
```

在 **fintech-demo** 右鍵 → **Set as Current Namespace**。

---

## 4. 仍只有 docker-desktop

1. **Settings → Kubernetes** → **Context** 改 **kind-trading-local**
2. Services → Kubernetes 節點旁 **Refresh**

---

## 5. 終端機自測（叢集還活著）

```powershell
cd FinTechDemo
$env:KUBECONFIG = ".\demo\.tools\kubeconfig-kind-trading-local"
kubectl config use-context kind-trading-local
kubectl -n fintech-demo get pods
```

四行 `Running 1/1` = 叢集正常，IDE 只差連線設定。

---

## 6. 相關檔案

| 檔案 | 用途 |
|------|------|
| `demo/.tools/kubeconfig-kind-trading-local` | kind 叢集連線（IntelliJ 指這裡） |
| `demo/platform-run.properties` | `K8S_CONTEXT`、`K8S_NAMESPACE` |
| `deploy/k8s/base/namespace.yaml` | 建立 `fintech-demo` namespace |

---

## 仍看不到 kind-trading-local 時請自查

1. Settings → Kubernetes 的 **Kubeconfig** 有哪些路徑？
2. Services → **Kubernetes**（不是 Docker）展開後顯示什麼？

故障排除詳見 [k8s-tips.html](../deploy/k8s-tips.html)。

# 面試 Demo — 簡易版（10 分鐘）

> 完整藍圖導覽：[blueprint-tour.md](./blueprint-tour.md) · 互動頁：http://localhost:5173/blueprint

---

## 一句話開場

「迷你券商 Demo：JWT 登入、前台下單、Feign 風控、後台查詢；日常 bootRun，進階用 **kind 四 Pod** 證明能部署。」

---

## 事前準備（面試前 30 分鐘）

```powershell
cd FinTechDemo
docker desktop start
.\scripts\check.ps1
.\demo\start-k8s-demo.ps1 -SkipBuild    # 或 -RecreateCluster 求乾淨
.\demo\k8s-walkthrough.ps1
```

另開（簡報用前端＋藍圖）：

```powershell
cd frontend
npm run dev
```

| 網址 | 用途 |
|------|------|
| http://127.0.0.1:5173/blueprint | 架構簡報 |
| http://127.0.0.1:5173/login | 業務 Demo（需 bootRun 或 port-forward） |

帳密：**trader1 / password**

---

## 簡報 3 張投影片（口條）

### ① 做什麼

- 前台：登入 → 下單 → 成交／風控拒絕
- 後台：查自己的訂單、餘額（RBAC）
- 後端：Gateway / Order / Risk / Account 微服務

### ② 怎麼跑

| 模式 | 指令 | 給誰看 |
|------|------|--------|
| 日常 | `.\開啟Demo.cmd` | 業務流程 |
| K8s | `.\demo\start-k8s-demo.ps1` | 維運／部署 |

### ③ 亮點

- `scripts/check.ps1` 測試綠燈
- `deploy/k8s/` + kustomize
- Docker 映像 → kind → namespace `fintech-demo`

---

## Live Demo 腳本（8 分鐘）

### A. 業務（3 分）— 有 bootRun 時

1. 開 http://127.0.0.1:5173/login → 登入
2. 建單 → **PENDING** → 成交 → **ACCEPTED**（或 REJECTED）
3. 指右側 **S2**（Order + Risk 綠燈）

**口條：** 「成交時 Order 用 OpenFeign 呼叫 Risk :8082。」

### B. K8s（4 分）— 你已跑通

```powershell
.\demo\k8s-walkthrough.ps1
```

講三層：**Images → kind load → 4 Pod Running**

```powershell
$env:KUBECONFIG = ".\demo\.tools\kubeconfig-kind-trading-local"
kubectl -n fintech-demo port-forward svc/gateway 18080:8080
```

瀏覽器：http://localhost:18080/actuator/health → **UP**

**口條（防問倒）：** 「Docker Desktop Panel 連 `docker-desktop` 內建叢集；Demo Pod 在 **kind-trading-local / fintech-demo**，所以 Panel 看不到 namespace 是正常的。」

### C. 藍圖（1 分）

開 http://127.0.0.1:5173/blueprint#layers → `#flow` → `#k8s-intellij`

---

## 結尾（20 秒）

「產品、微服務、容器、K8s、工程規範串在一條鏈；後續可接 CI 建映像、GitOps。」

---

## 翻車備援

- 秀 walkthrough **截圖** + 藍圖 Mermaid
- 口述：`demo/start-k8s-demo.ps1`、`deploy/k8s/overlays/dev`

---

## 常問短答

| 問 | 答 |
|----|-----|
| 為何不用 Eureka？ | 教學版固定 URL，先跑穩交易鏈 |
| Gateway？ | 統一入口 + 限流；最短可直連 Order |
| Panel 沒 fintech-demo？ | 兩套 K8s 大樓，見 blueprint §②c |

# FinTechDemo — K8s 跑通規則與故障排除技巧

> **目的**：開發要把問題解掉時，先分清「本專案強制驗證」與「本機真叢集跑通」兩層，避免把死掉的 `kind` context 誤當成專案失敗。  
> **對照**：真叢集一鍵腳本在 `TradingKubernetes/scripts/start-local.ps1`（叢集名 `trading-local`）。  
> **上線階梯（S0→S6 逐層打通）**：[`上線部署階段層次（MD）`](stages-doc.html) · [`上線部署階段層次（互動）`](../portals/stages.html)  
> **通用解法（Loop Engineering 教學指導）**：[`部署跑通-LoopEngineering教學指導.html`](部署跑通-LoopEngineering教學指導.html)

---

## 1. 驗證分層（先記這張表）

| 層級 | 指令 | 通過標準 | 要不要 Docker／kind |
|------|------|----------|---------------------|
| **L0 單元＋整合** | `.\gradlew.bat check` 或 `.\scripts\check.ps1` | BUILD SUCCESSFUL | 否 |
| **L1 Compose 語法** | `docker compose config` | exit 0 | 需有 `docker` CLI（可不開引擎） |
| **L2 K8s 清單（強制）** | `.\scripts\check-k8s.ps1` | `kubectl kustomize` 成功且含 account／order／risk／gateway | **只要 kubectl**；**不連 API server** |
| **L3 分散式本機（推薦日常）** | `.\scripts\verify-pipeline.ps1 -Up`（可加 `-Smoke`） | compose up 健康 + 可選 API 煙霧 | **Docker 引擎必須 Ready** |
| **L4 真 K8s apply（加分／進階）** | build 映像 → `kubectl apply -k deploy/k8s/overlays/dev` | Pod Ready、`/actuator/health` | Docker + **活著的** kind／其他叢集 |

**規則（客服口吻記住）：**

1. **CI／日常綠燈**以 L0～L2 為準；`check-k8s.ps1` **只 render YAML**，不證明叢集活著。  
2. **要「整個服務跑通」**：優先 L3（compose），成本低、與 `docker-compose.yml` 一致。  
3. **要「K8s 敘事／GitOps 示意」**：才上 L4；前置必須先修好 kubeconfig 指到活 API。  
4. 出現 `dial tcp 127.0.0.1:xxxxx: connection refused` → **先查叢集／Docker，不要改業務 Java**。  
5. **權威步驟見 SPEC §3.1**；前端頁頂亦有相同提醒。

---

## 2. 本專案 K8s 產物長什麼樣

```text
deploy/k8s/
  base/           # Deployment + Service（gateway／order／risk／account）
  overlays/dev/   # replicas=1、image tag=local
```

- 映像名：`fintech-demo/<module>:local`（placeholder；apply 前要本機 build／load）。  
- 驗證腳本：`scripts/check-k8s.ps1` → `kubectl kustomize deploy/k8s/overlays/dev`。  
- 入口說明：`deploy/README.md`。

**L2 通過技巧：**

```powershell
cd D:\ClaudeCode\FinTechDemo
.\scripts\check-k8s.ps1
# 輸出 YAML 中應看到：account-service、order-service、risk-service、gateway
```

無 `kubectl` 時腳本會黃字 SKIP 並 **exit 0**（示意產物不強制本機裝 kubectl）——正式開發機仍建議裝好 kubectl。

---

## 3. Pipeline 一條龍（官方）

```powershell
# 語法＋測試＋kustomize（不啟動容器）
.\scripts\verify-pipeline.ps1

# 同上 + 起核心分散式（Redpanda/Redis + 三 MS + gateway）
.\scripts\verify-pipeline.ps1 -Up

# 再打 API 煙霧
.\scripts\verify-pipeline.ps1 -Up -Smoke
```

對應內部步驟：`check` → `docker compose config` → `check-k8s.ps1` →（可選）`compose up` →（可選）`smoke-distributed.ps1`。

---

## 4. 本機真叢集跑通（L4）— 技巧方案

### 4.1 前置檢查清單

| 檢查 | 指令／現象 | 期望 |
|------|------------|------|
| Docker 引擎 | `docker info` 出現 **Server Version** | Client-only 不夠 |
| kind 可用 | `kind version` 或 `TradingKubernetes\tools\kind.exe` | PATH 或專案 tools |
| kubectl context | `kubectl config current-context` | 指到**活的**叢集 |
| API | `kubectl get --raw=/readyz` | exit 0／`ok` |

**網頁入口（複製指令）**：前端 [系統運作藍圖 → K8s 驗證](http://localhost:5173/blueprint#k8s-verify)（登入頁／nav「K8s 指令」同錨點）。

### 4.1b 建議驗證指令（貼 PowerShell）

```powershell
docker info
kubectl config current-context
kubectl get --raw=/readyz
kubectl get nodes
kubectl get all -n fintech-demo
kubectl get pods -A
```

期望：`readyz=ok`、node Ready、`fintech-demo` 內 gateway／order／risk／account 皆 Running。

### 4.2 本案已發生過的錯誤（保留解法）

**症狀：**

```text
couldn't get current server API group list:
Get "https://127.0.0.1:56525/api?timeout=32s": connectex: ... actively refused
Unable to connect to the server: dial tcp 127.0.0.1:56525 ...
```

**根因（本機實測）：**

1. `kubectl` current-context = `kind-trading-local`  
2. API 位址卡在舊 port `127.0.0.1:56525`（kind 死掉／重開後 port 會變）  
3. Docker Desktop 引擎未就緒（`docker ps` 打不開 `dockerDesktopLinuxEngine` pipe）  
4. 系統 PATH **沒有** `kind`（需用 `TradingKubernetes\tools\kind.exe` 或自行安裝）

**解法步驟（依序）：**

```powershell
# ① 先讓 Docker 引擎活起來
docker desktop restart   # 或 UI → Troubleshoot → Restart
# 等到：
docker info              # 必須看到 Server Version

# ② 用 TradingKubernetes 重建／修復 kind（叢集名 trading-local）
cd D:\ClaudeCode\TradingKubernetes
.\scripts\start-local.ps1 -RecreateCluster -SkipMvp -SkipBuild
# 腳本會：下載／使用 tools\kind.exe、create/recreate、export kubeconfig、use-context

# ③ 確認 API
kubectl config use-context kind-trading-local
kubectl get --raw=/readyz
kubectl cluster-info
```

**若暫時不做 K8s、只想消錯誤噪音：**

```powershell
# 不要打會連 API 的指令；L2 用 check-k8s（只 kustomize）即可
.\scripts\check-k8s.ps1
```

或改用其他可用 context（有的話）：`kubectl config get-contexts`。

### 4.3 映像進叢集再 apply（FinTechDemo）

```powershell
cd D:\ClaudeCode\FinTechDemo

# 建各服務映像（根 Dockerfile + MODULE）
docker build --build-arg MODULE=gateway -t fintech-demo/gateway:local .
docker build --build-arg MODULE=order-service -t fintech-demo/order-service:local .
docker build --build-arg MODULE=risk-service -t fintech-demo/risk-service:local .
docker build --build-arg MODULE=account-service -t fintech-demo/account-service:local .

# 載入 kind（名稱須與 context 一致）
kind load docker-image fintech-demo/gateway:local --name trading-local
kind load docker-image fintech-demo/order-service:local --name trading-local
kind load docker-image fintech-demo/risk-service:local --name trading-local
kind load docker-image fintech-demo/account-service:local --name trading-local
# 若 kind 不在 PATH：& D:\ClaudeCode\TradingKubernetes\tools\kind.exe load ...

# 套用 overlay
kubectl apply -k deploy/k8s/overlays/dev
kubectl -n fintech-demo get pods,svc
kubectl -n fintech-demo rollout status deploy/gateway
```

**通過標準（L4）：** 四個 Deployment Ready；對 Service／port-forward 打 `/actuator/health` 為 UP。

### 4.4 Compose vs Kind 怎麼選

| 目標 | 選哪個 | 原因 |
|------|--------|------|
| 開發／Demo／煙霧測試 | **Compose（L3）** | 已含 Kafka／Redis／健康檢查；與 application-demo 對齊 |
| 講 Kustomize／Namespace／Probe／資源請求 | **Kind apply（L4）** | Manifest 示意；infra（Kafka／Redis）本 overlay **未內建**，需另補或只展示 MS |
| CI 綠燈 | **L0～L2** | 不依賴本機叢集是否活著 |

---

## 5. 客服速查（症狀 → 動作）

| 症狀 | 先做 |
|------|------|
| `connection refused` 打 `127.0.0.1:高位埠` | `docker info` → 修 Docker → recreate kind／export kubeconfig |
| `kind` 不是 Cmdlet | 用 `TradingKubernetes\tools\kind.exe` 或裝進 PATH |
| `check-k8s` OK 但 `kubectl get nodes` 掛 | 正常：L2 不連 API；要 L4 才修叢集 |
| Pod `ImagePullBackOff` / `ErrImageNeverPull` | 本機 build + `kind load`，`imagePullPolicy: IfNotPresent` |
| Pod CrashLoop、健康檢查失敗 | `kubectl logs`／`describe`；對照 env（`FINTECH_SERVICES_*_URL`）是否用 **Service DNS** |
| 只想業務可 Demo | `verify-pipeline.ps1 -Up -Smoke`，不必死磕 kind |

---

## 6. 相關檔案索引

| 檔案 | 角色 |
|------|------|
| `scripts/check-k8s.ps1` | L2：kustomize 驗證 |
| `scripts/verify-pipeline.ps1` | L0～L3 一條龍 |
| `scripts/smoke-distributed.ps1` | API 煙霧 |
| `deploy/k8s/**` | Manifest |
| `deploy/README.md` | 短操作 |
| `docs/分散式系統落地`（[HTML](../architecture/distributed.html)） | 分散式敘事＋驗證入口 |
| `TradingKubernetes/scripts/start-local.ps1` | 本機 kind 重建權威腳本 |

---

*紀錄來源：2026-08-06 本機故障（`kind-trading-local` + Docker 引擎未就緒 + kind 不在 PATH）→ 解法寫入本文備查。*

---

## 7. Loop Engineering 實跑結果（2026-08-06）

| 關卡 | 結果 | 備註 |
|------|------|------|
| L0 `gradlew check` | PASS | |
| L1 Docker Ready | PASS | Server 28.5.1 |
| L2 `check-k8s.ps1` | PASS | |
| L3 Compose + smoke | PASS | `SMOKE_DISTRIBUTED_OK` |
| L4 kind apply | PASS（修復後） | 初版 CrashLoop exit 137 |

**L4 根因與修法：** 四個 Spring Boot 同節點啟動慢；`liveness` 過早 Killing → exit 137。改為 `startupProbe`（最长約 180s）、memory limit `768Mi`、`JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=70.0`。通過標準：四 Pod `1/1` 且 `/actuator/health` = UP。

通用教學頁：[`部署跑通-LoopEngineering教學指導.html`](部署跑通-LoopEngineering教學指導.html)。

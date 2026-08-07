# FinTechDemo — 專案規則（薄）

繼承：EngineeringOS eos-minimal @ **0.1.9**  
公版：`d:\ClaudeCode\EngineeringOS\eos-minimal\` · `HOW_TO_APPLY.md`  
權威：[FinTechDemo-SPEC.md](FinTechDemo-SPEC.md)（瀏覽請開 [`docs/guides/spec.html`](docs/guides/spec.html)）

## 與公版差異

- Ports：Gateway `8080` · order `8081` · risk `8082` · job `8083` · account `8084` · Vue `5173`
- 業務 MS ≥3：order／risk／account；Gateway 入口；Kafka＋Redis（demo）
- Frontend：Vue 3（無 Node BFF）
- DB：H2（local／test）；Docker Demo 用各服務 H2 + Kafka/Redis
- **Compose／Kafka／kind 前必須先開 Docker Desktop**（權威：SPEC §3.1；UI 頁頂提醒）
- 驗證：`.\scripts\verify-pipeline.ps1`（或 `.\scripts\check.ps1`）
- optional-frontend：yes

## 本專案專屬

- **統一學習入口**：[docs/index.html](docs/index.html)（設定／K8s／教學／架構；API 連 swagger）
- **學習導引地圖（雙軌）**：[docs/portals/learning-map.html](docs/portals/learning-map.html)（勿用中文檔名 URL）
- **MD 閱讀器**：[docs/md-reader.html](docs/md-reader.html)
- **靜態伺服**：`.\scripts\serve-docs.ps1` → `http://127.0.0.1:5500/docs/index.html`
- 分散式敘事：[architecture](docs/architecture/architecture.html)、[分散式系統落地](docs/architecture/distributed.html)
- Pipeline：check → compose config → k8s →（可選）compose up
- K8s 跑通規則／故障排除：[K8s跑通與驗證技巧](docs/deploy/k8s-tips.html)
- 上線部署階段（S0→S6）：[MD](docs/deploy/stages-doc.html) · [互動](docs/portals/stages.html)
- 部署跑通通用解法：[Loop Engineering](docs/portals/loop-guide.html)

## 註解

- comment_verbosity: **detailed**

## Git Remote

- `johnGitHub24`；規範見 eos `專案上船-GitHub.md`

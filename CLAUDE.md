# FinTechDemo — 專案規則（薄）

繼承：EngineeringOS eos-minimal @ **0.1.10**  
公版：`EngineeringOS/eos-minimal/` · `knowledge/apply-workspace.md`  
權威：[FinTechDemo-SPEC.md](FinTechDemo-SPEC.md)（[`docs/guides/spec.html`](docs/guides/spec.html)）

## 目錄分工

| 目錄 | 用途 | 其他專案？ |
|------|------|------------|
| `scripts/` | **Pure** — JDK 21 → check／bootRun | 可對齊 |
| `demo/` | LOOP／pipeline／doctor（腳本目錄見 `demo/README.md`） | 否 |
| `docs/tools/` | 文件伺服／HTML | 否 |
| 隱藏檔 `.*` | 本機 only | **不上 git**（僅 `.gitignore`） |

## 與公版差異

- Ports：Gateway `8080` · order `8081` · risk `8082` · job `8083` · account `8084` · Vue `5173`
- 業務 MS ≥3：order／risk／account；Gateway；Kafka＋Redis（demo）
- Frontend：Vue 3；DB：H2
- **Compose／Kafka／kind 前先開 Docker Desktop**（SPEC §3.1）
- Pure：`.\scripts\check.ps1` → `.\gradlew.bat :order-service:bootRun`
- Demo：`.\開啟Demo.cmd`（本機）／`.\開啟K8sDemo.cmd`（kind；擇一）；腳本指南 `docs/portals/demo-scripts.html`
- optional-frontend：yes

## 本專案專屬

- 學習入口：[docs/index.html](docs/index.html) · `.\docs\tools\serve-docs.ps1`
- 地圖／Loop：[learning-map](docs/portals/learning-map.html) · [loop-guide](docs/portals/loop-guide.html)
- 黃金對照：`eos-minimal/knowledge/golden-project.md`

## 註解

- comment_verbosity: **detailed**

## Git Remote

- `johnGitHub24`；規範見 eos `專案上船-GitHub.md`

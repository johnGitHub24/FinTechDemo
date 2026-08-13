# FinTechDemo `scripts/` — Pure only

> Demo：`../demo/` · Docs：`../docs/tools/`  
> 套用：`<WorkspaceRoot>/EngineeringOS/eos-minimal/hooks/apply-workspace.ps1`（`-WithDemo`）

| File | Role |
|------|------|
| `portable-env.*` / `env.*` | OS `JAVA_HOME` |
| `check.*` | `gradlew check` |
| `intellij-run.properties` | `:order-service:bootRun` |
| `fix-intellij-run.ps1` | 本機 IDE 提示 |

其他專案只跑 `apply-workspace.ps1`（不要 `-WithDemo`）。

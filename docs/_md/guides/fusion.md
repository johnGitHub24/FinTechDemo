# FinTechDemo — 技術融合對照（含採納技巧）

> 先讀 [`技術次序與架構為什麼`](why.html) 與 [`codeGraphic`](../portals/codeGraphic.html)。  
> 本表回答：**從哪個子專案抄哪招、為什麼、什麼不搬。**

---

## 按你給的次序

### 1. TradingPagingList
| 採納 | 來源技巧 | FinTechDemo 落點 |
|------|----------|------------------|
| 伺服器端分頁 | `page` 0-based、`size` 預設 10、後端 `Math.min(size,100)` | `GET /api/orders` |
| Page DTO | `PageResponse`：content／page／size／totalElements／totalPages | `common` DTO |
| Vue 換頁重打 API | `ProductTable` + `Pagination` emit | `frontend` 訂單表 |
| Vite proxy | `/api` → 後端，免 CORS | 開發時 proxy → Gateway `:8080` |
| **不搬** | Product Domain、DataSeeder 50 筆、`@CrossOrigin(*)` | |

### 2. TradingSpringSecurity
| 採納 | 來源技巧 | FinTechDemo 落點 |
|------|----------|------------------|
| JWT 無狀態 | `JwtTokenProvider` + `JwtAuthenticationFilter` + STATELESS | `order-service` Security |
| 角色 | USER／ADMIN；DELETE 限 ADMIN（規則在 FilterChain） | 同左 |
| 登入 | `AuthenticationManager` → 簽 Bearer | `POST /api/auth/login` |
| 身分來源 | `Authentication.getName()`，不信 body username | Order 建立 |
| **不搬** | 教學向完整公開註冊（可用種子帳號） | |

### 3. TradingCRUD
| 採納 | 來源技巧 | FinTechDemo 落點 |
|------|----------|------------------|
| 分層 | Controller→Service→Repo；Controller 不碰 JPA | order 模組 |
| DTO／Mapper | Entity 不直接出 API | Request／Response 分離 |
| `@Valid` + 薄 Controller | 寫入校驗 | 下單／更新 |
| Axios Bearer 攔截 | 401 統一處理 | frontend |
| **不搬** | Node Express BFF（改 Gateway）、批次 207 主軸 | |

### 4. TradingSpringCloud（Gateway）
| 採納 | 來源技巧 | FinTechDemo 落點 |
|------|----------|------------------|
| 固定 URL 路由 | `trading.services.*-url`／`@ConfigurationProperties` | gateway yml |
| Path 對齊 | 公開 `/api/**` 與下游一致，少 rewrite | 簡化架構敘事 |
| （可選）Feign 聚合 | Dashboard 式 BFF | 非必須；優先透傳 |
| **不搬** | loop-service 信任分數 Domain | |
| **可說明** | 正式環境可換成 MicroService 的 `lb://` | 口述即可 |

### 5. TradingMicroService
| 採納 | 來源技巧 | FinTechDemo 落點 |
|------|----------|------------------|
| 依職責拆服務 | 各服務自有 API | **order／risk／account**（≥3 業務 MS）+ job |
| Feign 契約 | Java interface 當跨服務 API | `RiskClient`、`AccountClient`（**固定 URL**，先跑穩交易鏈） |
| 啟動敘事 | 先業務再生 Gateway | scripts／README |
| **不搬（本版）** | Eureka、Config Server、Resilience4j 示範開關 | 升級口頭：拿掉 Feign `url` → 服務名＋Eureka；Gateway 改 `lb://`（發現機制升級，非重寫業務）；完整串接見 TradingMicroService |
| **勿混淆** | — | APIGatewayMQ **無** Eureka（主軸 Kafka 削峰） |

### 6. APIGatewayMQ（Kafka）
| 採納 | 來源技巧 | FinTechDemo 落點 |
|------|----------|------------------|
| 寫入削峰 | 驗證→Kafka→可查狀態 | POST orders → `order-events` |
| Topic／key | 常數放 common；key＝業務鍵 | `order-events`、`trade-events` |
| 跨服務 Consumer | 消費後呼叫其他服務 | order Consumer→risk；account Consumer→入帳+Redis |
| 冪等 | clientOrderId | orders 唯一約束 |
| local 降級 | enabled=false | 預設關 listener |
| **不搬** | EngineProxy 多副本 RR、R001–R010、WebFlux Gateway | |
| **詳述** | — | [分散式系統落地](../architecture/distributed.html) |

### 7. TradingJob
| 採納 | 來源技巧 | FinTechDemo 落點 |
|------|----------|------------------|
| Job 薄 Service 厚 | `@Scheduled` 只呼叫 Service | job-service |
| 執行緒池 | `ThreadPoolTaskScheduler` + pool-size≥2 | SchedulingConfig |
| 設定 prefix | `trading.job.*` + `@ConditionalOnProperty` | yml 可關 |
| **不搬** | 與 Engine 共庫／FailedCommand 全套 | 先做逾時取消 1 支 |

### 8. TradingIocAOP
| 採納 | 來源技巧 | FinTechDemo 落點 |
|------|----------|------------------|
| 成功後審計 | `@AfterReturning` 綁下單成功 | AuditAspect |
| 切面順序 | `@Order` | 固定 audit 順序 |
| **升級** | 原專案是記憶體 `AspectRecorder` | **改寫 DB `audit_log`**（展示更有說服力） |
| **不搬** | mini-ioc、六切面全抄 | 先 Audit 一條 |

### 9. TradingLocustJMeter
| 採納 | 來源技巧 | FinTechDemo 落點 |
|------|----------|------------------|
| baseline 一鍵 | `run-baseline.ps1` → HTML 報告 | `loadtest/` |
| 打 Gateway | `GATEWAY_URL=http://localhost:8080` | 不打內建靶場 |
| 冪等鍵 uuid | 避免壓測被冪等短路 | submit block |
| **不搬** | FastAPI 靶場本體（可選留 JMeter 一支） | |

### 10. TradingPrometheusActuator
| 採納 | 來源技巧 | FinTechDemo 落點 |
|------|----------|------------------|
| 暴露端點 | health,info,metrics,prometheus | 各服務 |
| Counter／Timer | 業務 Meter 在 Service 註冊 | orders_created、risk_rejected |
| 共用 tag | `MeterRegistryCustomizer` | application=fintech-demo |
| **不搬** | 無 DB 的 in-memory Trade 模型 | |

### 11. TradingKubernetes
| 採納 | 來源技巧 | FinTechDemo 落點 |
|------|----------|------------------|
| 目錄三層 | `apps/` + `infrastructure/` + `clusters/{env}` | `deploy/` |
| overlay | 改 replicas／image tag，不複製三份 | kustomize |
| check | `kustomize build` 三環境 | `scripts/check.ps1` 一段 |
| **不搬** | Argo+Flux 雙 GitOps、Canary Analysis 全集 | 口述即可 |

---

## 一句話

子專案＝**有次序的技巧教材**；FinTechDemo＝**依同一次序組裝的整合示範**（見 codeGraphic 四個 Tab）。

# engineering-config（eos-minimal）

```yaml
project: FinTechDemo
language: Java 21
framework: Spring Boot 3.x
database_dev: H2
database_prod: PostgreSQL   # optional; Demo 預設 H2 per service
messaging: Kafka (Redpanda localhost:19092)
cache: Redis (localhost:6379)  # account-service；demo profile
test: JUnit 5 + MockMvc
coverage_target: 80
logging: Logback
backend_ports:
  gateway: 8080
  order-service: 8081
  risk-service: 8082
  job-service: 8083
  account-service: 8084
frontend_port: 5173
optional_frontend: yes
node_bff: no   # 統一打 Spring Cloud Gateway
kafka_topics:
  - order-events
  - trade-events
modules:
  business:
    - order-service
    - risk-service
    - account-service
  edge:
    - gateway
  support:
    - job-service
    - common
    - frontend
    - loadtest
    - deploy
scheduler:
  pool_size: 4
  thread_name_prefix: fintech-job-
ai_tools:
  - Cursor
eos_version: 0.1.9
eos_path: d:\ClaudeCode\EngineeringOS\eos-minimal
apply: HOW_TO_APPLY.md
```

變數替換與檢查表：`eos-minimal/templates/spring-boot/docs/apply-checklist-zh.md`。

架構權威圖：[`architecture`](architecture.html) · [`分散式系統落地`](distributed.html)

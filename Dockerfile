# =============================================================================
# FinTechDemo — 根目錄多模組 Spring Boot Dockerfile
# 【職責】用同一份 Dockerfile，依 ARG MODULE 建出 gateway／order／risk／account／job 任一服務映像
# 【技巧】多階段建置（multi-stage）：AS build 編譯 → 最終 stage 只帶 JRE + jar，映像較小
# 【概念】學習重點：一個 Gradle 多模組專案 ≠ 一個映像；用 build-arg 選「哪一個 :bootJar」
# 用法：docker build --build-arg MODULE=order-service -t fintech-demo/order-service:local .
# =============================================================================

# 【技巧】ARG 在 FROM 之前：可當「預設模組名」；FROM 之後還要再宣告一次才能在該 stage 使用
ARG MODULE=order-service

# ---------------------------------------------------------------------------
# Stage 1：編譯（有完整 JDK + Gradle）
# 【職責】把多模組原始碼編成可執行的 Spring Boot fat jar
# 【技巧】FROM ... AS build → 後續可用 COPY --from=build 只帶走產物
# 【概念】編譯工具鏈不必進正式映像，降低攻擊面與體積（十二因子／不可變基礎設施）
# ---------------------------------------------------------------------------
FROM gradle:8.5-jdk21-alpine AS build
# 再次宣告：ARG 不會自動跨 stage 繼承到「可讀變數」語意，每個 stage 要用就要寫
ARG MODULE
WORKDIR /build

# 【技巧】先 COPY 建置描述檔再 COPY 原始碼：利用 Docker layer cache，改業務碼時不必重抓依賴
COPY settings.gradle build.gradle gradle.properties ./
COPY gradle ./gradle
# 【概念】多模組：common 常被各服務依賴，必須一併進 build context
COPY common ./common
COPY gateway ./gateway
COPY order-service ./order-service
COPY risk-service ./risk-service
COPY account-service ./account-service
COPY job-service ./job-service

# 【技巧】:${MODULE}:bootJar 是 Gradle 任務路徑；-x test 加快映像建置（測試交給 CI／本機 check）
# 【概念】--no-daemon：容器內一次性建置，不必留 Gradle daemon
RUN gradle :${MODULE}:bootJar --no-daemon -x test

# ---------------------------------------------------------------------------
# Stage 2：執行期（只含 JRE）
# 【職責】執行已編好的 app.jar；對外容器埠慣例 EXPOSE 8080（實際聽哪個埠看各服務 application.yml）
# 【技巧】eclipse-temurin:21-jre-alpine＝官方 Temurin JRE；比帶 JDK 的映像小很多
# 【概念】「建置與執行分離」：prod 映像不應再編譯
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
ARG MODULE
WORKDIR /app

# 【技巧】curl 供 docker-compose healthcheck 打 /actuator/health（見 docker-compose.yml）
RUN apk add --no-cache curl

# 【技巧】COPY --from=build：只複製上一 stage 的 jar；路徑對應各模組 bootJar 輸出名 app.jar
COPY --from=build /build/${MODULE}/build/libs/app.jar app.jar

# 【技巧】EXPOSE 只是文件／慣例，真正映射埠在 compose／K8s 的 ports／containerPort
EXPOSE 8080

# 【技巧】exec 形式 ENTRYPOINT（JSON 陣列）→ PID 1 是 java，訊號（SIGTERM）可正確傳給 JVM
ENTRYPOINT ["java", "-jar", "app.jar"]

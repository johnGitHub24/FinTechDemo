package com.fintech.demo.order.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * 【職責】order-service（主入口）就緒後印出全專案服務 URL，方便 IntelliJ Console 一鍵對照。
 * 【技巧】對齊 TradingCRUD StartupInfoLogger；只掛在主入口，避免每個服務重複刷屏。
 * 【概念】分散式 Demo 要開多個 Application；此處把每個埠的 health／API 一次列齊。
 */
@Component
public class StartupInfoLogger implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(StartupInfoLogger.class);

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Environment env = event.getApplicationContext().getEnvironment();
        if (!env.getProperty("startup.info.enabled", Boolean.class, true)) {
            return;
        }

        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        out.println();
        out.println("╔══════════════════════════════════════════════════════════════════════════════════╗");
        out.println("║  FinTechDemo — 主入口已啟動：OrderServiceApplication (:8081)                       ║");
        out.println("╠══════════════════════════════════════════════════════════════════════════════════╣");
        out.println("║ 【IntelliJ 最短可成交（必開兩個後端）】                                             ║");
        out.println("║   ★ RiskServiceApplication   → :8082   （點「成交」Feign 風控，必開）             ║");
        out.println("║   ★ OrderServiceApplication  → :8081   （登入／下單／成交／審計）                 ║");
        out.println("║   前端：cd frontend && npm run dev → http://localhost:5173                       ║");
        out.println("║   帳號：trader1 / password   ·   ADMIN：admin / password                         ║");
        out.println("╠══════════════════════════════════════════════════════════════════════════════════╣");
        out.println("║ 【前端】 ★ 可點連結在瀏覽器：http://localhost:5173/login （右側 Demo 快捷入口）   ║");
        out.println("║   Vue Dev      http://localhost:5173     ← 需另開：cd frontend && npm run dev     ║");
        out.println("║   Login        http://localhost:5173/login                                       ║");
        out.println("║   Trade        http://localhost:5173/trade                                       ║");
        out.println("║   Portal       http://localhost:5173/portal                                      ║");
        out.println("║   Audit        http://localhost:5173/portal/audit                                ║");
        out.println("╠══════════════════════════════════════════════════════════════════════════════════╣");
        out.println("║ 【Gateway :8080】  GatewayApplication（分散式統一入口，可選）                      ║");
        out.println("║   Health       http://localhost:8080/actuator/health                             ║");
        out.println("╠══════════════════════════════════════════════════════════════════════════════════╣");
        out.println("║ 【Order ★ :8081】  OrderServiceApplication（主入口）                              ║");
        out.println("║   Health       http://localhost:8081/actuator/health                             ║");
        out.println("║   Swagger UI   http://localhost:8081/swagger-ui.html                             ║");
        out.println("║   OpenAPI      http://localhost:8081/v3/api-docs                                 ║");
        out.println("║   H2 Console   http://localhost:8081/h2-console                                  ║");
        out.println("║   Prometheus   http://localhost:8081/actuator/prometheus                         ║");
        out.println("║   Login API    http://localhost:8081/api/auth/login                              ║");
        out.println("╠══════════════════════════════════════════════════════════════════════════════════╣");
        out.println("║ 【Risk ★ :8082】  RiskServiceApplication（成交必開）                              ║");
        out.println("║   Health       http://localhost:8082/actuator/health                             ║");
        out.println("║   Risk Check   http://localhost:8082/api/risk/check   （POST JSON）              ║");
        out.println("╠══════════════════════════════════════════════════════════════════════════════════╣");
        out.println("║ 【Job :8083】  JobServiceApplication（逾時取消排程，可選）                         ║");
        out.println("║   Health       http://localhost:8083/actuator/health                             ║");
        out.println("╠══════════════════════════════════════════════════════════════════════════════════╣");
        out.println("║ 【Account :8084】  AccountServiceApplication（餘額／持倉／Redis／Kafka，可選）    ║");
        out.println("║   Health       http://localhost:8084/actuator/health                             ║");
        out.println("║   Me Account   http://localhost:8084/api/accounts/me                             ║");
        out.println("║   Positions    http://localhost:8084/api/positions                               ║");
        out.println("║   Apply Trade  http://localhost:8084/api/internal/accounts/{userId}/apply-trade  ║");
        out.println("╠══════════════════════════════════════════════════════════════════════════════════╣");
        out.println("║ 【觀測／壓測 Demo】  docker compose --profile monitoring up -d                     ║");
        out.println("║   Grafana      http://localhost:3000  （admin/admin · FinTechDemo Overview）      ║");
        out.println("║   Prometheus   http://localhost:9090                                             ║");
        out.println("║   Locust UI    http://localhost:8089  （.\\scripts\\run-loadtest.ps1 -WebUi）      ║");
        out.println("║   前端快捷     http://localhost:5173/login  （登入頁可點上述連結）                ║");
        out.println("╠══════════════════════════════════════════════════════════════════════════════════╣");
        out.println("║ 【學習文件】 先跑 .\\scripts\\serve-docs.ps1 → http://127.0.0.1:5500/docs/…        ║");
        out.println("║   統一入口     http://127.0.0.1:5500/docs/index.html                              ║");
        out.println("║   Demo 流程    http://127.0.0.1:5500/docs/portals/demo-flow.html                  ║");
        out.println("║   學習手冊     http://127.0.0.1:5500/docs/portals/handbook.html                   ║");
        out.println("║   Swagger 靜態 http://127.0.0.1:5500/docs/portals/swagger.html                    ║");
        out.println("║   codeGraphic  http://127.0.0.1:5500/docs/portals/codeGraphic.html                ║");
        out.println("╠══════════════════════════════════════════════════════════════════════════════════╣");
        out.println("║ 【建議啟動順序】                                                                  ║");
        out.println("║   1) 最短可成交：Risk(:8082) → Order(:8081) → frontend(:5173)                     ║");
        out.println("║   2) 完整：Risk → Account → Order → Gateway（+ Job 可選）                        ║");
        out.println("║   3) 觀測：再加 --profile monitoring（Grafana/Prometheus）                       ║");
        out.println("║   H2 JDBC：jdbc:h2:mem:fintechdemo  ·  User：sa  ·  Password：（空白）            ║");
        out.println("╚══════════════════════════════════════════════════════════════════════════════════╝");
        out.println();
        log.info("FinTechDemo primary entry ready — http://localhost:8081/actuator/health");
        log.info("Risk (required for execute) — http://localhost:8082/actuator/health");
        log.info("Frontend — http://localhost:5173 (Trade/Portal 內建後端 PROCESS FLOW 面板)");
        log.info("Demo topology — GET /api/demo/topology");
    }
}

package com.fintech.demo.order.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * 【職責】Order 就緒後印短橫幅：LOOP 狀態 + 各服務 UP／DOWN（即時探測）。
 * 【技巧】ensure 開啟且尚未通時標 STARTING（不是最終失敗）。
 * 【概念】最終以 {@link DemoStackBootstrap} 結束後全橫幅 UP 為準。
 */
@Component
@Order(50)
public class StartupInfoLogger implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(StartupInfoLogger.class);

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Environment env = event.getApplicationContext().getEnvironment();
        if (!env.getProperty("startup.info.enabled", Boolean.class, true)) {
            return;
        }

        boolean ensure = env.getProperty("fintech.startup.ensure-stack", Boolean.class, true);
        boolean skipDocker = env.getProperty("fintech.startup.ensure-skip-docker", Boolean.class, false);
        boolean skipLocust = env.getProperty("fintech.startup.ensure-skip-locust", Boolean.class, false);

        String order = mark("http://127.0.0.1:8081/actuator/health");
        String risk = startingIf(ensure, mark("http://127.0.0.1:8082/actuator/health"));
        String vite = mark("http://127.0.0.1:5173/login");
        if ("DOWN".equals(vite)) {
            vite = mark("http://localhost:5173/login");
        }
        vite = startingIf(ensure, vite);
        String gateway = startingIf(ensure, mark("http://127.0.0.1:8080/actuator/health"));
        String job = startingIf(ensure, mark("http://127.0.0.1:8083/actuator/health"));
        String account = startingIf(ensure, mark("http://127.0.0.1:8084/actuator/health"));
        String docs = startingIf(ensure, mark("http://127.0.0.1:5500/docs/index.html"));
        String grafana = startingIf(ensure && !skipDocker, mark("http://127.0.0.1:3000/login"));
        String prometheus = startingIf(ensure && !skipDocker, mark("http://127.0.0.1:9090/-/healthy"));
        String locust = startingIf(ensure && !skipLocust, mark("http://127.0.0.1:8089/"));

        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        out.println();
        out.println("======== FinTechDemo Order ready :8081 ========");
        if (ensure) {
            out.println("LOOP: ensure-demo-links -FromOrder 背景進行中（約 3-8 分）");
            out.println("  ※ 目標：橫幅全部 UP（Order/Risk/Vite + Gateway/Job/Account/Docs/監控/Locust）");
            out.println("  log: logs/ensure-from-order.*.log");
            out.println("  skip-docker=" + skipDocker + "  skip-locust=" + skipLocust);
        } else {
            out.println("LOOP off — run: .\\scripts\\ensure-demo-links.ps1 -FromOrder");
        }
        out.println("-----------------------------------------------");
        out.println("trade-ready  Order " + bracket(order)
                + "  Risk " + bracket(risk)
                + "  Vite " + bracket(vite));
        out.println("-----------------------------------------------");
        line(out, vite, "Vue", "http://localhost:5173/login");
        line(out, order, "Order", "http://localhost:8081  (swagger / h2 / prometheus)");
        line(out, risk, "Risk", "http://localhost:8082");
        line(out, gateway, "Gateway", "http://localhost:8080");
        line(out, job, "Job", "http://localhost:8083");
        line(out, account, "Account", "http://localhost:8084");
        line(out, docs, "Docs", "http://127.0.0.1:5500/docs/index.html");
        line(out, grafana, "Grafana", "http://localhost:3000  (admin/admin)");
        line(out, prometheus, "Prom", "http://localhost:9090");
        line(out, locust, "Locust", "http://localhost:8089");
        out.println("-----------------------------------------------");
        out.println("login: trader1 / password");
        out.println("UI:    http://localhost:5173/blueprint");
        out.println("check: .\\scripts\\verify-demo-shortcuts.ps1");
        out.println("fix:   .\\scripts\\ensure-demo-links.ps1");
        out.println("================================================");
        out.println();

        log.info("Order ready — ensure={} skipDocker={} skipLocust={}; Risk={} Grafana={} Prom={} Locust={}",
                ensure, skipDocker, skipLocust, risk, grafana, prometheus, locust);
    }

    private static String startingIf(boolean ensure, String status) {
        if (ensure && "DOWN".equals(status)) {
            return "STARTING";
        }
        return status;
    }

    private static void line(PrintStream out, String status, String name, String url) {
        out.println(String.format("%-11s %-8s %s", bracket(status), name, url));
    }

    private static String bracket(String status) {
        return "[" + status + "]";
    }

    private static String mark(String url) {
        return probe(url) ? "UP" : "DOWN";
    }

    private static boolean probe(String url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setConnectTimeout(800);
            conn.setReadTimeout(800);
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(true);
            int code = conn.getResponseCode();
            return code >= 200 && code < 500;
        } catch (Exception ex) {
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}

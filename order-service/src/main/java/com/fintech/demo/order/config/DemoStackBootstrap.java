package com.fintech.demo.order.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 【職責】Order 就緒後自動跑 Loop Engineering：{@code scripts/ensure-demo-links.ps1 -FromOrder}。
 * 【技巧】背景執行；失敗會重試，直到橫幅服務全 UP（或耗盡重試）。
 * 【概念】Windows 上 bat／npm 必須用 cmd /c（腳本內已根修）；FromOrder 跳過 javadoc／test 長工。
 * 【邊界】省 RAM：{@code ensure-skip-docker}／{@code ensure-skip-locust}；測試關 {@code ensure-stack=false}。
 */
@Component
@Order(100)
@ConditionalOnProperty(name = "fintech.startup.ensure-stack", havingValue = "true", matchIfMissing = true)
public class DemoStackBootstrap implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(DemoStackBootstrap.class);
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    /** FromOrder 腳本整輪失敗時的額外重試次數（腳本內部已有 round）。 */
    private static final int MAX_SCRIPT_ATTEMPTS = 2;

    private final boolean skipDocker;
    private final boolean skipLocust;

    public DemoStackBootstrap(
            @Value("${fintech.startup.ensure-skip-docker:false}") boolean skipDocker,
            @Value("${fintech.startup.ensure-skip-locust:false}") boolean skipLocust) {
        this.skipDocker = skipDocker;
        this.skipLocust = skipLocust;
    }

    /**
     * 【職責】非同步觸發 ensure-demo-links，避免拖慢 ApplicationReady。
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!STARTED.compareAndSet(false, true)) {
            return;
        }
        Path script = resolveEnsureScript();
        if (script == null) {
            log.warn("ensure-demo-links.ps1 not found — skip auto stack. Run .\\scripts\\ensure-demo-links.ps1 manually.");
            return;
        }
        Thread t = new Thread(() -> runEnsure(script), "demo-stack-ensure");
        t.setDaemon(true);
        t.start();
        log.info("LOOP ensure-demo-links started in background: {}", script);
    }

    private void runEnsure(Path script) {
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        try {
            Path root = script.getParent().getParent();
            Path outLog = root.resolve("logs").resolve("ensure-from-order.out.log");
            Path errLog = root.resolve("logs").resolve("ensure-from-order.err.log");
            Files.createDirectories(outLog.getParent());

            int code = -1;
            for (int attempt = 1; attempt <= MAX_SCRIPT_ATTEMPTS; attempt++) {
                List<String> cmd = buildEnsureCommand(script);
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.directory(root.toFile());
                // 追加寫入，保留上一輪診斷
                pb.redirectOutput(ProcessBuilder.Redirect.appendTo(outLog.toFile()));
                pb.redirectError(ProcessBuilder.Redirect.appendTo(errLog.toFile()));
                Files.writeString(outLog,
                        System.lineSeparator()
                                + "===== DemoStackBootstrap attempt " + attempt + "/" + MAX_SCRIPT_ATTEMPTS
                                + " =====" + System.lineSeparator(),
                        java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.APPEND);
                Process p = pb.start();
                code = p.waitFor();
                if (code == 0 && isTradeReady()) {
                    break;
                }
                log.warn("LOOP ensure attempt {}/{} exit={} tradeReady={}",
                        attempt, MAX_SCRIPT_ATTEMPTS, code, isTradeReady());
                if (attempt < MAX_SCRIPT_ATTEMPTS) {
                    Thread.sleep(5_000L);
                }
            }

            out.println();
            out.println("======== LOOP ensure finished (exit " + code + ") ========");
            out.println("  log: logs/ensure-from-order.out.log");
            printStackStatus(out);
            out.println("==========================================================");
            out.println();

            if (code == 0 && isTradeReady()) {
                log.info("LOOP ensure-demo-links OK — banner stack ready. See logs/ensure-from-order.*.log");
            } else {
                log.warn("LOOP ensure-demo-links exit={} tradeReady={} — see logs/ensure-from-order.*.log then .\\scripts\\doctor-demo.ps1 -Fix",
                        code, isTradeReady());
            }
        } catch (Exception ex) {
            log.warn("LOOP ensure-demo-links failed to start: {}", ex.toString());
            out.println("LOOP ensure failed to start: " + ex);
        }
    }

    private List<String> buildEnsureCommand(Path script) {
        List<String> cmd = new ArrayList<>();
        cmd.add("powershell.exe");
        cmd.add("-NoProfile");
        cmd.add("-ExecutionPolicy");
        cmd.add("Bypass");
        cmd.add("-File");
        cmd.add(script.toAbsolutePath().toString());
        cmd.add("-FromOrder");
        if (skipDocker) {
            cmd.add("-SkipDocker");
        }
        if (skipLocust) {
            cmd.add("-SkipLocust");
        }
        return cmd;
    }

    /**
     * 【目的】LOOP 結束後重印橫幅服務狀態（與啟動快照分開）。
     */
    private static void printStackStatus(PrintStream out) {
        boolean order = probe("http://127.0.0.1:8081/actuator/health");
        boolean risk = probe("http://127.0.0.1:8082/actuator/health");
        boolean vite = probe("http://127.0.0.1:5173/login") || probe("http://localhost:5173/login");
        boolean gateway = probe("http://127.0.0.1:8080/actuator/health");
        boolean job = probe("http://127.0.0.1:8083/actuator/health");
        boolean account = probe("http://127.0.0.1:8084/actuator/health");
        boolean docs = probe("http://127.0.0.1:5500/docs/index.html");
        boolean grafana = probe("http://127.0.0.1:3000/login");
        boolean prom = probe("http://127.0.0.1:9090/-/healthy");
        boolean locust = probe("http://127.0.0.1:8089/");
        out.println("trade-ready  Order [" + up(order) + "]  Risk [" + up(risk) + "]  Vite [" + up(vite) + "]");
        out.println("stack        Gateway [" + up(gateway) + "]  Job [" + up(job) + "]  Account [" + up(account) + "]");
        out.println("             Docs [" + up(docs) + "]  Grafana [" + up(grafana) + "]  Prom [" + up(prom)
                + "]  Locust [" + up(locust) + "]");
        if (order && risk && vite) {
            out.println("TRADE-READY OK → http://127.0.0.1:5173/login  (trader1 / password)");
        } else {
            out.println("TRADE-READY FAIL — run: .\\scripts\\ensure-demo-links.ps1 -FromOrder");
        }
    }

    private static String up(boolean ok) {
        return ok ? "UP" : "DOWN";
    }

    private static boolean isTradeReady() {
        boolean order = probe("http://127.0.0.1:8081/actuator/health");
        boolean risk = probe("http://127.0.0.1:8082/actuator/health");
        boolean vite = probe("http://127.0.0.1:5173/login") || probe("http://localhost:5173/login");
        return order && risk && vite;
    }

    private static boolean probe(String url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setConnectTimeout(800);
            conn.setReadTimeout(800);
            conn.setRequestMethod("GET");
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

    /**
     * 【職責】從 user.dir 往上找專案根的 ensure 腳本（相容 IntelliJ 從模組目錄啟動）。
     */
    static Path resolveEnsureScript() {
        Path dir = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        for (int i = 0; i < 6 && dir != null; i++) {
            Path candidate = dir.resolve("scripts").resolve("ensure-demo-links.ps1");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            Path parent = dir.getParent();
            if (parent == null || parent.equals(dir)) {
                break;
            }
            dir = parent;
        }
        File alt = new File("scripts/ensure-demo-links.ps1");
        if (alt.isFile()) {
            return alt.toPath().toAbsolutePath();
        }
        return null;
    }
}

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 【職責】Order 就緒後自動跑 Loop Engineering：{@code scripts/ensure-demo-links.ps1}，補齊 DOWN 的服務。
 * 【技巧】{@link ApplicationReadyEvent} 後另開執行緒呼叫 PowerShell，不阻塞主啟動；
 *         腳本內對已 UP 的埠會 KEEP，不會再起第二個 Order。
 * 【概念】IntelliJ 只開 OrderServiceApplication 時，Risk／Vite／Gateway／Account／Job／docs
 *         ／Grafana／Prometheus／Locust 常是 DOWN → 橫幅連結點了會連不上。
 *         本類把「啟動主入口＝整棧可連（含觀測／壓測）」做成 Loop Engineering 預設。
 * 【邊界】預設不略過 monitoring／Locust。省 RAM 才設
 *         {@code fintech.startup.ensure-skip-docker=true} 或 {@code ensure-skip-locust=true}。
 *         測試用 {@code fintech.startup.ensure-stack=false} 關閉整段 ensure。
 */
@Component
@Order(100)
@ConditionalOnProperty(name = "fintech.startup.ensure-stack", havingValue = "true", matchIfMissing = true)
public class DemoStackBootstrap implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(DemoStackBootstrap.class);
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);

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
     * 【技巧】{@link AtomicBoolean} 防止同一 JVM 重複觸發；找不到腳本只 warn。
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
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("powershell.exe");
            cmd.add("-NoProfile");
            cmd.add("-ExecutionPolicy");
            cmd.add("Bypass");
            cmd.add("-File");
            cmd.add(script.toAbsolutePath().toString());
            if (skipDocker) {
                cmd.add("-SkipDocker");
            }
            if (skipLocust) {
                cmd.add("-SkipLocust");
            }

            Path root = script.getParent().getParent();
            Path out = root.resolve("logs").resolve("ensure-from-order.out.log");
            Path err = root.resolve("logs").resolve("ensure-from-order.err.log");
            Files.createDirectories(out.getParent());

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(root.toFile());
            pb.redirectOutput(out.toFile());
            pb.redirectError(err.toFile());
            Process p = pb.start();
            int code = p.waitFor();
            if (code == 0) {
                log.info("LOOP ensure-demo-links OK (exit 0). See logs/ensure-from-order.*.log");
            } else {
                log.warn("LOOP ensure-demo-links exit={} — see logs/ensure-from-order.*.log then .\\scripts\\doctor-demo.ps1 -Fix",
                        code);
            }
        } catch (Exception ex) {
            log.warn("LOOP ensure-demo-links failed to start: {}", ex.toString());
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

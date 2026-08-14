package com.fintech.demo.order.config;

import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

/**
 * 【職責】本機 Demo：在同一 JVM 開 H2 TCP，讓 IntelliJ／DataGrip 連到正在跑的 mem DB。
 * 【技巧】Spring 用 {@code jdbc:h2:mem:fintechdemo}；外部用 {@code jdbc:h2:tcp://localhost:9093/mem:fintechdemo}。
 * 【概念】mem 庫只活在 process 內；沒有 TCP 時 DataGrip 填 mem URL 會開出另一份空庫。
 * 【邊界】埠被占用（舊 order-service 未停）時只 warn、不讓整個應用啟動失敗；測試關 {@code spring.h2.tcp.enabled=false}。
 */
@Component
@ConditionalOnProperty(name = "spring.h2.tcp.enabled", havingValue = "true", matchIfMissing = true)
public class H2TcpConfig implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(H2TcpConfig.class);

    private final Server server;
    private final int port;

    public H2TcpConfig(@Value("${spring.h2.tcp.port:9093}") int port) {
        this.port = port;
        Server started = null;
        try {
            started = Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", String.valueOf(port));
            started.start();
            log.info("H2 TCP enabled — DataGrip URL: jdbc:h2:tcp://localhost:{}/mem:fintechdemo  (sa / 空白密碼)", port);
        } catch (SQLException ex) {
            log.warn(
                    "H2 TCP port {} not started (already in use?). "
                            + "Stop old :order-service:bootRun or set spring.h2.tcp.port. "
                            + "Existing instance may still serve jdbc:h2:tcp://localhost:{}/mem:fintechdemo — {}",
                    port,
                    port,
                    ex.getMessage());
        }
        this.server = started;
    }

    /** 【職責】供 StartupInfoLogger 判斷 TCP 是否真的在聽。 */
    public boolean isRunning() {
        return server != null && server.isRunning(false);
    }

    public int getPort() {
        return port;
    }

    @Override
    public void destroy() {
        if (server != null && server.isRunning(false)) {
            server.stop();
        }
    }
}

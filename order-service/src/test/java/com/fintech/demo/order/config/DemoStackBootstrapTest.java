package com.fintech.demo.order.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 【職責】驗證能從工作目錄解析到 demo/ensure-demo-links.ps1（IntelliJ／Gradle 啟動前提）。
 */
class DemoStackBootstrapTest {

    @Test
    void resolveEnsureScript_findsRepoScript() {
        Path script = DemoStackBootstrap.resolveEnsureScript();
        assertNotNull(script, "應找得到 demo/ensure-demo-links.ps1");
        assertTrue(Files.isRegularFile(script), script.toString());
        assertTrue(script.getFileName().toString().equals("ensure-demo-links.ps1"));
    }
}

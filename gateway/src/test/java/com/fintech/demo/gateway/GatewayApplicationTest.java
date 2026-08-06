package com.fintech.demo.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
/**
 * 【職責】驗證 Gateway 的 Spring Boot 應用程式內容可完整載入。
 * 【技巧】使用 @SpringBootTest 啟動完整 ApplicationContext。
 * 【概念】Gateway 的 context test 可及早找出路由或自動組態的啟動衝突。
 */
class GatewayApplicationTest {

    /**
     * CASE-GATEWAY-BOOT-001：Given 預設測試組態，When 載入 Gateway context，Then 不拋出例外。
     */
    @Test
    void contextLoads() {
    }
}

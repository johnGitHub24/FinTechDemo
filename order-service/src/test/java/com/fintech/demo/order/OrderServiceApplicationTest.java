package com.fintech.demo.order;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
/**
 * 【職責】驗證 Order Service 的 Spring Boot 應用程式內容可完整載入。
 * 【技巧】使用 @SpringBootTest 建立包含 Feign 與 Security 組態的 ApplicationContext。
 * 【概念】Context smoke test 是訂單服務跨層 Bean 可正確組裝的基本保護網。
 */
class OrderServiceApplicationTest {

    /**
     * CASE-ORDER-BOOT-001：Given 預設測試組態，When 載入 Order Service context，Then 不拋出例外。
     */
    @Test
    void contextLoads() {
    }
}

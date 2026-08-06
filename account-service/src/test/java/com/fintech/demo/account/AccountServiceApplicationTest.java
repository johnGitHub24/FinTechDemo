package com.fintech.demo.account;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
/**
 * 【職責】驗證 Account Service 的 Spring Boot 應用程式內容可完整載入。
 * 【技巧】使用 @SpringBootTest 建立正式組態所需的 ApplicationContext。
 * 【概念】Context smoke test 可及早攔截 Bean 組態、相依注入與自動設定錯誤。
 */
class AccountServiceApplicationTest {

    /**
     * CASE-ACCOUNT-BOOT-001：Given 預設測試組態，When 載入 Account Service context，Then 不拋出例外。
     */
    @Test
    void contextLoads() {
    }
}

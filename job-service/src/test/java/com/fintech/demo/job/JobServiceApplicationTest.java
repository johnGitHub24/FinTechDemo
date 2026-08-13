package com.fintech.demo.job;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
/**
 * 【職責】驗證 Job Service 的 Spring Boot 應用程式內容可完整載入。
 * 【技巧】使用 @SpringBootTest 建立含排程組態的 ApplicationContext。
 * 【概念】排程服務須先保證相依 Bean 與組態可建立，才可安全觸發工作。
 */
class JobServiceApplicationTest {

    /**
     * CASE JOB-BOOT-001：Given 預設測試組態，When 載入 Job Service context，Then 不拋出例外。
     */
    @Test
    void contextLoads() {
    }
}

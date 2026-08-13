package com.fintech.demo.order.config;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 【職責】鎖定種子常數，與 FLOW-001 整合（DB 關聯）成對。
 * 【技巧】不啟動 Spring；只斷言 Demo 帳號契約。
 * 【概念】前後台 Demo 登入帳密必須與 Seeder 一致。
 */
@Tag("unit")
class DemoDataSeederTest {

    /**
     * CASE FLOW-001：種子交易者／管理員／密碼常數固定。
     */
    @Test
    void FLOW_001_seedConstants_matchDemoContract() {
        assertThat(DemoDataSeeder.TRADER1).isEqualTo("trader1");
        assertThat(DemoDataSeeder.ADMIN).isEqualTo("admin");
        assertThat(DemoDataSeeder.DEMO_PASSWORD).isEqualTo("password");
    }
}

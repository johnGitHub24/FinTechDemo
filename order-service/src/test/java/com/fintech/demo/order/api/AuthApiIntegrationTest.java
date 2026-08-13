package com.fintech.demo.order.api;

import com.fintech.demo.order.support.OrderIntegrationTestBase;
import com.fintech.demo.support.DemoTestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 【職責】Auth HTTP 整合測試：登入成功／錯誤密碼／缺欄位。
 * 【技巧】MockMvc + DemoTestFixtures JSON；套件放在 {@code /api/} 以便成對掃描歸入整合層。
 * 【概念】Case ID 與單元 AuthService／LoginRequest 驗證對齊。
 */
class AuthApiIntegrationTest extends OrderIntegrationTestBase {

    /**
     * CASE AUTH-001：合法帳密 → 200 + token。
     * Given: AUTH-001-SUCCESS；When: POST /api/auth/login；Then: 200 + token。
     */
    @Test
    void AUTH_001_login_returnsToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DemoTestFixtures.loadJson("auth", "AUTH-001-SUCCESS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.username").value("trader1"));
    }

    /**
     * CASE AUTH-002：錯誤密碼 → 401。
     * Given: AUTH-002-BAD_CREDENTIALS；When: login；Then: 401。
     */
    @Test
    void AUTH_002_badCredentials_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DemoTestFixtures.loadJson("auth", "AUTH-002-BAD_CREDENTIALS")))
                .andExpect(status().isUnauthorized());
    }

    /**
     * CASE AUTH-003：缺必填 → 400。
     * Given: AUTH-003-MISSING_REQUIRED；When: login；Then: 400。
     */
    @Test
    void AUTH_003_missingRequired_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DemoTestFixtures.loadJson("auth", "AUTH-003-MISSING_REQUIRED")))
                .andExpect(status().isBadRequest());
    }
}

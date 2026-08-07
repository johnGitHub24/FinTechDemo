package com.fintech.demo.order.api;

import com.fintech.demo.common.dto.RiskCheckResponse;
import com.fintech.demo.order.support.OrderIntegrationTestBase;
import com.fintech.demo.support.DemoTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 【職責】訂單 API 整合測試：Happy／驗證失敗／未授權（Fixture 驅動）。
 * 【技巧】DemoTestFixtures；風控 Mock 預設通過。
 * 【概念】對齊 EOS Case ID：ORDER-001／003／004 + SEC 無 Token。
 */
class OrderApiIntegrationTest extends OrderIntegrationTestBase {

    private String traderToken;

    @BeforeEach
    void setUp() throws Exception {
        when(riskClient.check(any())).thenReturn(RiskCheckResponse.ok());
        traderToken = loginAndGetToken(DemoTestFixtures.loadJson("auth", "AUTH-001-SUCCESS"));
    }

    /**
     * CASE ORDER-001：合法下單 → 201 PENDING。
     */
    @Test
    void ORDER_001_create_returns201() throws Exception {
        String body = DemoTestFixtures.loadJson("order", "ORDER-001-SUCCESS")
                .replace("fixture-order-001", "fixture-order-" + System.nanoTime());
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(traderToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.symbol").value("AAPL"));
    }

    /**
     * CASE ORDER-003：缺必填 → 400。
     */
    @Test
    void ORDER_003_missingRequired_returns400() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(traderToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DemoTestFixtures.loadJson("order", "ORDER-003-MISSING_REQUIRED")))
                .andExpect(status().isBadRequest());
    }

    /**
     * CASE ORDER-004：quantity 非法 → 400。
     */
    @Test
    void ORDER_004_invalidQuantity_returns400() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(traderToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DemoTestFixtures.loadJson("order", "ORDER-004-INVALID_FORMAT")))
                .andExpect(status().isBadRequest());
    }

    /**
     * CASE SEC-001：無 Token 存取訂單 → 401。
     */
    @Test
    void SEC_001_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * CASE ORDER-008：列表分頁結構。
     */
    @Test
    void ORDER_008_list_returnsPagedMeta() throws Exception {
        mockMvc.perform(get("/api/orders?page=0&size=10")
                        .header("Authorization", bearer(traderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.meta.page").value(0))
                .andExpect(jsonPath("$.meta.size").value(10));
    }
}

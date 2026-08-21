package com.fintech.demo.order.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fintech.demo.common.dto.RiskCheckResponse;
import com.fintech.demo.order.support.OrderIntegrationTestBase;
import com.fintech.demo.support.DemoTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 【職責】訂單 API 整合測試：Happy／驗證失敗／未授權／成交／取消（Fixture 驅動）。
 * 【技巧】DemoTestFixtures；風控 Mock 預設通過，ORDER-006 再改為拒絕。
 * 【概念】對齊單元 Case：ORDER-001 到 ORDER-008、SEC-001、JWT-001、JWT-002。
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
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.username").value("trader1"));
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
                .andExpect(jsonPath("$.meta.size").value(10))
                .andExpect(jsonPath("$.data[0].username").value("trader1"));
    }

    /**
     * CASE ORDER-002：重複 clientOrderId → 422。
     */
    @Test
    void ORDER_002_duplicateClientOrderId_returns422() throws Exception {
        String id = "dup-" + System.nanoTime();
        String body = DemoTestFixtures.loadJson("order", "ORDER-001-SUCCESS")
                .replace("fixture-order-001", id);
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(traderToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(traderToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    /**
     * CASE ORDER-005：風控通過 → 執行 ACCEPTED。
     */
    @Test
    void ORDER_005_execute_whenRiskAllows_returnsAccepted() throws Exception {
        long orderId = createPending("exec-ok-" + System.nanoTime());
        mockMvc.perform(post("/api/orders/{id}/execute", orderId)
                        .header("Authorization", bearer(traderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    /**
     * CASE ORDER-006：風控拒絕 → 執行 REJECTED。
     */
    @Test
    void ORDER_006_execute_whenRiskRejects_returnsRejected() throws Exception {
        when(riskClient.check(any())).thenReturn(RiskCheckResponse.reject("notional exceeds max"));
        long orderId = createPending("exec-rej-" + System.nanoTime());
        mockMvc.perform(post("/api/orders/{id}/execute", orderId)
                        .header("Authorization", bearer(traderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    /**
     * CASE ORDER-007：取消 PENDING → CANCELLED。
     */
    @Test
    void ORDER_007_cancelPending_returnsCancelled() throws Exception {
        long orderId = createPending("cxl-" + System.nanoTime());
        mockMvc.perform(delete("/api/orders/{id}", orderId)
                        .header("Authorization", bearer(traderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    /**
     * CASE JWT-001：有效 Token 可讀訂單列表。
     */
    @Test
    void JWT_001_validToken_canListOrders() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .header("Authorization", bearer(traderToken)))
                .andExpect(status().isOk());
    }

    /**
     * CASE JWT-002：格式無效 Token → 401。
     */
    @Test
    void JWT_002_malformedToken_returns401() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    private long createPending(String clientOrderId) throws Exception {
        String body = DemoTestFixtures.loadJson("order", "ORDER-001-SUCCESS")
                .replace("fixture-order-001", clientOrderId);
        String json = mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(traderToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(json);
        return node.get("id").asLong();
    }
}

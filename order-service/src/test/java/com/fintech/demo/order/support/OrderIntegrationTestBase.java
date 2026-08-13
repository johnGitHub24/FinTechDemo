package com.fintech.demo.order.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.demo.order.client.AccountClient;
import com.fintech.demo.order.client.RiskClient;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 【職責】order-service 整合測試基底：完整 Spring Boot + MockMvc；外部 Feign 以 MockBean 隔離。
 * 【技巧】{@code @Tag("integration")}；子類專注 CASE；提供 login／bearer 輔助。
 * 【概念】對齊 TradingCRUD IntegrationTestBase；契約測與單元 Case ID 成對。
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class OrderIntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockBean
    protected RiskClient riskClient;

    @MockBean
    protected AccountClient accountClient;

    protected String loginAndGetToken(String jsonBody) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(body);
        return node.get("token").asText();
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }
}

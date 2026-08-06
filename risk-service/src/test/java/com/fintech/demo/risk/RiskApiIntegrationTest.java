package com.fintech.demo.risk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
/**
 * 【職責】驗證風控檢查 HTTP API 的 JSON 繫結與 allowed 回應契約。
 * 【技巧】使用 MockMvc 送出真實 JSON 請求，並以 JSONPath 驗證回應內容。
 * 【概念】API 整合測試保護 Controller、服務層與序列化格式的協作。
 */
class RiskApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * CASE-RISK-API-001：Given 現金足夠且未超過限額的買單，When 呼叫風控端點，Then 回傳 allowed=true。
     */
    @Test
    void checkEndpoint_shouldReturnAllowed() throws Exception {
        String body = """
                {
                  "userId": 1,
                  "symbol": "AAPL",
                  "side": "BUY",
                  "quantity": 1,
                  "price": 100,
                  "cashBalance": 1000
                }
                """;
        mockMvc.perform(post("/api/risk/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true));
    }

    /**
     * CASE-RISK-API-002：Given 現金不足的買單，When 呼叫風控端點，Then 回傳 allowed=false。
     */
    @Test
    void checkEndpoint_shouldRejectOverCash() throws Exception {
        String body = """
                {
                  "userId": 1,
                  "symbol": "AAPL",
                  "side": "BUY",
                  "quantity": 10,
                  "price": 100,
                  "cashBalance": 50
                }
                """;
        mockMvc.perform(post("/api/risk/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(false));
    }
}

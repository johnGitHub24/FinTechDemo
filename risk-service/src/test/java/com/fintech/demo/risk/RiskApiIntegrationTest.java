package com.fintech.demo.risk;

import com.fintech.demo.support.DemoTestFixtures;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 【職責】風控 API 整合測試（Fixture 驅動）。
 * 【技巧】DemoTestFixtures 載入 RISK-001／002。
 * 【概念】與 RiskServiceTest 單元 Case 成對。
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
class RiskApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * CASE RISK-001：現金足夠 → allowed=true。
     */
    @Test
    void RISK_001_check_shouldAllow() throws Exception {
        mockMvc.perform(post("/api/risk/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DemoTestFixtures.loadJson("risk", "RISK-001-PASS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true));
    }

    /**
     * CASE RISK-002：現金不足 → allowed=false。
     */
    @Test
    void RISK_002_check_shouldReject() throws Exception {
        mockMvc.perform(post("/api/risk/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DemoTestFixtures.loadJson("risk", "RISK-002-REJECT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(false));
    }
}

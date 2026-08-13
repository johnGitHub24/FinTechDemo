package com.fintech.demo.risk.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.demo.common.dto.RiskCheckRequest;
import com.fintech.demo.common.dto.RiskCheckResponse;
import com.fintech.demo.support.DemoTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 【職責】覆蓋 RiskService 現金與名義金額風控規則（與 Fixture Case 成對）。
 * 【技巧】RISK-001 與 RISK-002 由 DemoTestFixtures 載入；另測 RISK-003 超額名目。
 * 【概念】風控規則改動時，這裡是最快的迴歸網。
 */
class RiskServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RiskService riskService;

    @BeforeEach
    void setUp() {
        riskService = new RiskService(new BigDecimal("50000"));
    }

    /**
     * CASE RISK-001：Given RISK-001-PASS When check Then allowed。
     */
    @Test
    void RISK_001_buyWithinCash_shouldAllow() throws Exception {
        RiskCheckRequest req = MAPPER.readValue(
                DemoTestFixtures.loadJson("risk", "RISK-001-PASS"), RiskCheckRequest.class);
        assertThat(riskService.check(req).allowed()).isTrue();
    }

    /**
     * CASE RISK-002：Given RISK-002-REJECT When check Then reject。
     */
    @Test
    void RISK_002_buyOverCash_shouldReject() throws Exception {
        RiskCheckRequest req = MAPPER.readValue(
                DemoTestFixtures.loadJson("risk", "RISK-002-REJECT"), RiskCheckRequest.class);
        RiskCheckResponse resp = riskService.check(req);
        assertThat(resp.allowed()).isFalse();
        assertThat(resp.reason()).contains("insufficient cash");
    }

    /**
     * CASE RISK-003：Given 名義金額超過上限 When check Then reject（max）。
     */
    @Test
    void RISK_003_overMaxNotional_shouldReject() {
        RiskCheckRequest req = request("BUY", 1000, "100", "1000000");
        RiskCheckResponse resp = riskService.check(req);
        assertThat(resp.allowed()).isFalse();
        assertThat(resp.reason()).contains("max");
    }

    private RiskCheckRequest request(String side, int qty, String price, String cash) {
        RiskCheckRequest req = new RiskCheckRequest();
        req.setUserId(1L);
        req.setSymbol("AAPL");
        req.setSide(side);
        req.setQuantity(qty);
        req.setPrice(new BigDecimal(price));
        req.setCashBalance(new BigDecimal(cash));
        return req;
    }
}

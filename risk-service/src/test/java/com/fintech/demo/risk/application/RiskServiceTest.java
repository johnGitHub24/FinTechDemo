package com.fintech.demo.risk.application;

import com.fintech.demo.common.dto.RiskCheckRequest;
import com.fintech.demo.common.dto.RiskCheckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 【職責】覆蓋 RiskService 現金與名義金額風控規則。
 * 【技巧】純單元：構造 RiskCheckRequest 驗證 allow／reject。
 * 【概念】風控規則改動時，這裡是最快的迴歸網。
 */
class RiskServiceTest {

    private RiskService riskService;

    @BeforeEach
    void setUp() {
        riskService = new RiskService(new BigDecimal("50000"));
    }

    /**
     * CASE-RISK-001：Given 買入名義金額小於現金與上限 When check Then allowed。
     */
    @Test
    void buyWithinCashAndLimit_shouldAllow() {
        RiskCheckRequest req = request("BUY", 10, "100", "5000");
        assertThat(riskService.check(req).allowed()).isTrue();
    }

    /**
     * CASE-RISK-002：Given 買入超過現金 When check Then reject（insufficient cash）。
     */
    @Test
    void buyOverCash_shouldReject() {
        RiskCheckRequest req = request("BUY", 10, "100", "500");
        RiskCheckResponse resp = riskService.check(req);
        assertThat(resp.allowed()).isFalse();
        assertThat(resp.reason()).contains("insufficient cash");
    }

    /**
     * CASE-RISK-003：Given 名義金額超過上限 When check Then reject（max）。
     */
    @Test
    void overMaxNotional_shouldReject() {
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

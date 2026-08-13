package com.fintech.demo.account.api;

import com.fintech.demo.account.application.AccountLedgerService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 【職責】內部入帳 API 整合測試，與 LEDGER 單元 Case 成對。
 * 【技巧】{@code @Transactional} 回滾，避免污染 ACCOUNT-001 種子餘額。
 * 【概念】Feign／Job 走 X-Internal-Token，不經使用者 JWT。
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InternalAccountApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountLedgerService ledgerService;

    /**
     * CASE LEDGER-001：現金足夠且無持倉，內部 BUY 入帳 → 扣現金。
     */
    @Test
    void LEDGER_001_internalBuy_deductsCash() throws Exception {
        ledgerService.seedAccount(9001L, new BigDecimal("1000.00"), "TWD");
        mockMvc.perform(post("/api/internal/accounts/9001/apply-trade")
                        .header("X-Internal-Token", "demo-job-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":1,"userId":9001,"symbol":"NVDA","side":"BUY","quantity":1,"price":100.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashBalance").value(900.0));
    }

    /**
     * CASE LEDGER-002：既有持倉 SELL 入帳 → 加現金。
     */
    @Test
    void LEDGER_002_internalSell_addsCash() throws Exception {
        ledgerService.seedAccount(9002L, new BigDecimal("1000.00"), "TWD");
        ledgerService.seedPosition(9002L, "AAPL", 10, new BigDecimal("150.00"));
        mockMvc.perform(post("/api/internal/accounts/9002/apply-trade")
                        .header("X-Internal-Token", "demo-job-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":2,"userId":9002,"symbol":"AAPL","side":"SELL","quantity":1,"price":160.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashBalance").value(1160.0));
    }

    /**
     * CASE LEDGER-003：現金不足 BUY → 422。
     */
    @Test
    void LEDGER_003_insufficientCash_returns422() throws Exception {
        ledgerService.seedAccount(9003L, new BigDecimal("10.00"), "TWD");
        mockMvc.perform(post("/api/internal/accounts/9003/apply-trade")
                        .header("X-Internal-Token", "demo-job-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":3,"userId":9003,"symbol":"AAPL","side":"BUY","quantity":10,"price":150.00}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

}

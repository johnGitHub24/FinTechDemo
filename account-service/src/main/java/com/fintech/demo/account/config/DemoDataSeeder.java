package com.fintech.demo.account.config;

import com.fintech.demo.account.application.AccountLedgerService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 【職責】寫入 Demo 帳戶／持倉：userId=1 現金 85000＋AAPL 100@150；userId=2 現金 100000。
 * 【技巧】委派 LedgerService.seed*，重複啟動 idempotent。
 * 【概念】與 order-service seeder 對齊數字，方便跨服務對帳 Demo。
 */
@Component
public class DemoDataSeeder implements ApplicationRunner {

    private final AccountLedgerService ledgerService;

    public DemoDataSeeder(AccountLedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ledgerService.seedAccount(1L, new BigDecimal("85000.00"), "TWD");
        ledgerService.seedPosition(1L, "AAPL", 100, new BigDecimal("150.0000"));
        ledgerService.seedAccount(2L, new BigDecimal("100000.00"), "TWD");
    }
}

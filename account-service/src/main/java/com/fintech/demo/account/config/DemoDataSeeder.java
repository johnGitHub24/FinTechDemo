package com.fintech.demo.account.config;

import com.fintech.demo.account.application.AccountLedgerService;
import com.fintech.demo.account.application.AccountQueryService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 【職責】寫入 Demo 帳戶／持倉：userId=1 現金 85000＋AAPL 100@150；userId=2 現金 100000。
 * 【技巧】委派 LedgerService.seed*，再經 QueryService 回填 Redis，DataGrip 才看得到 key。
 * 【概念】與 order-service seeder 對齊數字，方便跨服務對帳 Demo。
 */
@Component
public class DemoDataSeeder implements ApplicationRunner {

    private final AccountLedgerService ledgerService;
    private final AccountQueryService queryService;

    public DemoDataSeeder(AccountLedgerService ledgerService, AccountQueryService queryService) {
        this.ledgerService = ledgerService;
        this.queryService = queryService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ledgerService.seedAccount(1L, new BigDecimal("85000.00"), "TWD");
        ledgerService.seedPosition(1L, "AAPL", 100, new BigDecimal("150.0000"));
        ledgerService.seedAccount(2L, new BigDecimal("100000.00"), "TWD");
        // 細節：種子寫 DB 不會自動進 cache；主動讀一次讓 Redis 有 account:1／positions:1
        queryService.getAccount(1L);
        queryService.listPositions(1L);
        queryService.getAccount(2L);
        queryService.listPositions(2L);
    }
}

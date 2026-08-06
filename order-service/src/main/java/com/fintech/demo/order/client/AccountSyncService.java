package com.fintech.demo.order.client;

import com.fintech.demo.common.dto.AccountDto;
import com.fintech.demo.common.dto.ApplyTradeRequest;
import com.fintech.demo.common.event.TradeExecutedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 【職責】同步通知 account-service 入帳（Kafka 關閉時的分散式路徑）。
 * 【技巧】feign-sync 可關；失敗只打 warn，不阻斷本機 order 帳本（standalone Demo）。
 * 【概念】Kafka on → 走 trade-events；Kafka off + feign-sync → Feign 強一致寫 account。
 */
@Service
public class AccountSyncService {

    private static final Logger log = LoggerFactory.getLogger(AccountSyncService.class);

    private final AccountClient accountClient;
    private final boolean feignSync;
    private final boolean kafkaEnabled;
    private final String internalToken;

    public AccountSyncService(
            AccountClient accountClient,
            @Value("${fintech.account.feign-sync:false}") boolean feignSync,
            @Value("${fintech.kafka.enabled:false}") boolean kafkaEnabled,
            @Value("${fintech.job.token:demo-job-token}") String internalToken) {
        this.accountClient = accountClient;
        this.feignSync = feignSync;
        this.kafkaEnabled = kafkaEnabled;
        this.internalToken = internalToken;
    }

    /**
     * 【職責】在 Feign 同步模式讀取 account-service 的現金餘額。
     * 【技巧】Kafka 已啟用或同步開關關閉時直接略過；遠端失敗記錄 warn 並降級為 null。
     * 【概念】此讀取為可選跨服務整合，讓單體本機 Demo 不依賴 account-service。
     */
    public AccountDto fetchCash(Long userId) {
        if (!feignSync || kafkaEnabled) {
            return null;
        }
        try {
            return accountClient.getAccount(userId, internalToken);
        } catch (Exception ex) {
            log.warn("account-service getAccount failed userId={}: {}", userId, ex.getMessage());
            return null;
        }
    }

    /**
     * 【職責】把成交事件同步轉為 account-service 的入帳請求。
     * 【技巧】只在非 Kafka 的 Feign 模式組裝 ApplyTradeRequest，並帶內部 token 呼叫。
     * 【概念】Kafka 與同步 HTTP 是兩條互斥的服務整合路徑，避免同筆成交重複入帳。
     */
    public void syncTrade(TradeExecutedEvent event) {
        if (!feignSync || kafkaEnabled) {
            return;
        }
        try {
            ApplyTradeRequest req = new ApplyTradeRequest();
            req.setOrderId(event.orderId());
            req.setUserId(event.userId());
            req.setSymbol(event.symbol());
            req.setSide(event.side());
            req.setQuantity(event.quantity());
            req.setPrice(event.price());
            req.setNotional(event.notional());
            accountClient.applyTrade(event.userId(), req, internalToken);
        } catch (Exception ex) {
            log.warn("account-service applyTrade failed orderId={}: {}", event.orderId(), ex.getMessage());
        }
    }
}

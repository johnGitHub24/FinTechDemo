package com.fintech.demo.account.kafka;

import com.fintech.demo.account.application.AccountLedgerService;
import com.fintech.demo.account.application.AccountQueryService;
import com.fintech.demo.common.ApiConstants;
import com.fintech.demo.common.event.TradeExecutedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 【職責】消費 trade-events → 入帳並清 Redis cache。
 * 【技巧】@ConditionalOnProperty：預設關閉，避免本機無 broker 時 listener 連線。
 * 【概念】最終一致：order 先標 ACCEPTED 再發事件；account 異步更新帳本。
 */
@Component
@ConditionalOnProperty(name = "fintech.kafka.enabled", havingValue = "true")
public class TradeEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TradeEventConsumer.class);

    private final AccountLedgerService ledgerService;
    private final AccountQueryService queryService;

    public TradeEventConsumer(AccountLedgerService ledgerService, AccountQueryService queryService) {
        this.ledgerService = ledgerService;
        this.queryService = queryService;
    }

    /**
     * 【職責】消費成交事件，更新帳本後淘汰使用者讀側快取。
     * 【技巧】依序呼叫 ledgerService.applyTrade 與 queryService.evict，保持事件處理的寫後失效順序。
     * 【概念】這是 account-service 實現最終一致帳本同步的消費端。
     */
    @KafkaListener(topics = ApiConstants.TOPIC_TRADE_EVENTS, groupId = "fintech-account")
    public void onMessage(TradeExecutedEvent event) {
        log.info("trade-events orderId={} userId={} {} {} x{}",
                event.orderId(), event.userId(), event.side(), event.symbol(), event.quantity());
        ledgerService.applyTrade(event);
        queryService.evict(event.userId());
    }
}

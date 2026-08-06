package com.fintech.demo.order.kafka;

import com.fintech.demo.common.ApiConstants;
import com.fintech.demo.common.event.OrderExecuteCommand;
import com.fintech.demo.order.application.TradingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 【職責】消費 order-events → 呼叫 risk／帳務編排（execute）。
 * 【技巧】同進程 Consumer；成交後 TradingService 再發 trade-events 給 account-service。
 * 【概念】Event bus 鏈式：order-events →（風控+改狀態）→ trade-events → account 入帳+清 cache。
 */
@Component
@ConditionalOnProperty(name = "fintech.kafka.enabled", havingValue = "true")
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final TradingService tradingService;

    public OrderEventConsumer(TradingService tradingService) {
        this.tradingService = tradingService;
    }

    /**
     * 【職責】消費 order-events 並觸發訂單成交編排。
     * 【技巧】將 orderId 與 userId 原樣交給 TradingService，保留原有擁有權與狀態檢查。
     * 【概念】Consumer 將非同步命令轉回受交易保護的業務服務呼叫。
     */
    @KafkaListener(topics = ApiConstants.TOPIC_ORDER_EVENTS, groupId = "fintech-order")
    public void onMessage(OrderExecuteCommand command) {
        log.info("order-events consume orderId={} userId={} → execute (risk + ledger + trade-events)",
                command.orderId(), command.userId());
        tradingService.execute(command.userId(), command.orderId());
    }
}

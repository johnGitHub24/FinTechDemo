package com.fintech.demo.order.kafka;

import com.fintech.demo.common.ApiConstants;
import com.fintech.demo.common.event.TradeExecutedEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 【職責】成交後發 trade-events，驅動 account-service 入帳／清快取。
 * 【技巧】afterCommit 再送，避免交易回滾仍入帳。
 * 【概念】Event bus：order＝Producer；account＝Consumer；topic 解耦服務。
 */
@Component
@ConditionalOnProperty(name = "fintech.kafka.enabled", havingValue = "true")
public class TradeEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TradeEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * 【職責】在成交交易提交後發布 trade-events。
     * 【技巧】使用 TransactionSynchronization 確保回滾的成交不會送往帳戶服務。
     * 【概念】事件發佈將 order 的成交決策與 account 的帳本寫入解耦。
     */
    public void publishAfterCommit(TradeExecutedEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(event);
                }
            });
        } else {
            send(event);
        }
    }

    /**
     * 【職責】以使用者為 key 發送成交事件至 trade-events topic。
     * 【技巧】KafkaTemplate 統一使用 ApiConstants topic，避免字串散落。
     * 【概念】同一使用者事件使用相同 key 有助於帳本消費端維持順序。
     */
    private void send(TradeExecutedEvent event) {
        kafkaTemplate.send(
                ApiConstants.TOPIC_TRADE_EVENTS,
                String.valueOf(event.userId()),
                event);
    }
}

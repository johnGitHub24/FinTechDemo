package com.fintech.demo.order.kafka;

import com.fintech.demo.common.ApiConstants;
import com.fintech.demo.common.event.OrderExecuteCommand;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 【職責】下單後發 Kafka（交易提交後 afterCommit）。
 * 【技巧】配合同套件 Service／Controller 使用。
 * 【概念】教學 Demo 以可講清邊界為優先。
 */
@Component
@ConditionalOnProperty(name = "fintech.kafka.enabled", havingValue = "true")
public class OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * 【職責】建立下單執行命令，並在本地交易提交後發送 order-events。
     * 【技巧】若交易同步已啟用則註冊 afterCommit callback，否則直接發送以支援非交易呼叫。
     * 【概念】Outbox 的簡化版 after-commit 發送可避免資料回滾後仍觸發非同步成交。
     */
    public void publishAfterCommit(Long orderId, Long userId) {
        OrderExecuteCommand cmd = new OrderExecuteCommand(orderId, userId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(cmd);
                }
            });
        } else {
            send(cmd);
        }
    }

    /**
     * 【職責】依訂單主鍵發送 order-events 命令。
     * 【技巧】以 orderId 作 Kafka key，讓同一訂單具有可預期的 partition 歸屬。
     * 【概念】訊息 key 是維持同一業務聚合事件順序的基礎。
     */
    private void send(OrderExecuteCommand cmd) {
        kafkaTemplate.send(ApiConstants.TOPIC_ORDER_EVENTS, String.valueOf(cmd.orderId()), cmd);
    }
}

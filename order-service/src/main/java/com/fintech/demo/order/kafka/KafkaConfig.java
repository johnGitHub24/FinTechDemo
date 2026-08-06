package com.fintech.demo.order.kafka;

import com.fintech.demo.common.event.OrderExecuteCommand;
import com.fintech.demo.common.event.TradeExecutedEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

/**
 * 【職責】Kafka Topic 與 Template（僅 kafka.enabled=true）。
 * 【技巧】以 @Configuration／Properties 外置環境差異。
 * 【概念】order-events＝執行編排；trade-events＝通知 account-service 入帳（跨服務 event bus）。
 */
@Configuration
@ConditionalOnProperty(name = "fintech.kafka.enabled", havingValue = "true")
public class KafkaConfig {

    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name(com.fintech.demo.common.ApiConstants.TOPIC_ORDER_EVENTS).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic tradeEventsTopic() {
        return TopicBuilder.name(com.fintech.demo.common.ApiConstants.TOPIC_TRADE_EVENTS).partitions(1).replicas(1).build();
    }

    /**
     * 共用 JSON KafkaTemplate；Boot 預設 ProducerFactory 即可序列化各 event DTO。
     */
    @Bean
    public KafkaTemplate<String, Object> fintechKafkaTemplate(
            ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}

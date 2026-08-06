package com.fintech.demo.account.kafka;

import com.fintech.demo.common.ApiConstants;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * 【職責】確保 trade-events Topic 存在（僅 kafka.enabled=true）。
 * 【技巧】以 @Configuration／Properties 外置環境差異。
 * 【概念】Producer（order）與 Consumer（account）都可宣告 Topic，idempotent。
 */
@Configuration
@ConditionalOnProperty(name = "fintech.kafka.enabled", havingValue = "true")
public class KafkaConfig {

    @Bean
    public NewTopic tradeEventsTopic() {
        return TopicBuilder.name(ApiConstants.TOPIC_TRADE_EVENTS).partitions(1).replicas(1).build();
    }
}

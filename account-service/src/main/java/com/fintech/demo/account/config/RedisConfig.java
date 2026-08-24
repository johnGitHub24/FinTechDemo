package com.fintech.demo.account.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * 【職責】標記 Redis 功能開啟（實際連線由 RedisAutoConfiguration 提供）。
 * 【技巧】僅在 fintech.redis.enabled=true 時載入；本機 Demo 預設開，測試再關。
 * 【概念】只有 account 連 Redis；無容器時 QueryService 連線失敗會降級打 H2。
 */
@Configuration
@ConditionalOnProperty(name = "fintech.redis.enabled", havingValue = "true")
public class RedisConfig {
}

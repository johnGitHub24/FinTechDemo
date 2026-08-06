package com.fintech.demo.account.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * 【職責】標記 Redis 功能開啟（實際連線由 RedisAutoConfiguration 提供）。
 * 【技巧】僅在 fintech.redis.enabled=true 時載入；預設 profile 排除 RedisAutoConfiguration。
 * 【概念】可關降級：本機無 Redis 仍可直接打 DB，Demo 再開 cache。
 */
@Configuration
@ConditionalOnProperty(name = "fintech.redis.enabled", havingValue = "true")
public class RedisConfig {
}

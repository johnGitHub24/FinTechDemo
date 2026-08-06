package com.fintech.demo.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * 【職責】account-service 啟動入口（帳戶／持倉、Redis 快取、Kafka 入帳）。
 * 【技巧】@EnableKafka 常駐；實際 Listener／Topic 以 fintech.kafka.enabled 條件載入，本機可關 Kafka。
 * 【概念】第三業務微服務（:8084）：order 發 trade-events → 本服務入帳並清 cache。
 */
@SpringBootApplication
@EnableKafka
public class AccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }
}

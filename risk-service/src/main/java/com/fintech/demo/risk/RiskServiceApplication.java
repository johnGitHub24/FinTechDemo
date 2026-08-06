package com.fintech.demo.risk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 【職責】risk-service 啟動入口（名義金額風控 — P5）。
 * 【技巧】@SpringBootApplication 啟動自動組態。
 * 【概念】P0 僅健康檢查骨架，對齊 SPEC 埠 8082。
 */
@SpringBootApplication
public class RiskServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RiskServiceApplication.class, args);
    }
}

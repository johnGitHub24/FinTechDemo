package com.fintech.demo.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 【職責】API Gateway 統一入口（P4 補路由；P0 可啟動）。
 * 【技巧】@SpringBootApplication 啟動自動組態。
 * 【概念】採 Spring Cloud Gateway MVC（對齊 TradingSpringCloud）；埠 8080。
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}

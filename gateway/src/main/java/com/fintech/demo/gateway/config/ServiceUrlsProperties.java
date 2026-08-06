package com.fintech.demo.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 【職責】綁定下游服務基底 URL（order／risk／account），供 {@link GatewayRouteConfig} 路由轉發。
 * 【技巧】Java {@code record} + {@code @ConfigurationProperties}（Spring Boot 3 constructor binding）：
 *         編譯器自動產生不可變元件、{@code orderUrl()}／{@code riskUrl()}／{@code accountUrl()}、
 *         equals／hashCode／toString，免手寫 getter／setter；缺省值用 {@link DefaultValue}。
 * 【概念】為何用 record？
 *         （1）不可變組態／契約：啟動後不應被執行期改寫 URL，避免路由中途被誤改。
 *         （2）Properties、API 回應、Kafka 事件同屬「組裝後唯讀」——優先 record。
 *         （3）對照：JPA Entity、需逐步 {@code setXxx} 的 Request DTO 仍用 class + Lombok。
 *         呼叫端見 {@link GatewayRouteConfig}：注入本 record 後用 {@code urls.orderUrl()}／{@code accountUrl()}。
 */
@ConfigurationProperties(prefix = "fintech.services")
public record ServiceUrlsProperties(
        @DefaultValue("http://localhost:8081") String orderUrl,
        @DefaultValue("http://localhost:8082") String riskUrl,
        @DefaultValue("http://localhost:8084") String accountUrl) {
}

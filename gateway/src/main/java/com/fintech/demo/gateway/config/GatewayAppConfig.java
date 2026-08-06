package com.fintech.demo.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * 【職責】啟用 {@link ServiceUrlsProperties} 並提供 Gateway MVC 轉發用 HTTP 用戶端。
 * 【技巧】{@code @EnableConfigurationProperties(ServiceUrlsProperties.class)} 註冊 record 型 Properties；
 *         路由實際取值在 {@link GatewayRouteConfig}（{@code urls.orderUrl()}／{@code accountUrl()}）。
 * 【概念】組態用不可變 record（啟動後不改寫）；與業務邏輯分離，本機／Docker／K8s 才好用環境變數覆寫 URL。
 */
@Configuration
@EnableConfigurationProperties(ServiceUrlsProperties.class)
public class GatewayAppConfig {

    /**
     * 【職責】建立轉發下游用的 JDK HttpClient 工廠。
     * 【技巧】HTTP/1.1 + connectTimeout，對齊 TradingSpringCloud Gateway MVC 轉發模式。
     * 【概念】Gateway 不持業務狀態，只負責可靠地把請求送到下游。
     */
    @Bean
    public ClientHttpRequestFactory gatewayClientHttpRequestFactory() {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        return new JdkClientHttpRequestFactory(httpClient);
    }
}

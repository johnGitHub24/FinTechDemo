package com.fintech.demo.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.addRequestHeader;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

/**
 * 【職責】依路徑拆到業務微服務（分散式 HTTP 入口）。
 * 【技巧】注入 {@link ServiceUrlsProperties} record，以元件存取子取值（非 getXxx）：
 *         {@code urls.orderUrl()}、{@code urls.accountUrl()}（對應 yml {@code fintech.services.*-url}）。
 *         較具體的 account 路由先註冊，避免被 {@code /api/**} 吃掉。
 * 【概念】為何這裡用 record Properties？
 *         組態屬「啟動後不可變」；Gateway 路由建立時讀一次即可。
 *         前端只打 Gateway；JWT 仍由各下游驗證。
 *         （Entity／需 setXxx 的 Request 不用 record，見 order-service Entity／CreateOrderRequest。）
 */
@Configuration
public class GatewayRouteConfig {

    /**
     * 【職責】帳戶／持倉路徑 → account-service。
     * 【技巧】{@code http(urls.accountUrl())}：record 元件存取子；compose／K8s 可用環境變數覆寫 URL。
     * 【概念】具體路徑優先，避免被通用 {@code /api/**} 攔截。
     */
    @Bean
    public RouterFunction<ServerResponse> accountRoutes(ServiceUrlsProperties urls) {
        return route("account-api")
                .route(path("/api/accounts/**"), http(urls.accountUrl()))
                .route(path("/api/positions/**"), http(urls.accountUrl()))
                .route(path("/api/positions"), http(urls.accountUrl()))
                .before(addRequestHeader("X-Demo-Via-Gateway", "1"))
                .build();
    }

    /**
     * 【職責】其餘 {@code /api/**} → order-service。
     * 【技巧】{@code http(urls.orderUrl())}：與 {@link ServiceUrlsProperties#orderUrl()} 對應。
     * 【概念】統一前端入口，下游各自驗證 JWT；加 X-Demo-Via-Gateway 供 demoTrace。
     */
    @Bean
    public RouterFunction<ServerResponse> orderRoutes(ServiceUrlsProperties urls) {
        return route("order-api")
                .route(path("/api/**"), http(urls.orderUrl()))
                .before(addRequestHeader("X-Demo-Via-Gateway", "1"))
                .build();
    }
}

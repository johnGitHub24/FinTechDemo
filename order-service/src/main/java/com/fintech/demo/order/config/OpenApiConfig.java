package com.fintech.demo.order.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 【職責】OpenAPI／Swagger 設定（學習用 API 文件）。
 * 【技巧】Bearer JWT 一次 Authorize，之後 Try it out 自動帶 Token。
 * 【概念】對齊 TradingCRUD docs/swagger.html 的「可點可試」體驗。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fintechOpenApi() {
        final String scheme = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("FinTechDemo — order-service API")
                        .description("JWT 登入後：前台下單／後台歷史。account／risk 見 docs/openapi.yaml。")
                        .version("0.1.0"))
                .addSecurityItem(new SecurityRequirement().addList(scheme))
                .components(new Components().addSecuritySchemes(scheme,
                        new SecurityScheme()
                                .name(scheme)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}

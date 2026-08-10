package com.fintech.demo.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 【職責】Web MVC 設定：CORS 允許 Vue 前端跨域呼叫 API。
 * 【技巧】實作 {@link WebMvcConfigurer#addCorsMappings}；來源來自 {@code fintech.cors.allowed-origins}。
 * 【概念】瀏覽器同源政策會擋「前端 :5173 直連後端 :8081」；CORS 告訴瀏覽器哪些來源可跨域。
 *         開發亦可用 Vite proxy 免 CORS；本類讓「直連後端」與 Demo 敘事仍成立。
 * 【邊界】不負責認證（見 {@link SecurityConfig}）；CORS ≠ 授權。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${fintech.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private String allowedOrigins;

    /**
     * 【職責】註冊 {@code /api/**} CORS 規則。
     * 【技巧】{@code allowedOriginPatterns} 支援逗號分隔多來源；含 OPTIONS preflight。
     * 【概念】正式環境應收斂為明確前端網域，勿用過寬 pattern。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}

package com.fintech.demo.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 【職責】Gateway 入口輕量限流：依客戶端鍵做每秒固定視窗計數，超限回 429。
 * 【技巧】Gateway MVC（Servlet）用 {@link OncePerRequestFilter}；Demo 用進程內
 *         {@link ConcurrentHashMap} 計數（免強制 Redis）。類名對齊 APIGatewayMQ 的
 *         {@code RateLimitWebFilter}，方便口述「入口有限流」。
 * 【概念】限流放在 Filter 而非 Controller：所有進入 Gateway 的請求在轉發下游前先被保護。
 *         正式多副本應改 Redis INCR（見 APIGatewayMQ）；本 Demo 證明機制存在即可。
 * 【邊界】不做認證、不解析 body；閾值來自 {@code fintech.gateway.rate-limit.per-second}。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@ConditionalOnProperty(name = "fintech.gateway.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitWebFilter extends OncePerRequestFilter {

    private final int limitPerSecond;
    /** key = clientKey + ":" + epochSecond → count */
    private final Map<String, AtomicInteger> windows = new ConcurrentHashMap<>();

    public RateLimitWebFilter(
            @Value("${fintech.gateway.rate-limit.per-second:80}") int limitPerSecond) {
        this.limitPerSecond = Math.max(1, limitPerSecond);
    }

    /**
     * 【職責】對非 actuator／OPTIONS 請求做 1 秒視窗計數；超限寫 429 JSON。
     * 【技巧】window key 含 epoch 秒，自然過期；順便清掉非當秒的舊 key 避免 map 膨脹。
     * 【概念】固定視窗實作簡單，適合 Demo；邊界秒可能短暫突衝，與令牌桶不同。
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path != null && (path.startsWith("/actuator") || "OPTIONS".equalsIgnoreCase(request.getMethod()))) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientKey = resolveClientKey(request);
        long epochSec = System.currentTimeMillis() / 1000L;
        String windowKey = clientKey + ":" + epochSec;
        windows.keySet().removeIf(k -> !k.endsWith(":" + epochSec));

        int count = windows.computeIfAbsent(windowKey, k -> new AtomicInteger(0)).incrementAndGet();
        if (count > limitPerSecond) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "1");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            byte[] body = """
                    {"errorCode":"RATE_LIMIT_EXCEEDED","message":"Too many requests, retry later"}
                    """.getBytes(StandardCharsets.UTF_8);
            response.getOutputStream().write(body);
            return;
        }
        filterChain.doFilter(request, response);
    }

    /** 優先 X-Forwarded-For 第一個 IP，否則 remoteAddr。 */
    private String resolveClientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String remote = request.getRemoteAddr();
        return (remote == null || remote.isBlank()) ? "unknown" : remote;
    }
}

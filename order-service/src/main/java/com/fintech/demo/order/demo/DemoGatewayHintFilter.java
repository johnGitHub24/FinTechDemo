package com.fintech.demo.order.demo;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 【職責】讀取 Gateway 轉發標記，供 TradingService 組 demoTrace。
 * 【技巧】header {@code X-Demo-Via-Gateway: 1} → request attribute。
 * 【概念】讓 PROCESS FLOW 能顯示「有沒有經過 Gateway」。
 */
@Component
public class DemoGatewayHintFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Demo-Via-Gateway";
    public static final String ATTR = "demo.viaGateway";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        boolean via = "1".equals(request.getHeader(HEADER));
        request.setAttribute(ATTR, via);
        filterChain.doFilter(request, response);
    }
}

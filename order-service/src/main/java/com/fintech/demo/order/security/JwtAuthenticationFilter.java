package com.fintech.demo.order.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 【職責】Authorization: Bearer → SecurityContext。
 * 【技巧】配合同套件 Service／Controller 使用。
 * 【概念】教學 Demo 以可講清邊界為優先。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";
    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    /**
     * 【職責】將有效 Bearer JWT 轉換為 Spring Security 的 Authentication。
     * 【技巧】只有 Context 尚未有身分時才建立 authority，最後一律繼續 filter chain。
     * 【概念】Filter 是 HTTP token 與框架 SecurityContext 之間的轉接層。
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String token = resolve(request);
        if (token != null && tokenProvider.validateToken(token)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String username = tokenProvider.getUsername(token);
            List<SimpleGrantedAuthority> authorities = tokenProvider.getRoles(token).stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();
            var auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 【職責】從 Authorization header 擷取 Bearer token。
     * 【技巧】先檢查 header 有文字且以前綴開頭，再移除固定前綴。
     * 【概念】集中解析可避免各端點各自處理 header 格式而產生差異。
     */
    private String resolve(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith(BEARER)) {
            return header.substring(BEARER.length());
        }
        return null;
    }
}

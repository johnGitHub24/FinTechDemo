package com.fintech.demo.order.config;

import com.fintech.demo.order.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 【職責】無狀態 JWT FilterChain + RBAC（USER／ADMIN）。
 * 【技巧】PUBLIC 含 swagger／actuator／login；audit 限 ADMIN。
 * 【概念】前端 router 守衛只是 UX；真正授權在這裡。
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC = {
            "/api/auth/**",
            "/api/demo/**",
            "/api/internal/jobs/**",
            "/actuator/health",
            "/actuator/info",
            "/actuator/prometheus",
            "/h2-console/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/error"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * 【職責】建立無狀態 JWT 的 HTTP 授權規則與 filter chain。
     * 【技巧】停用 CSRF／Session；Audit 限 ADMIN。401／403 用 setStatus 寫 JSON，避免 {@code sendError}
     *         觸發 Tomcat ERROR 轉發／error 再被當成未登入而把 403 變成 401。
     * 【概念】真正的存取控制應在伺服器端執行，前端守衛只能改善使用體驗。
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                // 【技巧】啟用 CORS，讓 WebConfig 的映射在 Security 鏈生效（含 preflight OPTIONS）
                .cors(c -> {})
                .headers(h -> h.frameOptions(f -> f.disable()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC).permitAll()
                        .requestMatchers("/api/audit-logs", "/api/audit-logs/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> writeJson(res, HttpStatus.UNAUTHORIZED, "unauthorized"))
                        .accessDeniedHandler((req, res, e) -> writeJson(res, HttpStatus.FORBIDDEN, "forbidden")))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * 【職責】把安全例外寫成固定 JSON 狀態，不走 {@code sendError}。
     * 【技巧】直接 setStatus＋寫 body，讓已登入但缺角色的請求維持 403。
     * 【概念】Tomcat 對 sendError 會 ERROR dispatch；若 /error 仍需認證，使用者會看到 401。
     */
    private static void writeJson(HttpServletResponse res, HttpStatus status, String error) throws java.io.IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.getWriter().write("{\"error\":\"" + error + "\"}");
    }

    /**
     * 【職責】提供使用者密碼雜湊與驗證所需的 PasswordEncoder。
     * 【技巧】採用 BCryptPasswordEncoder，不保存明文密碼。
     * 【概念】密碼雜湊是帳密驗證與 JWT 簽發之前的必要安全邊界。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 【職責】公開 Spring Security 組裝完成的 AuthenticationManager。
     * 【技巧】從 AuthenticationConfiguration 取得，避免自行重建 provider 鏈。
     * 【概念】Service 透過此介面驗證帳密，與具體安全設定解耦。
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}

package com.fintech.demo.order.config;

import com.fintech.demo.order.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
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
            "/v3/api-docs/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * 【職責】建立無狀態 JWT 的 HTTP 授權規則與 filter chain。
     * 【技巧】停用 CSRF／Session，設定公開路徑、ADMIN 審計路徑與 JWT filter 的插入位置。
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
                        .requestMatchers("/api/audit-logs/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((req, res, e) -> res.sendError(HttpStatus.FORBIDDEN.value())))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
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

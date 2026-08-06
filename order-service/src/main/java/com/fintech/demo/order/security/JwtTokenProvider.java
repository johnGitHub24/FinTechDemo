package com.fintech.demo.order.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * 【職責】JWT 簽發／驗證（對齊 TradingSpringSecurity）。
 * 【技巧】配合同套件 Service／Controller 使用。
 * 【概念】教學 Demo 以可講清邊界為優先。
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * 【職責】簽發包含使用者、uid、角色與期限的 JWT。
     * 【技巧】使用 HMAC key 簽章，將 uid 與 roles 放入 claims，expiration 由設定控制。
     * 【概念】JWT 讓下游可在不查 Session 的情況下辨識使用者與授權角色。
     */
    public String generateToken(String username, Long userId, List<String> roles) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("uid", userId)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    /**
     * 【職責】判定傳入 JWT 是否可驗簽且結構合法。
     * 【技巧】透過共用 parse 集中驗證，攔截 JwtException 後回傳 false 而非洩漏例外。
     * 【概念】Filter 需要布林結果來維持無效 token 不建立 SecurityContext 的安全邊界。
     */
    public boolean validateToken(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT invalid: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * 【職責】從已驗簽 JWT 取得 subject 使用者名稱。
     * 【技巧】委派 parse，確保不會直接信任未簽章的 payload。
     * 【概念】subject 是 Spring Security principal 的可讀識別值。
     */
    public String getUsername(String token) {
        return parse(token).getSubject();
    }

    /**
     * 【職責】從 JWT claims 解析角色清單。
     * 【技巧】僅接受 List 型別；缺少或型別不符時回傳空集合。
     * 【概念】授權角色應來自已簽發的權杖 claims，而不是前端任意傳入的欄位。
     */
    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        Object roles = parse(token).get("roles");
        return roles instanceof List<?> list ? (List<String>) list : List.of();
    }

    /**
     * 【職責】驗簽並解析 JWT payload。
     * 【技巧】JJWT parser 綁定 HMAC key，驗簽失敗會由呼叫端統一處理。
     * 【概念】集中解析點可避免不同讀取方法採用不一致的安全規則。
     */
    private Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}

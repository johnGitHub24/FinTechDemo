package com.fintech.demo.account.security;

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
import java.util.List;
import java.util.Map;

/**
 * 【職責】驗證 order-service 簽發的 JWT（同 secret），解析 username／uid。
 * 【技巧】優先讀 claim {@code uid}；缺省時以 username→userId 對照（trader1→1、admin→2）。
 * 【概念】account 不簽發 token，只驗簽；跨 MS 共用 secret 是 Demo 簡化，正式應改 JWKS。
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private static final Map<String, Long> USERNAME_FALLBACK = Map.of(
            "trader1", 1L,
            "admin", 2L);

    private final SecretKey key;

    public JwtTokenProvider(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public boolean validateToken(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT invalid: {}", ex.getMessage());
            return false;
        }
    }

    public String getUsername(String token) {
        return parse(token).getSubject();
    }

    /**
     * 從 claim {@code uid} 取 userId；若無則 username fallback。
     */
    public Long getUserId(String token) {
        Claims claims = parse(token);
        Object uid = claims.get("uid");
        if (uid instanceof Number number) {
            return number.longValue();
        }
        if (uid instanceof String str && !str.isBlank()) {
            return Long.parseLong(str);
        }
        String username = claims.getSubject();
        Long mapped = USERNAME_FALLBACK.get(username);
        if (mapped != null) {
            return mapped;
        }
        throw new IllegalArgumentException("JWT missing uid claim for user: " + username);
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        Object roles = parse(token).get("roles");
        return roles instanceof List<?> list ? (List<String>) list : List.of();
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}

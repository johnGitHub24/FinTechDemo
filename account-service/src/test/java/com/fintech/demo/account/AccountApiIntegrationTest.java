package com.fintech.demo.account;

import com.fintech.demo.support.DemoTestFixtures;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 【職責】帳戶 API 整合測試（Fixture 文件對照 ACCOUNT Case）。
 * 【技巧】JWT 含 uid；ACCOUNT-* JSON 為契約註記（GET 無 body）。
 * 【概念】與單元／種子資料成對，保護 /api/accounts/me。
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
class AccountApiIntegrationTest {

    private static final String SECRET = "fintech-demo-dev-secret-key-32bytes!!";

    @Autowired
    private MockMvc mockMvc;

    /**
     * CASE ACCOUNT-002：無 Token → 401（對照 ACCOUNT-002-UNAUTHORIZED fixture 註記）。
     */
    @Test
    void ACCOUNT_002_withoutToken_shouldUnauthorized() throws Exception {
        DemoTestFixtures.loadJson("account", "ACCOUNT-002-UNAUTHORIZED");
        mockMvc.perform(get("/api/accounts/me"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * CASE ACCOUNT-001：有效 JWT → 種子帳戶（對照 ACCOUNT-001-SUCCESS）。
     */
    @Test
    void ACCOUNT_001_withJwt_shouldReturnSeedAccount() throws Exception {
        DemoTestFixtures.loadJson("account", "ACCOUNT-001-SUCCESS");
        String token = tokenWithUid("trader1", 1L, List.of("ROLE_USER"));
        mockMvc.perform(get("/api/accounts/me")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.cashBalance").value(85000.0));
    }

    private static String tokenWithUid(String username, long uid, List<String> roles) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .claim("uid", uid)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 3_600_000))
                .signWith(key)
                .compact();
    }
}

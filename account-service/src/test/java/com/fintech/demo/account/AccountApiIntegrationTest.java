package com.fintech.demo.account;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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

@SpringBootTest
@AutoConfigureMockMvc
/**
 * 【職責】驗證帳戶 HTTP API 的 JWT 驗證與目前使用者帳戶查詢。
 * 【技巧】以 MockMvc 模擬請求，並用相同 HMAC 密鑰簽發含 uid claim 的測試 JWT。
 * 【概念】整合測試保護 Security Filter、Controller 與種子帳戶資料的跨層契約。
 */
class AccountApiIntegrationTest {

    private static final String SECRET = "fintech-demo-dev-secret-key-32bytes!!";

    @Autowired
    private MockMvc mockMvc;

    /**
     * CASE-ACCOUNT-API-001：Given 未攜帶授權 Token，When 查詢目前帳戶，Then 回應 401 Unauthorized。
     */
    @Test
    void accountsMe_withoutToken_shouldUnauthorized() throws Exception {
        mockMvc.perform(get("/api/accounts/me"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * CASE-ACCOUNT-API-002：Given 含有效 uid 的 JWT，When 查詢目前帳戶，Then 回傳對應種子帳戶。
     */
    @Test
    void accountsMe_withJwtUid_shouldReturnSeedAccount() throws Exception {
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

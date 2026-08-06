package com.fintech.demo.order.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 【職責】驗證 JWT 的簽發、驗證與角色 Claim 解析。
 * 【技巧】以固定測試密鑰執行 token round-trip，避免依賴 Spring ApplicationContext。
 * 【概念】Token 必須能被驗簽並安全讀回授權資訊，無效格式則不可通過驗證。
 */
class JwtTokenProviderTest {

    /**
     * CASE-JWT-001：Given 有效使用者、識別與角色，When 簽發再驗證 JWT，Then 可讀回正確 Claim。
     */
    @Test
    void generateAndValidate_roundTrip() {
        JwtTokenProvider provider = new JwtTokenProvider(
                "fintech-demo-dev-secret-key-32bytes!!", 3600_000);
        String token = provider.generateToken("trader1", 1L, List.of("ROLE_USER"));
        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUsername(token)).isEqualTo("trader1");
        assertThat(provider.getRoles(token)).contains("ROLE_USER");
    }

    /**
     * CASE-JWT-002：Given 格式無效的字串，When 驗證 JWT，Then 回傳 false。
     */
    @Test
    void invalidToken_shouldFailValidation() {
        JwtTokenProvider provider = new JwtTokenProvider(
                "fintech-demo-dev-secret-key-32bytes!!", 3600_000);
        assertThat(provider.validateToken("not.a.jwt")).isFalse();
    }
}

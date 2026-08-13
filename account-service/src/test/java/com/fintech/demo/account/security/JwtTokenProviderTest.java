package com.fintech.demo.account.security;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 【職責】account-service 只驗簽、不簽發；與 ACCOUNT-002 HTTP 401 成對。
 * 【技巧】空白／非法 token 必須失敗，對齊 Filter 未帶 Token 的拒絕路徑。
 * 【概念】跨服務共用 secret 時，無效權杖不可被當成已驗證。
 */
@Tag("unit")
class JwtTokenProviderTest {

    /**
     * CASE ACCOUNT-002：無／空白 Token 驗證失敗（對應 HTTP 401）。
     */
    @Test
    void ACCOUNT_002_blankToken_isInvalid() {
        JwtTokenProvider provider = new JwtTokenProvider("fintech-demo-dev-secret-key-32bytes!!");
        assertThat(provider.validateToken("")).isFalse();
        assertThat(provider.validateToken("not.a.jwt")).isFalse();
    }
}

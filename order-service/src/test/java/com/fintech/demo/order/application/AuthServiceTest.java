package com.fintech.demo.order.application;

import com.fintech.demo.order.domain.Role;
import com.fintech.demo.order.dto.LoginRequest;
import com.fintech.demo.order.dto.LoginResponse;
import com.fintech.demo.order.infrastructure.UserEntity;
import com.fintech.demo.order.infrastructure.UserRepository;
import com.fintech.demo.order.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
/**
 * 【職責】驗證登入流程的驗證委派、使用者查詢與 JWT 回應組裝。
 * 【技巧】以 Mockito 模擬 AuthenticationManager、UserRepository 與 JwtTokenProvider。
 * 【概念】認證、使用者識別與權杖簽發應在服務層協作，Controller 不承擔安全細節。
 */
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    /**
     * CASE AUTH-001：Given 認證成功的使用者與角色，When 登入，Then 回傳 JWT、帳號及角色。
     * CASE JWT-001：回傳的 token 字串非空，整合層以同一權杖存取 API。
     * CASE SEC-001：登入契約假設後續請求必須帶 Token（無 Token 由 Filter 拒）。
     * CASE FLOW-002：未授權與 SEC-001 同一拒絕語意。
     */
    @Test
    void AUTH_001_login_shouldReturnBearerTokenAndRoles() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "trader1",
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setUsername("trader1");
        user.setRole(Role.USER);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findByUsername("trader1")).thenReturn(Optional.of(user));
        when(tokenProvider.generateToken("trader1", 1L, List.of("ROLE_USER"))).thenReturn("jwt-token");

        LoginRequest req = new LoginRequest();
        req.setUsername("trader1");
        req.setPassword("password");

        LoginResponse resp = authService.login(req);

        assertThat(resp.token()).isEqualTo("jwt-token");
        assertThat(resp.token()).isNotBlank();
        assertThat(resp.username()).isEqualTo("trader1");
        assertThat(resp.roles()).contains("ROLE_USER");
    }

    /**
     * CASE AUTH-002：Given AuthenticationManager 拒絕，When 登入，Then 拋 BadCredentialsException。
     * CASE JWT-002：失敗登入不簽發可用權杖，對應整合層無效 Token → 401。
     */
    @Test
    void AUTH_002_badCredentials_shouldPropagate() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad"));
        LoginRequest req = new LoginRequest();
        req.setUsername("trader1");
        req.setPassword("wrong");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }
}

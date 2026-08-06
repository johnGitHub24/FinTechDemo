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
     * CASE-AUTH-001：Given 認證成功的使用者與角色，When 登入，Then 回傳 JWT、帳號及角色。
     */
    @Test
    void login_shouldReturnBearerTokenAndRoles() {
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
        assertThat(resp.username()).isEqualTo("trader1");
        assertThat(resp.roles()).contains("ROLE_USER");
    }
}

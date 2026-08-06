package com.fintech.demo.order.application;

import com.fintech.demo.order.dto.LoginRequest;
import com.fintech.demo.order.dto.LoginResponse;
import com.fintech.demo.order.infrastructure.UserEntity;
import com.fintech.demo.order.infrastructure.UserRepository;
import com.fintech.demo.order.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 【職責】登入：驗證後簽 JWT（含 uid，供 account-service 跨服務辨識）。
 * 【技巧】讀多用 @Transactional(readOnly=true)；寫入走預設交易。
 * 【概念】Service 是 Demo 最常說明的「流程編排」層。
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtTokenProvider tokenProvider,
            UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
    }

    /**
     * 【職責】驗證使用者帳密並回傳含角色與 uid 的登入結果。
     * 【技巧】交由 AuthenticationManager 驗證，成功後從資料庫取得穩定 userId 再簽 JWT。
     * 【概念】登入流程把身分驗證與權杖簽發集中在 Service，Controller 無需知道安全細節。
     */
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        UserEntity user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        String token = tokenProvider.generateToken(authentication.getName(), user.getId(), roles);
        return new LoginResponse(token, authentication.getName(), roles);
    }
}

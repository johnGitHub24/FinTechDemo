package com.fintech.demo.order.api;

import com.fintech.demo.order.application.AuthService;
import com.fintech.demo.order.dto.LoginRequest;
import com.fintech.demo.order.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 【職責】公開登入 API。
 * 【技巧】只做參數／驗證／HTTP 狀態；商業規則在 Service。
 * 【概念】薄 Controller 利於測試與替換傳輸層。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 【職責】接收合法登入請求並回傳 JWT 登入結果。
     * 【技巧】以 @Valid 在呼叫 Service 前執行欄位格式驗證。
     * 【概念】公開登入端點是由帳密交換無狀態授權權杖的邊界。
     */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}

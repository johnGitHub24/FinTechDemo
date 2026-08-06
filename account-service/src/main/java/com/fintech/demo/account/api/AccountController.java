package com.fintech.demo.account.api;

import com.fintech.demo.account.application.AccountQueryService;
import com.fintech.demo.account.security.JwtTokenProvider;
import com.fintech.demo.common.dto.AccountDto;
import com.fintech.demo.common.dto.PositionDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * 【職責】JWT 保護的帳戶／持倉查詢 API。
 * 【技巧】credentials 存 raw JWT，再取 uid claim（或 username fallback）。
 * 【概念】Gateway 轉發時帶同一 Bearer，本服務獨立驗簽。
 */
@RestController
@RequestMapping("/api")
public class AccountController {

    private final AccountQueryService queryService;
    private final JwtTokenProvider tokenProvider;

    public AccountController(AccountQueryService queryService, JwtTokenProvider tokenProvider) {
        this.queryService = queryService;
        this.tokenProvider = tokenProvider;
    }

    /**
     * 【職責】查詢目前 JWT 使用者的帳戶資料。
     * 【技巧】從 SecurityContext 解析 uid，而非接受可竄改的 request userId。
     * 【概念】me 端點提供以登入身分為範圍的安全讀取介面。
     */
    @GetMapping("/accounts/me")
    public AccountDto me() {
        return queryService.getAccount(currentUserId());
    }

    /**
     * 【職責】查詢目前 JWT 使用者的持倉資料。
     * 【技巧】統一使用 currentUserId 解析可驗簽 token 的 uid claim。
     * 【概念】帳戶與持倉 API 應使用同一套身分邊界。
     */
    @GetMapping("/positions")
    public List<PositionDto> positions() {
        return queryService.listPositions(currentUserId());
    }

    /**
     * 【職責】從 SecurityContext 中保留的原始 JWT 解析使用者 uid。
     * 【技巧】先確認 Authentication 與 credentials 存在，再將解析失敗轉為 401。
     * 【概念】Controller 只負責 HTTP 身分轉接，帳務規則仍留在 Service。
     */
    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getCredentials() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not authenticated");
        }
        String token = auth.getCredentials().toString();
        try {
            return tokenProvider.getUserId(token);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ex.getMessage());
        }
    }
}

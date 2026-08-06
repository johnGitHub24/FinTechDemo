package com.fintech.demo.order.api;

import com.fintech.demo.common.dto.PageResponse;
import com.fintech.demo.order.application.TradingService;
import com.fintech.demo.order.dto.AccountResponse;
import com.fintech.demo.order.dto.AuditLogResponse;
import com.fintech.demo.order.dto.PositionResponse;
import com.fintech.demo.order.security.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 【職責】後台查詢；審計由 SecurityConfig 限 ADMIN。
 * 【技巧】只做參數／驗證／HTTP 狀態；商業規則在 Service。
 * 【概念】薄 Controller 利於測試與替換傳輸層。
 */
@RestController
@RequestMapping("/api")
public class PortalController {

    private final TradingService tradingService;
    private final CurrentUserService currentUserService;

    public PortalController(TradingService tradingService, CurrentUserService currentUserService) {
        this.tradingService = tradingService;
        this.currentUserService = currentUserService;
    }

    /**
     * 【職責】回傳目前登入使用者的帳戶摘要。
     * 【技巧】由 CurrentUserService 取得可信 uid，再交給 TradingService 查詢。
     * 【概念】端點不接受任意 userId，可避免使用者讀取他人帳戶。
     */
    @GetMapping("/accounts/me")
    public AccountResponse account() {
        return tradingService.account(currentUserService.requireUserId());
    }

    /**
     * 【職責】回傳目前登入使用者的持倉清單。
     * 【技巧】使用 SecurityContext 解析的 uid 限定查詢範圍。
     * 【概念】持倉是帳本讀模型，應以登入身分做資料隔離。
     */
    @GetMapping("/positions")
    public List<PositionResponse> positions() {
        return tradingService.positions(currentUserService.requireUserId());
    }

    /**
     * 【職責】分頁取得管理者可查閱的審計紀錄。
     * 【技巧】page 與 size 只負責 HTTP 綁定，範圍限制由 Service 執行。
     * 【概念】審計資料通常量大，必須使用伺服器端分頁。
     */
    @GetMapping("/audit-logs")
    public PageResponse<AuditLogResponse> auditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return tradingService.auditLogs(page, size);
    }

    /**
     * 【職責】提供前端下單可選的展示商品清單。
     * 【技巧】端點只委派 TradingService，不把商品規則寫在 Controller。
     * 【概念】讀取型 Portal API 保持薄層可讓傳輸協定更容易替換。
     */
    @GetMapping("/market/symbols")
    public List<Map<String, Object>> symbols() {
        return tradingService.symbols();
    }
}

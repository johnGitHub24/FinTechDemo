package com.fintech.demo.order.api;

import com.fintech.demo.common.dto.PageResponse;
import com.fintech.demo.order.application.TradingService;
import com.fintech.demo.order.domain.OrderStatus;
import com.fintech.demo.order.dto.CreateOrderRequest;
import com.fintech.demo.order.dto.OrderResponse;
import com.fintech.demo.order.security.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 【職責】交易 API；身分來自 JWT（CurrentUserService）。
 * 【技巧】只做參數／驗證／HTTP 狀態；商業規則在 Service。
 * 【概念】薄 Controller 利於測試與替換傳輸層。
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final TradingService tradingService;
    private final CurrentUserService currentUserService;

    public OrderController(TradingService tradingService, CurrentUserService currentUserService) {
        this.tradingService = tradingService;
        this.currentUserService = currentUserService;
    }

    /** 【職責】下單。【技巧】{@code @Valid} 觸發 Bean Validation。【概念】201 Created 表示資源已建立。 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
        return tradingService.create(currentUserService.requireUserId(), request);
    }

    /** 【職責】成交（會 Feign 呼叫 risk-service :8082）。【概念】這是跨服務編排的展示點。 */
    @PostMapping("/{id}/execute")
    public OrderResponse execute(@PathVariable Long id) {
        return tradingService.execute(currentUserService.requireUserId(), id);
    }

    /** 【職責】查單筆訂單（僅本人）。 */
    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id) {
        return tradingService.get(currentUserService.requireUserId(), id);
    }

    /** 【職責】訂單分頁；ADMIN 看全部。【技巧】status／page／size 皆為 query 參數。 */
    @GetMapping
    public PageResponse<OrderResponse> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return tradingService.list(
                currentUserService.requireUserId(),
                currentUserService.isAdmin(),
                status,
                page,
                size);
    }

    /** 【職責】取消 PENDING 訂單。 */
    @DeleteMapping("/{id}")
    public OrderResponse cancel(@PathVariable Long id) {
        return tradingService.cancel(currentUserService.requireUserId(), id);
    }
}

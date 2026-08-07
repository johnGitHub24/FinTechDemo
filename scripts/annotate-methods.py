# -*- coding: utf-8 -*-
"""Add method-level detail JavaDocs for core FinTechDemo classes."""
from pathlib import Path

ROOT = Path(r"D:\ClaudeCode\FinTechDemo")


def patch(rel: str, old: str, new: str) -> None:
    path = ROOT / rel
    text = path.read_text(encoding="utf-8")
    if old not in text:
        if new.strip() in text:
            print("SKIP already", rel, old[:40].replace("\n", " "))
            return
        raise SystemExit(f"NOT FOUND in {rel}:\n{old[:120]}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8", newline="\n")
    print("OK", rel)


# --- Audited annotation ---
patch(
    "order-service/src/main/java/com/fintech/demo/order/aop/Audited.java",
    Path(ROOT / "order-service/src/main/java/com/fintech/demo/order/aop/Audited.java").read_text(encoding="utf-8"),
    '''package com.fintech.demo.order.aop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 【職責】標記需要寫入審計日誌的業務方法。
 * 【技巧】由 {@link TradingAuditAspect} 攔截；{@code action} 寫入 audit 表。
 * 【概念】AOP 把「記一筆誰做了什麼」橫切出去，避免每個 Service 手動重複。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Audited {
    /** 審計動作代碼，例如 ORDER_CREATED／ORDER_EXECUTE。 */
    String action();
}
''',
)

# --- TradingService method docs ---
TS = "order-service/src/main/java/com/fintech/demo/order/application/TradingService.java"

patch(
    TS,
    """    @Audited(action = "ORDER_CREATED")
    public OrderResponse create(Long userId, CreateOrderRequest request) {
        requireUser(userId);
        if (orderRepository.existsByClientOrderId(request.getClientOrderId())) {
            throw new BusinessException("clientOrderId already exists");
        }
        OrderEntity entity = new OrderEntity();
        entity.setUserId(userId);
        entity.setClientOrderId(request.getClientOrderId());
        entity.setSymbol(request.getSymbol().toUpperCase());
""",
    """    /**
     * 【職責】建立 PENDING 訂單（冪等鍵 clientOrderId）。
     * 【技巧】symbol 統一 toUpperCase；成功後可選發 Kafka order-events。
     * 【概念】建單 ≠ 成交：先落單再 execute，方便風控與取消。
     * @param userId 登入使用者
     * @param request 下單內容
     * @return 新建訂單
     */
    @Audited(action = "ORDER_CREATED")
    public OrderResponse create(Long userId, CreateOrderRequest request) {
        requireUser(userId);
        // 細節：同一 clientOrderId 不可重複，防止前端重送造成雙單
        if (orderRepository.existsByClientOrderId(request.getClientOrderId())) {
            throw new BusinessException("clientOrderId already exists");
        }
        OrderEntity entity = new OrderEntity();
        entity.setUserId(userId);
        entity.setClientOrderId(request.getClientOrderId());
        entity.setSymbol(request.getSymbol().toUpperCase());
""",
)

patch(
    TS,
    """    @Audited(action = "ORDER_EXECUTE")
    public OrderResponse execute(Long userId, Long orderId) {
        OrderEntity order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new NotFoundException("order not found"));
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("only PENDING can execute");
        }
        AccountEntity account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("account not found"));

        RiskCheckRequest riskReq = new RiskCheckRequest();
        riskReq.setUserId(userId);
        riskReq.setSymbol(order.getSymbol());
        riskReq.setSide(order.getSide().name());
        riskReq.setQuantity(order.getQuantity());
        riskReq.setPrice(order.getPrice());
        riskReq.setCashBalance(account.getCashBalance());
        RiskCheckResponse risk = riskClient.check(riskReq);
        if (!risk.isAllowed()) {
            order.setStatus(OrderStatus.REJECTED);
            orderRepository.save(order);
            writeAudit(usernameOf(userId), "ORDER_REJECTED", "orders/" + orderId, risk.getReason());
            return toOrderResponse(order);
        }

        BigDecimal notional = order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity()));
        if (order.getSide() == OrderSide.BUY) {
            account.setCashBalance(account.getCashBalance().subtract(notional));
            accountRepository.save(account);
            upsertBuyPosition(userId, order);
        } else {
            PositionEntity pos = positionRepository.findByUserIdAndSymbol(userId, order.getSymbol())
                    .orElseThrow(() -> new BusinessException("no position to sell"));
            if (pos.getQuantity() < order.getQuantity()) {
                order.setStatus(OrderStatus.REJECTED);
                orderRepository.save(order);
                writeAudit(usernameOf(userId), "ORDER_REJECTED", "orders/" + orderId, "insufficient position");
                return toOrderResponse(order);
            }
            pos.setQuantity(pos.getQuantity() - order.getQuantity());
            positionRepository.save(pos);
            account.setCashBalance(account.getCashBalance().add(notional));
            accountRepository.save(account);
        }
        order.setStatus(OrderStatus.ACCEPTED);
        orderRepository.save(order);
        writeAudit(usernameOf(userId), "ORDER_ACCEPTED", "orders/" + orderId, "executed " + notional);

        TradeExecutedEvent trade = new TradeExecutedEvent(
                order.getId(),
                userId,
                order.getSymbol(),
                order.getSide().name(),
                order.getQuantity(),
                order.getPrice(),
                notional);
        TradeEventPublisher tradePublisher = tradeEventPublisher.getIfAvailable();
        if (tradePublisher != null) {
            tradePublisher.publishAfterCommit(trade);
        }
        accountSyncService.syncTrade(trade);
        return toOrderResponse(order);
    }
""",
    """    /**
     * 【職責】成交：Feign 風控 → 調帳／持倉 → ACCEPTED，並同步／發佈成交事件。
     * 【技巧】riskClient 固定打 :8082；Kafka publisher 以 ObjectProvider 可選。
     * 【概念】這是分散式 Demo 的關鍵路徑：order 編排、risk 決策、account 入帳可拆服務。
     * @param userId 擁有者（禁止跨使用者成交）
     * @param orderId 訂單主鍵
     */
    @Audited(action = "ORDER_EXECUTE")
    public OrderResponse execute(Long userId, Long orderId) {
        OrderEntity order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new NotFoundException("order not found"));
        // 細節：狀態機 guard — 只有 PENDING 可成交，避免重送變成二次扣款
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("only PENDING can execute");
        }
        AccountEntity account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("account not found"));

        // 細節：把現金餘額一併送給 risk-service，由對方判斷名義金額是否可接受
        RiskCheckRequest riskReq = new RiskCheckRequest();
        riskReq.setUserId(userId);
        riskReq.setSymbol(order.getSymbol());
        riskReq.setSide(order.getSide().name());
        riskReq.setQuantity(order.getQuantity());
        riskReq.setPrice(order.getPrice());
        riskReq.setCashBalance(account.getCashBalance());
        RiskCheckResponse risk = riskClient.check(riskReq);
        if (!risk.isAllowed()) {
            // 細節：風控拒絕仍落 REJECTED + 審計，方便前端／Demo 展示失敗路徑
            order.setStatus(OrderStatus.REJECTED);
            orderRepository.save(order);
            writeAudit(usernameOf(userId), "ORDER_REJECTED", "orders/" + orderId, risk.getReason());
            return toOrderResponse(order);
        }

        // 細節：名義金額 = 價格 × 數量；BUY 扣現金、SELL 加現金
        BigDecimal notional = order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity()));
        if (order.getSide() == OrderSide.BUY) {
            account.setCashBalance(account.getCashBalance().subtract(notional));
            accountRepository.save(account);
            upsertBuyPosition(userId, order);
        } else {
            PositionEntity pos = positionRepository.findByUserIdAndSymbol(userId, order.getSymbol())
                    .orElseThrow(() -> new BusinessException("no position to sell"));
            if (pos.getQuantity() < order.getQuantity()) {
                order.setStatus(OrderStatus.REJECTED);
                orderRepository.save(order);
                writeAudit(usernameOf(userId), "ORDER_REJECTED", "orders/" + orderId, "insufficient position");
                return toOrderResponse(order);
            }
            pos.setQuantity(pos.getQuantity() - order.getQuantity());
            positionRepository.save(pos);
            account.setCashBalance(account.getCashBalance().add(notional));
            accountRepository.save(account);
        }
        order.setStatus(OrderStatus.ACCEPTED);
        orderRepository.save(order);
        writeAudit(usernameOf(userId), "ORDER_ACCEPTED", "orders/" + orderId, "executed " + notional);

        TradeExecutedEvent trade = new TradeExecutedEvent(
                order.getId(),
                userId,
                order.getSymbol(),
                order.getSide().name(),
                order.getQuantity(),
                order.getPrice(),
                notional);
        // 細節：有啟用 Kafka 才發 trade-events；否則靠 Feign AccountSync 同步帳本服務
        TradeEventPublisher tradePublisher = tradeEventPublisher.getIfAvailable();
        if (tradePublisher != null) {
            tradePublisher.publishAfterCommit(trade);
        }
        accountSyncService.syncTrade(trade);
        return toOrderResponse(order);
    }
""",
)

patch(
    TS,
    """    public OrderResponse cancel(Long userId, Long orderId) {
""",
    """    /**
     * 【職責】取消尚未成交的 PENDING 訂單。
     * 【技巧】同樣用狀態 guard，避免取消已 ACCEPTED 的單。
     * 【概念】取消是使用者主動路徑；逾時取消則由 job-service 觸發。
     */
    public OrderResponse cancel(Long userId, Long orderId) {
""",
)

patch(
    TS,
    """    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> list(Long userId, boolean adminAll, OrderStatus status, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
""",
    """    /**
     * 【職責】訂單分頁列表；ADMIN 可看全部，USER 只看自己。
     * 【技巧】page／size 做邊界夾制，防止一次拉爆記憶體。
     * 【概念】伺服器端分頁是 Portal 大表的基本功。
     */
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> list(Long userId, boolean adminAll, OrderStatus status, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
""",
)

patch(
    TS,
    """    private void upsertBuyPosition(Long userId, OrderEntity order) {
        PositionEntity pos = positionRepository.findByUserIdAndSymbol(userId, order.getSymbol())
                .orElseGet(() -> {
                    PositionEntity p = new PositionEntity();
                    p.setUserId(userId);
                    p.setSymbol(order.getSymbol());
                    p.setQuantity(0);
                    p.setAvgPrice(BigDecimal.ZERO);
                    return p;
                });
        int newQty = pos.getQuantity() + order.getQuantity();
        BigDecimal oldCost = pos.getAvgPrice().multiply(BigDecimal.valueOf(pos.getQuantity()));
        BigDecimal addCost = order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity()));
        pos.setQuantity(newQty);
        pos.setAvgPrice(oldCost.add(addCost).divide(BigDecimal.valueOf(newQty), 4, RoundingMode.HALF_UP));
        positionRepository.save(pos);
    }
""",
    """    /**
     * 【職責】買入後更新（或新建）持倉與加權平均成本。
     * 【技巧】avg = (舊成本 + 新成本) / 新數量，HALF_UP 到 4 位。
     * 【概念】均價是後續賣出損益估算的基礎（本 Demo 未做完整 PnL）。
     */
    private void upsertBuyPosition(Long userId, OrderEntity order) {
        PositionEntity pos = positionRepository.findByUserIdAndSymbol(userId, order.getSymbol())
                .orElseGet(() -> {
                    PositionEntity p = new PositionEntity();
                    p.setUserId(userId);
                    p.setSymbol(order.getSymbol());
                    p.setQuantity(0);
                    p.setAvgPrice(BigDecimal.ZERO);
                    return p;
                });
        int newQty = pos.getQuantity() + order.getQuantity();
        // 細節：舊持倉成本 + 本次成交成本，再除以新數量
        BigDecimal oldCost = pos.getAvgPrice().multiply(BigDecimal.valueOf(pos.getQuantity()));
        BigDecimal addCost = order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity()));
        pos.setQuantity(newQty);
        pos.setAvgPrice(oldCost.add(addCost).divide(BigDecimal.valueOf(newQty), 4, RoundingMode.HALF_UP));
        positionRepository.save(pos);
    }
""",
)

# --- RiskService ---
patch(
    "risk-service/src/main/java/com/fintech/demo/risk/application/RiskService.java",
    """    public RiskCheckResponse check(RiskCheckRequest request) {
        if (request.getQuantity() == null || request.getQuantity() <= 0
                || request.getPrice() == null || request.getPrice().signum() <= 0) {
            return RiskCheckResponse.reject("invalid quantity or price");
        }
        BigDecimal notional = request.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
        if (notional.compareTo(maxNotional) > 0) {
            return RiskCheckResponse.reject("notional exceeds max " + maxNotional);
        }
        if ("BUY".equalsIgnoreCase(request.getSide())) {
            if (request.getCashBalance() == null || request.getCashBalance().compareTo(notional) < 0) {
                return RiskCheckResponse.reject("insufficient cash for notional " + notional);
            }
        }
        return RiskCheckResponse.ok();
    }
""",
    """    /**
     * 【職責】檢查單筆名義金額是否通過風控。
     * 【技巧】規則：數量／價格合法 → notional ≤ maxNotional → BUY 時 notional ≤ 現金。
     * 【概念】風控獨立進程後，order 只能「問可不可以」，不能自己偷偷改規則。
     * @param request 含 side／qty／price／cashBalance
     * @return allowed + reason
     */
    public RiskCheckResponse check(RiskCheckRequest request) {
        // 細節：先擋非法輸入，避免後續 BigDecimal 運算無意義
        if (request.getQuantity() == null || request.getQuantity() <= 0
                || request.getPrice() == null || request.getPrice().signum() <= 0) {
            return RiskCheckResponse.reject("invalid quantity or price");
        }
        BigDecimal notional = request.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
        // 細節：單筆上限（預設 50000）防止 Demo 一次打爆帳本
        if (notional.compareTo(maxNotional) > 0) {
            return RiskCheckResponse.reject("notional exceeds max " + maxNotional);
        }
        // 細節：賣出不檢查現金；買入必須現金足夠覆蓋名義金額
        if ("BUY".equalsIgnoreCase(request.getSide())) {
            if (request.getCashBalance() == null || request.getCashBalance().compareTo(notional) < 0) {
                return RiskCheckResponse.reject("insufficient cash for notional " + notional);
            }
        }
        return RiskCheckResponse.ok();
    }
""",
)

# --- OrderController method docs ---
OC = "order-service/src/main/java/com/fintech/demo/order/api/OrderController.java"
text = (ROOT / OC).read_text(encoding="utf-8")
# ensure class triple if script left short
if "【技巧】" not in text:
    text = text.replace(
        """/**
 * 【職責】交易 API；身分來自 JWT（CurrentUserService）。
 */
""",
        """/**
 * 【職責】交易 API；身分來自 JWT（CurrentUserService）。
 * 【技巧】Controller 只轉交 TradingService，不寫扣款／風控。
 * 【概念】薄 Controller + 厚 Service，是 Demo 常講的分層。
 */
""",
    )
replacements = [
    (
        """    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
""",
        """    /** 【職責】下單。【技巧】{@code @Valid} 觸發 Bean Validation。【概念】201 Created 表示資源已建立。 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
""",
    ),
    (
        """    @PostMapping("/{id}/execute")
    public OrderResponse execute(@PathVariable Long id) {
""",
        """    /** 【職責】成交（會 Feign 呼叫 risk-service :8082）。【概念】這是跨服務編排的展示點。 */
    @PostMapping("/{id}/execute")
    public OrderResponse execute(@PathVariable Long id) {
""",
    ),
    (
        """    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id) {
""",
        """    /** 【職責】查單筆訂單（僅本人）。 */
    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id) {
""",
    ),
    (
        """    @GetMapping
    public PageResponse<OrderResponse> list(
""",
        """    /** 【職責】訂單分頁；ADMIN 看全部。【技巧】status／page／size 皆為 query 參數。 */
    @GetMapping
    public PageResponse<OrderResponse> list(
""",
    ),
    (
        """    @DeleteMapping("/{id}")
    public OrderResponse cancel(@PathVariable Long id) {
""",
        """    /** 【職責】取消 PENDING 訂單。 */
    @DeleteMapping("/{id}")
    public OrderResponse cancel(@PathVariable Long id) {
""",
    ),
]
for old, new in replacements:
    if old not in text:
        raise SystemExit("OrderController missing block: " + old[:60])
    text = text.replace(old, new, 1)
(ROOT / OC).write_text(text, encoding="utf-8", newline="\n")
print("OK OrderController methods")

print("DONE")

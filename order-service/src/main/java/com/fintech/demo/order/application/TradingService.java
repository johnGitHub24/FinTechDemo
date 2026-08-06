package com.fintech.demo.order.application;

import com.fintech.demo.common.dto.PageResponse;
import com.fintech.demo.common.dto.RiskCheckRequest;
import com.fintech.demo.common.dto.RiskCheckResponse;
import com.fintech.demo.order.aop.Audited;
import com.fintech.demo.order.client.AccountSyncService;
import com.fintech.demo.order.client.RiskClient;
import com.fintech.demo.order.common.BusinessException;
import com.fintech.demo.order.common.NotFoundException;
import com.fintech.demo.order.demo.DemoGatewayHintFilter;
import com.fintech.demo.order.demo.DemoTraceFactory;
import com.fintech.demo.order.domain.OrderSide;
import com.fintech.demo.order.domain.OrderStatus;
import com.fintech.demo.order.dto.AccountResponse;
import com.fintech.demo.order.dto.AuditLogResponse;
import com.fintech.demo.order.dto.CreateOrderRequest;
import com.fintech.demo.order.dto.DemoTrace;
import com.fintech.demo.order.dto.OrderResponse;
import com.fintech.demo.order.dto.PositionResponse;
import com.fintech.demo.order.infrastructure.AccountEntity;
import com.fintech.demo.order.infrastructure.AccountRepository;
import com.fintech.demo.order.infrastructure.AuditLogEntity;
import com.fintech.demo.order.infrastructure.AuditLogRepository;
import com.fintech.demo.order.infrastructure.OrderEntity;
import com.fintech.demo.order.infrastructure.OrderRepository;
import com.fintech.demo.order.infrastructure.PositionEntity;
import com.fintech.demo.order.infrastructure.PositionRepository;
import com.fintech.demo.order.infrastructure.UserEntity;
import com.fintech.demo.order.infrastructure.UserRepository;
import com.fintech.demo.order.kafka.OrderEventPublisher;
import com.fintech.demo.order.kafka.TradeEventPublisher;
import com.fintech.demo.common.event.TradeExecutedEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * 【職責】訂單交易核心：建單、Feign 風控、成交、取消、帳本與審計。
 * 【技巧】execute 以固定 URL Feign 呼叫 risk-service；Kafka publisher 用 ObjectProvider 可選啟用。
 * 【概念】微服務拆分後，成交路徑必須真實連到 :8082，才符合分散式設計敘事。
 */
@Service
@Transactional
public class TradingService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final OrderRepository orderRepository;
    private final PositionRepository positionRepository;
    private final AuditLogRepository auditLogRepository;
    private final RiskClient riskClient;
    private final AccountSyncService accountSyncService;
    private final ObjectProvider<OrderEventPublisher> orderEventPublisher;
    private final ObjectProvider<TradeEventPublisher> tradeEventPublisher;
    private final Counter ordersCreatedCounter;

    public TradingService(
            UserRepository userRepository,
            AccountRepository accountRepository,
            OrderRepository orderRepository,
            PositionRepository positionRepository,
            AuditLogRepository auditLogRepository,
            RiskClient riskClient,
            AccountSyncService accountSyncService,
            ObjectProvider<OrderEventPublisher> orderEventPublisher,
            ObjectProvider<TradeEventPublisher> tradeEventPublisher,
            MeterRegistry meterRegistry) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.orderRepository = orderRepository;
        this.positionRepository = positionRepository;
        this.auditLogRepository = auditLogRepository;
        this.riskClient = riskClient;
        this.accountSyncService = accountSyncService;
        this.orderEventPublisher = orderEventPublisher;
        this.tradeEventPublisher = tradeEventPublisher;
        this.ordersCreatedCounter = Counter.builder("fintech.orders.created")
                .description("Number of orders created")
                .register(meterRegistry);
    }

    /**
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
        entity.setSide(request.getSide());
        entity.setQuantity(request.getQuantity());
        entity.setPrice(request.getPrice());
        entity.setStatus(OrderStatus.PENDING);
        OrderEntity saved = orderRepository.save(entity);
        ordersCreatedCounter.increment();
        writeAudit(usernameOf(userId), "ORDER_CREATED", "orders/" + saved.getId(),
                saved.getSide() + " " + saved.getSymbol());
        OrderEventPublisher publisher = orderEventPublisher.getIfAvailable();
        if (publisher != null) {
            publisher.publishAfterCommit(saved.getId(), userId);
        }
        return withTrace(toOrderResponse(saved), DemoTraceFactory.forCreate(
                viaGateway(), saved.getId(), saved.getStatus().name()));
    }

    /**
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
        if (!risk.allowed()) {
            // 細節：風控拒絕仍落 REJECTED + 審計，方便前端／Demo 展示失敗路徑
            order.setStatus(OrderStatus.REJECTED);
            orderRepository.save(order);
            writeAudit(usernameOf(userId), "ORDER_REJECTED", "orders/" + orderId, risk.reason());
            return withTrace(toOrderResponse(order), DemoTraceFactory.forExecute(
                    viaGateway(), order.getId(), order.getStatus().name(), false,
                    risk.reason() != null ? risk.reason() : "risk rejected"));
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
                return withTrace(toOrderResponse(order), DemoTraceFactory.forExecute(
                        viaGateway(), order.getId(), order.getStatus().name(), true,
                        "risk ok; rejected: insufficient position"));
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
        return withTrace(toOrderResponse(order), DemoTraceFactory.forExecute(
                viaGateway(), order.getId(), order.getStatus().name(), true, "notional within limit"));
    }

    /**
     * 【職責】取消尚未成交的 PENDING 訂單。
     * 【技巧】同樣用狀態 guard，避免取消已 ACCEPTED 的單。
     * 【概念】取消是使用者主動路徑；逾時取消則由 job-service 觸發。
     */
    public OrderResponse cancel(Long userId, Long orderId) {
        OrderEntity order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new NotFoundException("order not found"));
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("only PENDING can cancel");
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        writeAudit(usernameOf(userId), "ORDER_CANCELLED", "orders/" + orderId, "cancelled by user");
        return withTrace(toOrderResponse(order), DemoTraceFactory.forCancel(
                viaGateway(), order.getId(), order.getStatus().name()));
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long userId, Long orderId) {
        return toOrderResponse(orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new NotFoundException("order not found")));
    }

    /**
     * 【職責】訂單分頁列表；ADMIN 可看全部，USER 只看自己。
     * 【技巧】page／size 做邊界夾制，防止一次拉爆記憶體。
     * 【概念】伺服器端分頁是 Portal 大表的基本功。
     */
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> list(Long userId, boolean adminAll, OrderStatus status, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pr = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OrderEntity> result = adminAll
                ? orderRepository.findByOptionalStatus(status, pr)
                : orderRepository.findByUserIdAndOptionalStatus(userId, status, pr);
        List<OrderResponse> data = result.getContent().stream().map(this::toOrderResponse).toList();
        return PageResponse.of(data, safePage, safeSize, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public AccountResponse account(Long userId) {
        AccountEntity a = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("account not found"));
        return new AccountResponse(a.getUserId(), a.getCashBalance(), a.getCurrency());
    }

    @Transactional(readOnly = true)
    public List<PositionResponse> positions(Long userId) {
        return positionRepository.findByUserId(userId).stream()
                .map(p -> new PositionResponse(p.getSymbol(), p.getQuantity(), p.getAvgPrice()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> auditLogs(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<AuditLogEntity> result = auditLogRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(safePage, safeSize));
        List<AuditLogResponse> data = result.getContent().stream().map(this::toAudit).toList();
        return PageResponse.of(data, safePage, safeSize, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> symbols() {
        return List.of(
                Map.of("symbol", "AAPL", "refPrice", new BigDecimal("150.00")),
                Map.of("symbol", "TSLA", "refPrice", new BigDecimal("200.00")),
                Map.of("symbol", "MSFT", "refPrice", new BigDecimal("300.00")));
    }

    /**
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

    private void writeAudit(String username, String action, String resource, String detail) {
        AuditLogEntity log = new AuditLogEntity();
        log.setUsername(username);
        log.setAction(action);
        log.setResource(resource);
        log.setDetail(detail);
        auditLogRepository.save(log);
    }

    private UserEntity requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("user not found"));
    }

    private String usernameOf(Long userId) {
        return requireUser(userId).getUsername();
    }

    private OrderResponse toOrderResponse(OrderEntity e) {
        OrderResponse r = new OrderResponse();
        r.setId(e.getId());
        r.setUserId(e.getUserId());
        r.setClientOrderId(e.getClientOrderId());
        r.setSymbol(e.getSymbol());
        r.setSide(e.getSide());
        r.setQuantity(e.getQuantity());
        r.setPrice(e.getPrice());
        r.setStatus(e.getStatus());
        r.setCreatedAt(e.getCreatedAt());
        return r;
    }

    private OrderResponse withTrace(OrderResponse response, DemoTrace trace) {
        response.setDemoTrace(trace);
        return response;
    }

    /**
     * 【職責】判斷本次請求是否經 Gateway 轉發。
     * 【技巧】讀 {@link DemoGatewayHintFilter} 寫入的 request attribute；無 HTTP 上下文（單元測）則 false。
     * 【概念】viaGateway 只影響 demoTrace 展示，不改業務結果。
     */
    private boolean viaGateway() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            Object v = servletAttrs.getRequest().getAttribute(DemoGatewayHintFilter.ATTR);
            return Boolean.TRUE.equals(v);
        }
        return false;
    }

    private AuditLogResponse toAudit(AuditLogEntity e) {
        AuditLogResponse r = new AuditLogResponse();
        r.setId(e.getId());
        r.setAction(e.getAction());
        r.setResource(e.getResource());
        r.setDetail(e.getDetail());
        r.setUsername(e.getUsername());
        r.setCreatedAt(e.getCreatedAt());
        return r;
    }
}

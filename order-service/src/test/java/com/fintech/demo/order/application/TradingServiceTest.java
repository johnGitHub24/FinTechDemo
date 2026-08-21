package com.fintech.demo.order.application;

import com.fintech.demo.common.dto.RiskCheckResponse;
import com.fintech.demo.order.client.AccountSyncService;
import com.fintech.demo.order.client.RiskClient;
import com.fintech.demo.order.common.BusinessException;
import com.fintech.demo.order.domain.OrderSide;
import com.fintech.demo.order.domain.OrderStatus;
import com.fintech.demo.order.domain.Role;
import com.fintech.demo.order.dto.CreateOrderRequest;
import com.fintech.demo.order.dto.OrderResponse;
import com.fintech.demo.common.dto.PageResponse;
import com.fintech.demo.order.dto.AccountResponse;
import com.fintech.demo.order.dto.AuditLogResponse;
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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 【職責】覆蓋 TradingService 建單、冪等與風控成交。
 * 【技巧】Mockito 隔離 RiskClient／Repository／Kafka publisher。
 * 【概念】訂單核心單元測試：happy path 與 reject 路徑都在這。
 */
@ExtendWith(MockitoExtension.class)
class TradingServiceTest {

    @Mock UserRepository userRepository;
    @Mock AccountRepository accountRepository;
    @Mock OrderRepository orderRepository;
    @Mock PositionRepository positionRepository;
    @Mock AuditLogRepository auditLogRepository;
    @Mock RiskClient riskClient;
    @Mock AccountSyncService accountSyncService;
    @Mock ObjectProvider<OrderEventPublisher> orderEventPublisher;
    @Mock ObjectProvider<TradeEventPublisher> tradeEventPublisher;

    TradingService tradingService;

    private UserEntity trader;

    @BeforeEach
    void setUp() {
        trader = new UserEntity();
        trader.setId(1L);
        trader.setUsername("trader1");
        trader.setRole(Role.USER);
        trader.setPasswordHash("password");
        tradingService = new TradingService(
                userRepository,
                accountRepository,
                orderRepository,
                positionRepository,
                auditLogRepository,
                riskClient,
                accountSyncService,
                orderEventPublisher,
                tradeEventPublisher,
                new SimpleMeterRegistry());
    }

    /**
     * CASE ORDER-001：Given 合法下單 When create Then 回 PENDING 且 symbol 大寫。
     */
    @Test
    void ORDER_001_create_shouldPersistPendingOrder() {
        when(orderEventPublisher.getIfAvailable()).thenReturn(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(trader));
        when(orderRepository.existsByClientOrderId("C1")).thenReturn(false);
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> {
            OrderEntity e = inv.getArgument(0);
            e.setId(10L);
            return e;
        });

        CreateOrderRequest req = new CreateOrderRequest();
        req.setClientOrderId("C1");
        req.setSymbol("aapl");
        req.setSide(OrderSide.BUY);
        req.setQuantity(2);
        req.setPrice(new BigDecimal("10.00"));

        OrderResponse resp = tradingService.create(1L, req);
        assertThat(resp.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(resp.getSymbol()).isEqualTo("AAPL");
        assertThat(resp.getUsername()).isEqualTo("trader1");
        verify(auditLogRepository).save(any());
    }

    /**
     * CASE ORDER-002：Given 重複 clientOrderId When create Then 拋 BusinessException。
     */
    @Test
    void ORDER_002_duplicateClientOrderId_shouldFail() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(trader));
        when(orderRepository.existsByClientOrderId("DUP")).thenReturn(true);

        CreateOrderRequest req = new CreateOrderRequest();
        req.setClientOrderId("DUP");
        req.setSymbol("AAPL");
        req.setSide(OrderSide.BUY);
        req.setQuantity(1);
        req.setPrice(new BigDecimal("1.00"));

        assertThatThrownBy(() -> tradingService.create(1L, req))
                .isInstanceOf(BusinessException.class);
    }

    /**
     * CASE ORDER-005：Given 風控通過 When execute Then ACCEPTED 且扣現金。
     * CASE FLOW-005：成交後餘額與持倉由同一 execute 路徑更新。
     */
    @Test
    void ORDER_005_FLOW_005_execute_whenRiskAllows_shouldAcceptAndDeductCash() {
        when(tradeEventPublisher.getIfAvailable()).thenReturn(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(trader));
        OrderEntity pending = new OrderEntity();
        pending.setId(5L);
        pending.setUserId(1L);
        pending.setSymbol("AAPL");
        pending.setSide(OrderSide.BUY);
        pending.setQuantity(10);
        pending.setPrice(new BigDecimal("100.00"));
        pending.setStatus(OrderStatus.PENDING);
        when(orderRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(pending));

        AccountEntity account = new AccountEntity();
        account.setUserId(1L);
        account.setCashBalance(new BigDecimal("5000.00"));
        when(accountRepository.findByUserId(1L)).thenReturn(Optional.of(account));
        when(riskClient.check(any())).thenReturn(RiskCheckResponse.ok());
        when(positionRepository.findByUserIdAndSymbol(1L, "AAPL")).thenReturn(Optional.empty());
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(positionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse resp = tradingService.execute(1L, 5L);
        assertThat(resp.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(account.getCashBalance()).isEqualByComparingTo("4000.00");
        assertThat(resp.getDemoTrace()).isNotNull();
        assertThat(resp.getDemoTrace().action()).isEqualTo("EXECUTE");
        assertThat(resp.getDemoTrace().hops()).anyMatch(h -> "risk-service".equals(h.service()) && h.ok());
    }

    /**
     * CASE ORDER-006：Given 風控拒絕 When execute Then REJECTED 且現金不變。
     */
    @Test
    void ORDER_006_execute_whenRiskRejects_shouldMarkRejected() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(trader));
        OrderEntity pending = new OrderEntity();
        pending.setId(5L);
        pending.setUserId(1L);
        pending.setSymbol("AAPL");
        pending.setSide(OrderSide.BUY);
        pending.setQuantity(10);
        pending.setPrice(new BigDecimal("100.00"));
        pending.setStatus(OrderStatus.PENDING);
        when(orderRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(pending));

        AccountEntity account = new AccountEntity();
        account.setUserId(1L);
        account.setCashBalance(new BigDecimal("5000.00"));
        when(accountRepository.findByUserId(1L)).thenReturn(Optional.of(account));
        when(riskClient.check(any())).thenReturn(RiskCheckResponse.reject("notional exceeds max"));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse resp = tradingService.execute(1L, 5L);
        assertThat(resp.getStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(account.getCashBalance()).isEqualByComparingTo("5000.00");
        assertThat(resp.getDemoTrace()).isNotNull();
        assertThat(resp.getDemoTrace().hops()).anyMatch(h -> "risk-service".equals(h.service()) && !h.ok());
    }

    /**
     * CASE ORDER-007：Given PENDING 訂單 When cancel Then CANCELLED。
     * CASE FLOW-006：取消後歷程狀態與整合層同一契約。
     */
    @Test
    void ORDER_007_FLOW_006_cancelPending_shouldMarkCancelled() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(trader));
        OrderEntity pending = new OrderEntity();
        pending.setId(7L);
        pending.setUserId(1L);
        pending.setClientOrderId("CXL");
        pending.setSymbol("TSLA");
        pending.setSide(OrderSide.BUY);
        pending.setQuantity(1);
        pending.setPrice(new BigDecimal("200.00"));
        pending.setStatus(OrderStatus.PENDING);
        when(orderRepository.findByIdAndUserId(7L, 1L)).thenReturn(Optional.of(pending));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse resp = tradingService.cancel(1L, 7L);
        assertThat(resp.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(auditLogRepository).save(any());
    }

    /**
     * CASE ORDER-008：Given 分頁查詢 When list Then meta.page 與 size 正確，且列上有 username。
     * CASE FLOW-007：ADMIN／USER 列表皆走同一 list 契約。
     */
    @Test
    void ORDER_008_FLOW_007_list_shouldReturnPagedMeta() {
        OrderEntity row = new OrderEntity();
        row.setId(1L);
        row.setUserId(1L);
        row.setClientOrderId("L1");
        row.setSymbol("AAPL");
        row.setSide(OrderSide.BUY);
        row.setQuantity(1);
        row.setPrice(new BigDecimal("10.00"));
        row.setStatus(OrderStatus.PENDING);
        when(userRepository.findById(1L)).thenReturn(Optional.of(trader));
        when(orderRepository.findByUserIdAndOptionalStatus(eq(1L), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 10), 1));

        PageResponse<OrderResponse> page = tradingService.list(1L, false, null, 0, 10);
        assertThat(page.meta().page()).isEqualTo(0);
        assertThat(page.meta().size()).isEqualTo(10);
        assertThat(page.data()).hasSize(1);
        assertThat(page.data().getFirst().getUsername()).isEqualTo("trader1");
    }

    /**
     * CASE FLOW-003：Given 審計紀錄 When auditLogs Then 分頁映射 DTO。
     */
    @Test
    void FLOW_003_auditLogs_shouldMapPaged() {
        AuditLogEntity log = new AuditLogEntity();
        log.setId(1L);
        log.setAction("ORDER_CREATED");
        log.setResource("orders/1");
        log.setDetail("ok");
        log.setUsername("trader1");
        when(auditLogRepository.findAllByOrderByCreatedAtDesc(any()))
                .thenReturn(new PageImpl<>(List.of(log), PageRequest.of(0, 10), 1));

        PageResponse<AuditLogResponse> page = tradingService.auditLogs(0, 10);
        assertThat(page.data()).hasSize(1);
        assertThat(page.data().getFirst().getAction()).isEqualTo("ORDER_CREATED");
        assertThat(page.meta().page()).isEqualTo(0);
    }

    /**
     * CASE FLOW-004：Given 帳戶與持倉 When 查詢入口資料 Then 回傳餘額／持倉／商品。
     */
    @Test
    void FLOW_004_portalQueries_shouldReturnAccountPositionSymbols() {
        AccountEntity account = new AccountEntity();
        account.setUserId(1L);
        account.setCashBalance(new BigDecimal("85000.00"));
        account.setCurrency("TWD");
        when(accountRepository.findByUserId(1L)).thenReturn(Optional.of(account));
        PositionEntity pos = new PositionEntity();
        pos.setUserId(1L);
        pos.setSymbol("AAPL");
        pos.setQuantity(100);
        pos.setAvgPrice(new BigDecimal("150.00"));
        when(positionRepository.findByUserId(1L)).thenReturn(List.of(pos));

        AccountResponse acc = tradingService.account(1L);
        List<PositionResponse> positions = tradingService.positions(1L);
        assertThat(acc.cashBalance()).isEqualByComparingTo("85000.00");
        assertThat(positions).hasSize(1);
        assertThat(positions.getFirst().symbol()).isEqualTo("AAPL");
        assertThat(tradingService.symbols()).isNotEmpty();
    }
}

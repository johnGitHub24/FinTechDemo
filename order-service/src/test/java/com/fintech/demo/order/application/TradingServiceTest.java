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
import com.fintech.demo.order.infrastructure.AccountEntity;
import com.fintech.demo.order.infrastructure.AccountRepository;
import com.fintech.demo.order.infrastructure.AuditLogRepository;
import com.fintech.demo.order.infrastructure.OrderEntity;
import com.fintech.demo.order.infrastructure.OrderRepository;
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

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
     * CASE-TRADING-001：Given 合法下單 When create Then 回 PENDING 且 symbol 大寫。
     */
    @Test
    void create_shouldPersistPendingOrder() {
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
        verify(auditLogRepository).save(any());
    }

    /**
     * CASE-TRADING-002：Given 重複 clientOrderId When create Then 拋 BusinessException。
     */
    @Test
    void create_duplicateClientOrderId_shouldFail() {
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
     * CASE-TRADING-003：Given 風控通過 When execute Then ACCEPTED 且扣現金。
     */
    @Test
    void execute_whenRiskAllows_shouldAcceptAndDeductCash() {
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
     * CASE-TRADING-004：Given 風控拒絕 When execute Then REJECTED 且現金不變。
     */
    @Test
    void execute_whenRiskRejects_shouldMarkRejected() {
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
}

package com.fintech.demo.order.application;

import com.fintech.demo.order.domain.OrderStatus;
import com.fintech.demo.order.infrastructure.OrderEntity;
import com.fintech.demo.order.infrastructure.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
/**
 * 【職責】驗證逾時待處理訂單的排程取消規則。
 * 【技巧】以 Mockito 提供不同建立時間的訂單，隔離資料庫與時間邊界以外的相依。
 * 【概念】逾時取消只應影響超過門檻且狀態仍為 PENDING 的訂單。
 */
class StaleOrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    private StaleOrderService staleOrderService;

    @BeforeEach
    void setUp() {
        staleOrderService = new StaleOrderService(orderRepository, 30);
    }

    /**
     * CASE-STALE-ORDER-001：Given 新舊兩筆待處理訂單，When 執行逾時取消，Then 僅取消舊訂單。
     */
    @Test
    void cancelStalePending_shouldCancelOldPendingOnly() {
        OrderEntity oldPending = new OrderEntity();
        oldPending.setId(1L);
        oldPending.setStatus(OrderStatus.PENDING);
        oldPending.setCreatedAt(Instant.now().minus(60, ChronoUnit.MINUTES));

        OrderEntity fresh = new OrderEntity();
        fresh.setId(2L);
        fresh.setStatus(OrderStatus.PENDING);
        fresh.setCreatedAt(Instant.now());

        when(orderRepository.findAll()).thenReturn(List.of(oldPending, fresh));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int n = staleOrderService.cancelStalePending();
        assertThat(n).isEqualTo(1);
        assertThat(oldPending.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(fresh.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orderRepository, times(1)).save(oldPending);
    }
}

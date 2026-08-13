package com.fintech.demo.order.api;

import com.fintech.demo.order.domain.OrderSide;
import com.fintech.demo.order.domain.OrderStatus;
import com.fintech.demo.order.infrastructure.OrderEntity;
import com.fintech.demo.order.infrastructure.OrderRepository;
import com.fintech.demo.order.support.OrderIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 【職責】內部逾時取消 Job API 整合測試，與 STALE-001 單元成對。
 * 【技巧】先寫入一筆逾時 PENDING，再帶 X-Job-Token 觸發。
 * 【概念】job-service 遠端觸發同一條內部契約。
 */
class InternalJobApiIntegrationTest extends OrderIntegrationTestBase {

    @Autowired
    private OrderRepository orderRepository;

    /**
     * CASE STALE-001：逾時 PENDING 被取消，新鮮訂單保留。
     */
    @Test
    void STALE_001_cancelStale_cancelsOldPendingOnly() throws Exception {
        OrderEntity oldPending = new OrderEntity();
        oldPending.setUserId(1L);
        oldPending.setClientOrderId("stale-" + System.nanoTime());
        oldPending.setSymbol("AAPL");
        oldPending.setSide(OrderSide.BUY);
        oldPending.setQuantity(1);
        oldPending.setPrice(new BigDecimal("10.00"));
        oldPending.setStatus(OrderStatus.PENDING);
        oldPending.setCreatedAt(Instant.now().minus(60, ChronoUnit.MINUTES));
        orderRepository.save(oldPending);

        mockMvc.perform(post("/api/internal/jobs/cancel-stale")
                        .header("X-Job-Token", "demo-job-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelled").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }
}

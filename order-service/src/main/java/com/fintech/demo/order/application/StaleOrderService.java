package com.fintech.demo.order.application;

import com.fintech.demo.order.domain.OrderStatus;
import com.fintech.demo.order.infrastructure.OrderEntity;
import com.fintech.demo.order.infrastructure.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 【職責】逾時 PENDING 訂單取消（Job 呼叫）。
 * 【技巧】讀多用 @Transactional(readOnly=true)；寫入走預設交易。
 * 【概念】Service 是 Demo 最常說明的「流程編排」層。
 */
@Service
public class StaleOrderService {

    private static final Logger log = LoggerFactory.getLogger(StaleOrderService.class);

    private final OrderRepository orderRepository;
    private final int staleMinutes;

    public StaleOrderService(
            OrderRepository orderRepository,
            @Value("${fintech.job.stale-order-minutes:30}") int staleMinutes) {
        this.orderRepository = orderRepository;
        this.staleMinutes = staleMinutes;
    }

    /**
     * 【職責】取消超過設定分鐘數仍為 PENDING 的訂單，並回傳取消數量。
     * 【技巧】以 Instant 閾值過濾後逐筆更新狀態，確保整批更新位於同一交易內。
     * 【概念】排程取消是逾時補償機制，避免未成交訂單永久停留在狀態機中。
     */
    @Transactional
    public int cancelStalePending() {
        Instant threshold = Instant.now().minus(staleMinutes, ChronoUnit.MINUTES);
        List<OrderEntity> stale = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING)
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isBefore(threshold))
                .toList();
        for (OrderEntity order : stale) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
        }
        if (!stale.isEmpty()) {
            log.info("Cancelled {} stale PENDING orders older than {} minutes", stale.size(), staleMinutes);
        }
        return stale.size();
    }
}

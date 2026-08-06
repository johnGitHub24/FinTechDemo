package com.fintech.demo.order.dto;

import java.time.Instant;
import java.util.List;

/**
 * 【職責】附加於 OrderResponse 的 Demo 過程追蹤。
 * 【技巧】record；hops 於工廠用 List.copyOf 組裝。
 * 【概念】舊客戶端可忽略此欄位；前端用來渲染 PROCESS FLOW。
 */
public record DemoTrace(
        String requestId,
        String action,
        boolean viaGateway,
        String inferredStage,
        Long orderId,
        String orderStatus,
        List<DemoHop> hops,
        Instant at
) {
}

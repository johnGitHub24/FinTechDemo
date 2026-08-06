package com.fintech.demo.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 【職責】同步 Feign 入帳請求，欄位對齊 {@code TradeExecutedEvent}。
 * 【技巧】class + Lombok {@code @Data}（非 record）：無參／全參＋{@code setXxx} 供逐步組裝與 Jackson。
 * 【概念】為何不用 record？Request 組裝階段可變；成交後事件快照才用 record（TradeExecutedEvent）。
 *         規則：Properties／API 回應／Kafka 事件 → record；需 setXxx 的 Request → class + Lombok。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplyTradeRequest {

    private Long orderId;
    private Long userId;
    private String symbol;
    private String side;
    private int quantity;
    private BigDecimal price;
    private BigDecimal notional;
}

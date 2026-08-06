package com.fintech.demo.common.event;

import java.math.BigDecimal;

/**
 * 【職責】Kafka trade-events：成交後通知 account-service 入帳。
 * 【技巧】Java {@code record}；side 用 String（BUY/SELL）跨服務序列化，不綁 order enum。
 * 【概念】為何用 record？成交事件是帳本輸入依據，屬不可變「事件契約」——改寫欄位等於竄改帳務。
 *         對照：JPA Entity、需 {@code setXxx} 的 Request → class + Lombok。
 */
public record TradeExecutedEvent(
        Long orderId,
        Long userId,
        String symbol,
        /** BUY 或 SELL */
        String side,
        int quantity,
        BigDecimal price,
        BigDecimal notional) {
}

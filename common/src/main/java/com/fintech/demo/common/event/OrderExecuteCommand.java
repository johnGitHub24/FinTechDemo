package com.fintech.demo.common.event;

/**
 * 【職責】Kafka order-events：建立後非同步執行。
 * 【技巧】Java {@code record}：自動元件存取子／equals／hashCode；Jackson／Kafka 以元件名序列化。
 * 【概念】為何用 record？Kafka 事件是「已發生之事」——啟動／發出後不應被消費端改寫 payload。
 *         對照：JPA Entity、需逐步 {@code setXxx} 的 Request → class + Lombok。
 */
public record OrderExecuteCommand(Long orderId, Long userId) {
}

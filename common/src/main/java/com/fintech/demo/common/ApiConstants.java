package com.fintech.demo.common;

/**
 * 【職責】跨服務共用常數（API 前綴、Kafka topic 名等）。
 * 【技巧】Topic 名集中宣告，Producer／Consumer 避免字串漂移。
 * 【概念】order-events＝下單執行；trade-events＝成交入帳（account-service）。
 */
public final class ApiConstants {

    public static final String API_PREFIX = "/api";
    public static final String TOPIC_ORDER_EVENTS = "order-events";
    /** order 成交後發布；account-service 消費入帳。 */
    public static final String TOPIC_TRADE_EVENTS = "trade-events";

    private ApiConstants() {
    }
}

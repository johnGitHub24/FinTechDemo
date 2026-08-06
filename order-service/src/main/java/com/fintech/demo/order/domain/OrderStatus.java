package com.fintech.demo.order.domain;

/**
 * 【職責】訂單狀態機（精簡）：PENDING → ACCEPTED｜REJECTED｜CANCELLED。
 * 【技巧】成交／取消前都以 PENDING 做 guard，避免重入。
 * 【概念】狀態機比「隨意改欄位」更可講、更好測。
 */
public enum OrderStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELLED
}

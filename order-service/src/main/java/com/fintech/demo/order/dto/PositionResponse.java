package com.fintech.demo.order.dto;

import java.math.BigDecimal;

/**
 * 【職責】持倉查詢回應（標的／數量／均價）。
 * 【技巧】Java {@code record}；均價用 BigDecimal。
 * 【概念】為何用 record？查詢結果是不可變投影（API 回應）；PENDING 訂單／Entity 另當別論。
 */
public record PositionResponse(String symbol, Integer quantity, BigDecimal avgPrice) {
}

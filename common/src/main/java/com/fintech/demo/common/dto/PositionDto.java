package com.fintech.demo.common.dto;

import java.math.BigDecimal;

/**
 * 【職責】持倉對外 DTO（symbol／數量／均價）。
 * 【技巧】Java {@code record}：自動元件存取子／equals／hashCode；Jackson 以元件名序列化。
 * 【概念】為何用 record？持倉快照是不可變契約／回應載體；avgPrice 不應在傳輸中被就地修改。
 *         對照：需 {@code setXxx} 組裝的 Request → class + Lombok {@code @Data}。
 */
public record PositionDto(String symbol, Integer quantity, BigDecimal avgPrice) {
}

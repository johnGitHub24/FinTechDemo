package com.fintech.demo.order.dto;

import java.math.BigDecimal;

/**
 * 【職責】帳戶餘額查詢回應。
 * 【技巧】Java {@code record}：免手寫 getter／setter；與 Entity 分離。
 * 【概念】為何用 record？API 回應是對外不可變契約快照。Entity 可變（JPA）→ class + Lombok。
 */
public record AccountResponse(Long userId, BigDecimal cashBalance, String currency) {
}

package com.fintech.demo.order.dto;

import com.fintech.demo.order.domain.OrderSide;
import com.fintech.demo.order.domain.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 【職責】訂單回應 DTO。
 * 【技巧】class + Lombok {@code @Data}：Service 以 setter 從 Entity 逐欄映射（教學上較直觀）。
 * 【概念】此處保留可變映射風格；若欄位固定且一次建構，可改 record。Entity 本身必須 class + Lombok（JPA）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private Long userId;
    /** 下單者帳號（ADMIN 全站列表用來對帳，避免誤當成登入者本人）。 */
    private String username;
    private String clientOrderId;
    private String symbol;
    private OrderSide side;
    private Integer quantity;
    private BigDecimal price;
    private OrderStatus status;
    private Instant createdAt;
    /** 可選：展演用過程追蹤（誰／做什麼／狀態）。 */
    private DemoTrace demoTrace;
}

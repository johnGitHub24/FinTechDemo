package com.fintech.demo.order.dto;

import com.fintech.demo.order.domain.OrderSide;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 【職責】下單請求 DTO。
 * 【技巧】class + Lombok {@code @Data}（非 record）：Jackson／測試以 {@code setXxx} 組裝；Bean Validation 擋非法輸入。
 * 【概念】為何不用 record？Request 綁定階段需要可變；不可變訂單回應可另用 record 或本類映射後再輸出。
 *         規則：Properties／回應／Kafka 事件 → record；Entity／需 setXxx 的 Request → class + Lombok。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotBlank
    private String clientOrderId;

    @NotBlank
    private String symbol;

    @NotNull
    private OrderSide side;

    @NotNull
    @Min(1)
    private Integer quantity;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal price;
}

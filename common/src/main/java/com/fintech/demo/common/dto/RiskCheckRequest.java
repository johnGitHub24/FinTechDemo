package com.fintech.demo.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 【職責】風控檢查請求（order → risk）。
 * 【技巧】class + Lombok {@code @Data}（非 record）：無參建構＋{@code setXxx} 供 Feign／測試／Service 逐步組裝。
 * 【概念】為何不用 record？Request 在綁定／組裝階段需要可變；不可變結果見 {@link RiskCheckResponse} record。
 *         規則：Properties／API 回應／Kafka 事件 → record；Entity／需 setXxx 的 Request → class + Lombok。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskCheckRequest {
    private Long userId;
    private String symbol;
    private String side;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal cashBalance;
}

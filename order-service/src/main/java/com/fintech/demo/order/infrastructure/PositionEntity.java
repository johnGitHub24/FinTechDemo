package com.fintech.demo.order.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 【職責】持倉（後台 B2）；與 ACCEPTED 買單串連。
 * 【技巧】class + Lombok {@code @Getter}/{@code @Setter}（非 record、勿 {@code @Data}）：JPA 需無參建構與可變欄位。
 * 【概念】為何不用 record？Entity 有生命週期／dirty checking。API 持倉快照見 PositionResponse record。
 *         規則：Properties／回應／Kafka 事件 → record；JPA Entity／需 setXxx Request → class + Lombok。
 */
@Entity
@Table(name = "positions", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "symbol"}))
@Getter
@Setter
public class PositionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "avg_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal avgPrice;
}

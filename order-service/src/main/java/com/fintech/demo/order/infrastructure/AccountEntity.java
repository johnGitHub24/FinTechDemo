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
 * 【職責】用戶現金帳戶（後台餘額 B1）；JPA 實體對應 {@code accounts}。
 * 【技巧】class + Lombok {@code @Getter}/{@code @Setter}（非 record、勿 {@code @Data}）；與 {@code users} 一對一。
 * 【概念】為何不用 record？Entity 可變；對外餘額契約見 AccountDto／AccountResponse record。
 */
@Entity
@Table(name = "accounts", uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
@Getter
@Setter
public class AccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "cash_balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal cashBalance;

    @Column(nullable = false, length = 8)
    private String currency = "TWD";
}

package com.fintech.demo.order.infrastructure;

import com.fintech.demo.order.domain.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 【職責】使用者帳號（登入／RBAC 基礎表）。
 * 【技巧】class + Lombok {@code @Getter}/{@code @Setter}（非 record、勿 {@code @Data}）。
 * 【概念】為何不用 record？JPA Entity 必須可變；JWT 讀 passwordHash／role。登入回應才用 LoginResponse record。
 */
@Entity
@Table(name = "users")
@Getter
@Setter
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Role role;
}

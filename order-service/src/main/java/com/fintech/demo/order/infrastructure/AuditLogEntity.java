package com.fintech.demo.order.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 【職責】審計軌跡（後台 B4）；種子資料對應具體訂單動作。
 * 【技巧】class + Lombok {@code @Getter}/{@code @Setter}（非 record）；{@code @PrePersist} 補 createdAt。
 * 【概念】為何不用 record？JPA Entity 必須可變。規則：事件／回應 → record；Entity → class + Lombok。
 */
@Entity
@Table(name = "audit_log")
@Getter
@Setter
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(nullable = false, length = 64)
    private String resource;

    @Column(length = 512)
    private String detail;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}

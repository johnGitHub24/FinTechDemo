package com.fintech.demo.order.infrastructure;

import com.fintech.demo.order.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 【職責】訂單持久化與條件分頁查詢。
 * 【技巧】JPQL 以 {@code :status IS NULL OR ...} 做可選狀態篩選；{@code clientOrderId} 冪等檢查。
 * 【概念】查詢條件外置到 Repository，Service 保持流程可讀。
 */
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    boolean existsByClientOrderId(String clientOrderId);

    Optional<OrderEntity> findByIdAndUserId(Long id, Long userId);

    @Query("""
            SELECT o FROM OrderEntity o
            WHERE o.userId = :userId
              AND (:status IS NULL OR o.status = :status)
            """)
    Page<OrderEntity> findByUserIdAndOptionalStatus(
            @Param("userId") Long userId,
            @Param("status") OrderStatus status,
            Pageable pageable);

    @Query("""
            SELECT o FROM OrderEntity o
            WHERE (:status IS NULL OR o.status = :status)
            """)
    Page<OrderEntity> findByOptionalStatus(
            @Param("status") OrderStatus status,
            Pageable pageable);
}

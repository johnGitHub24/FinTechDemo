package com.fintech.demo.order.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 【職責】持倉持久化存取。
 * 【技巧】{@code findByUserIdAndSymbol} 對齊「一人一標的一列」模型。
 * 【概念】成交後 upsert 持倉；賣出前必須先有足夠數量。
 */
public interface PositionRepository extends JpaRepository<PositionEntity, Long> {
    List<PositionEntity> findByUserId(Long userId);

    Optional<PositionEntity> findByUserIdAndSymbol(Long userId, String symbol);
}

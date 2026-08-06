package com.fintech.demo.account.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 【職責】持倉 Repository。
 * 【技巧】Spring Data 方法名／JPQL 產生查詢。
 * 【概念】資料存取與領域規則分離，避免 Repository 膨脹。
 */
public interface PositionRepository extends JpaRepository<PositionEntity, Long> {

    List<PositionEntity> findByUserId(Long userId);

    Optional<PositionEntity> findByUserIdAndSymbol(Long userId, String symbol);
}

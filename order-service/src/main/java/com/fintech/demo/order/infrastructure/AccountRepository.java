package com.fintech.demo.order.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 【職責】帳戶（現金）持久化存取。
 * 【技巧】Spring Data 方法名 {@code findByUserId} 自動產生查詢。
 * 【概念】Repository 只做存取，扣款規則在 TradingService。
 */
public interface AccountRepository extends JpaRepository<AccountEntity, Long> {
    Optional<AccountEntity> findByUserId(Long userId);
}

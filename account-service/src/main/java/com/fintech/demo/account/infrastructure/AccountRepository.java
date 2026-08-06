package com.fintech.demo.account.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 【職責】帳戶 Repository。
 * 【技巧】Spring Data 方法名／JPQL 產生查詢。
 * 【概念】資料存取與領域規則分離，避免 Repository 膨脹。
 */
public interface AccountRepository extends JpaRepository<AccountEntity, Long> {

    Optional<AccountEntity> findByUserId(Long userId);
}

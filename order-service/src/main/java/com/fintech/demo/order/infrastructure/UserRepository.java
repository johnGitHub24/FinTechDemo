package com.fintech.demo.order.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 【職責】使用者持久化存取。
 * 【技巧】{@code findByUsername} 供登入與 JWT 主體解析。
 * 【概念】使用者與帳戶一對一，交易以 userId 串起來。
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername(String username);
}

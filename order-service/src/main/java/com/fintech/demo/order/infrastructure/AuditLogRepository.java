package com.fintech.demo.order.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 【職責】審計紀錄持久化與分頁查詢。
 * 【技巧】{@code findAllByOrderByCreatedAtDesc} 讓最新事件在前。
 * 【概念】審計表可追加、不改歷史，利於追溯。
 */
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {
    Page<AuditLogEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

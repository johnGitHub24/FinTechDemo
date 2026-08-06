package com.fintech.demo.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 【職責】審計紀錄查詢回應 DTO。
 * 【技巧】class + Lombok {@code @Data}：Service 以 setter 從 Entity 映射。
 * 【概念】審計是證據鏈；映射階段可變。純快照契約亦可改 record（見 AccountResponse）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private Long id;
    private String action;
    private String resource;
    private String detail;
    private String username;
    private Instant createdAt;
}

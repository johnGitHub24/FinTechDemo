package com.fintech.demo.common.dto;

import java.math.BigDecimal;

/**
 * 【職責】帳戶餘額對外 DTO（account-service ↔ gateway／Feign）。
 * 【技巧】Java {@code record}：自動產生 {@code userId()}／{@code cashBalance()}／{@code currency()}、
 *         equals／hashCode／toString，免手寫 getter／setter。
 * 【概念】為何用 record？跨服務契約／API 回應屬「組裝後不可變」快照，不應再被改寫。
 *         對照：JPA Entity、需逐步 {@code setXxx} 的 Request → class + Lombok。
 */
public record AccountDto(Long userId, BigDecimal cashBalance, String currency) {
}

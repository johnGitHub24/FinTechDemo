package com.fintech.demo.order.client;

import com.fintech.demo.common.dto.AccountDto;
import com.fintech.demo.common.dto.ApplyTradeRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 【職責】呼叫 account-service 內部帳本 API（本版：固定 URL OpenFeign）。
 * 【技巧】url 來自 {@code fintech.services.account-url}；內部呼叫帶 {@code X-Internal-Token}。
 * 【概念】order 編排成交後可同步入帳；account 管現金／持倉（第三業務 MS）。
 *         服務發現升級同 {@link RiskClient}：拿掉 url → Eureka／服務名（SPEC §2.3）。
 */
@FeignClient(name = "account-service", url = "${fintech.services.account-url}")
public interface AccountClient {

    /** 【職責】查內部帳戶餘額。 */
    @GetMapping("/api/internal/accounts/{userId}")
    AccountDto getAccount(
            @PathVariable("userId") Long userId,
            @RequestHeader("X-Internal-Token") String token);

    /** 【職責】套用一筆成交至帳戶（扣／加現金與持倉）。 */
    @PostMapping("/api/internal/accounts/{userId}/apply-trade")
    AccountDto applyTrade(
            @PathVariable("userId") Long userId,
            @RequestBody ApplyTradeRequest request,
            @RequestHeader("X-Internal-Token") String token);
}

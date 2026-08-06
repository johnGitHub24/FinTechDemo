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
 * 【職責】呼叫 account-service 內部帳本 API。
 * 【技巧】url 來自設定，固定 URL 示意服務拆分。
 * 【概念】order 編排；account 管現金／持倉（第三業務微服務）。
 */
@FeignClient(name = "account-service", url = "${fintech.services.account-url}")
public interface AccountClient {

    @GetMapping("/api/internal/accounts/{userId}")
    AccountDto getAccount(
            @PathVariable("userId") Long userId,
            @RequestHeader("X-Internal-Token") String token);

    @PostMapping("/api/internal/accounts/{userId}/apply-trade")
    AccountDto applyTrade(
            @PathVariable("userId") Long userId,
            @RequestBody ApplyTradeRequest request,
            @RequestHeader("X-Internal-Token") String token);
}

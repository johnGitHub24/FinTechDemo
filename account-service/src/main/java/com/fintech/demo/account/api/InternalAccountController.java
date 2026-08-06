package com.fintech.demo.account.api;

import com.fintech.demo.account.application.AccountLedgerService;
import com.fintech.demo.account.application.AccountQueryService;
import com.fintech.demo.common.dto.AccountDto;
import com.fintech.demo.common.dto.ApplyTradeRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 【職責】內部 API：Feign 同步入帳、查帳戶（X-Job-Token／X-Internal-Token）。
 * 【技巧】兩個 header 擇一等於 demo-job-token 即可，方便 Job 與 order 共用。
 * 【概念】permitAll + 自訂 token：避免內部服務再走 JWT 登入。
 */
@RestController
@RequestMapping("/api/internal/accounts")
public class InternalAccountController {

    private final AccountLedgerService ledgerService;
    private final AccountQueryService queryService;
    private final String jobToken;

    public InternalAccountController(
            AccountLedgerService ledgerService,
            AccountQueryService queryService,
            @Value("${fintech.job.token:demo-job-token}") String jobToken) {
        this.ledgerService = ledgerService;
        this.queryService = queryService;
        this.jobToken = jobToken;
    }

    /**
     * 【職責】驗證內部 token 後，為指定使用者同步套用成交帳本更新。
     * 【技巧】校驗 path 與 body 的 userId 一致，入帳後立即淘汰快取。
     * 【概念】Feign 同步路徑必須防止呼叫方替另一個使用者入帳。
     */
    @PostMapping("/{userId}/apply-trade")
    public AccountDto applyTrade(
            @PathVariable Long userId,
            @RequestBody ApplyTradeRequest request,
            @RequestHeader(value = "X-Job-Token", required = false) String jobHeader,
            @RequestHeader(value = "X-Internal-Token", required = false) String internalHeader) {
        assertInternalToken(jobHeader, internalHeader);
        if (request.getUserId() == null) {
            request.setUserId(userId);
        } else if (!userId.equals(request.getUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId mismatch");
        }
        AccountDto result = ledgerService.applyTrade(request);
        queryService.evict(userId);
        return result;
    }

    /**
     * 【職責】驗證內部 token 後取得指定使用者的帳戶。
     * 【技巧】先通過服務對服務 token 驗證，才委派 cache-aware QueryService。
     * 【概念】內部查詢可避開使用者 JWT，但不可避開呼叫端身分驗證。
     */
    @GetMapping("/{userId}")
    public AccountDto getAccount(
            @PathVariable Long userId,
            @RequestHeader(value = "X-Job-Token", required = false) String jobHeader,
            @RequestHeader(value = "X-Internal-Token", required = false) String internalHeader) {
        assertInternalToken(jobHeader, internalHeader);
        return queryService.getAccount(userId);
    }

    /**
     * 【職責】確認任一允許的內部 header 帶有正確 token。
     * 【技巧】X-Job-Token 與 X-Internal-Token 任一相符即放行，否則回應 403。
     * 【概念】以明確的服務 token 分隔內部 API 與公開 JWT API。
     */
    private void assertInternalToken(String jobHeader, String internalHeader) {
        if (jobToken.equals(jobHeader) || jobToken.equals(internalHeader)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid internal token");
    }
}

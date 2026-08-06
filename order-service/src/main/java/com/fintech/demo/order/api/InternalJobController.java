package com.fintech.demo.order.api;

import com.fintech.demo.order.application.StaleOrderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * 【職責】供 job-service 觸發的內部 Job API。
 * 【技巧】只做參數／驗證／HTTP 狀態；商業規則在 Service。
 * 【概念】薄 Controller 利於測試與替換傳輸層。
 */
@RestController
@RequestMapping("/api/internal/jobs")
public class InternalJobController {

    private final StaleOrderService staleOrderService;
    private final String jobToken;

    public InternalJobController(
            StaleOrderService staleOrderService,
            @Value("${fintech.job.token:demo-job-token}") String jobToken) {
        this.staleOrderService = staleOrderService;
        this.jobToken = jobToken;
    }

    /**
     * 【職責】供 job-service 請求取消逾時未成交訂單。
     * 【技巧】先比對 X-Job-Token 再執行服務，拒絕缺失或不符的內部呼叫。
     * 【概念】內部端點可不經使用者 JWT，但仍需服務對服務的最小驗證。
     */
    @PostMapping("/cancel-stale")
    public Map<String, Object> cancelStale(@RequestHeader(value = "X-Job-Token", required = false) String token) {
        if (!jobToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid job token");
        }
        int n = staleOrderService.cancelStalePending();
        return Map.of("cancelled", n);
    }
}

package com.fintech.demo.risk.api;

import com.fintech.demo.common.dto.RiskCheckRequest;
import com.fintech.demo.common.dto.RiskCheckResponse;
import com.fintech.demo.risk.application.RiskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 【職責】風控 HTTP API，供 order-service Feign 呼叫。
 * 【技巧】只做參數／驗證／HTTP 狀態；商業規則在 Service。
 * 【概念】薄 Controller 利於測試與替換傳輸層。
 */
@RestController
@RequestMapping("/api/risk")
public class RiskController {

    private final RiskService riskService;

    public RiskController(RiskService riskService) {
        this.riskService = riskService;
    }

    @PostMapping("/check")
    public RiskCheckResponse check(@Valid @RequestBody RiskCheckRequest request) {
        return riskService.check(request);
    }
}

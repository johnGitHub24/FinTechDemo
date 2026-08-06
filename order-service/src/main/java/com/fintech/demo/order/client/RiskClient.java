package com.fintech.demo.order.client;

import com.fintech.demo.common.dto.RiskCheckRequest;
import com.fintech.demo.common.dto.RiskCheckResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 【職責】呼叫 risk-service 風控 API（固定 URL Feign）。
 * 【技巧】url 來自設定，固定 URL 示意服務拆分。
 * 【概念】可說明：下一步可換成服務發現。
 */
@FeignClient(name = "risk-service", url = "${fintech.services.risk-url}")
public interface RiskClient {

    @PostMapping("/api/risk/check")
    RiskCheckResponse check(@RequestBody RiskCheckRequest request);
}

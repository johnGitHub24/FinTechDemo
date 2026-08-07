package com.fintech.demo.order.client;

import com.fintech.demo.common.dto.RiskCheckRequest;
import com.fintech.demo.common.dto.RiskCheckResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 【職責】呼叫 risk-service 風控 API（本版：固定 URL OpenFeign）。
 * 【技巧】{@code @FeignClient(name=..., url="${fintech.services.risk-url}")}；成交路徑同步等待風控結果。
 * 【概念】固定 URL 先把交易鏈跑穩。升級時拿掉 {@code url}、改依服務名＋Eureka，
 *         Gateway 改 {@code lb://}——這是發現機制升級，不是重寫業務（見 FinTechDemo-SPEC §2.3；
 *         完整串接見 TradingMicroService）。
 */
@FeignClient(name = "risk-service", url = "${fintech.services.risk-url}")
public interface RiskClient {

    /** 【職責】送出名目金額風控檢查請求。 */
    @PostMapping("/api/risk/check")
    RiskCheckResponse check(@RequestBody RiskCheckRequest request);
}

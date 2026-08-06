package com.fintech.demo.job.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * 【職責】呼叫 order-service 內部 Job API（薄 Job 編排）。
 * 【技巧】讀多用 @Transactional(readOnly=true)；寫入走預設交易。
 * 【概念】Service 是 Demo 最常說明的「流程編排」層。
 */
@Service
public class RemoteJobTriggerService {

    private static final Logger log = LoggerFactory.getLogger(RemoteJobTriggerService.class);

    private final RestClient restClient;
    private final String jobToken;

    public RemoteJobTriggerService(
            @Value("${fintech.order-url}") String orderUrl,
            @Value("${fintech.job.token}") String jobToken) {
        this.restClient = RestClient.builder().baseUrl(orderUrl).build();
        this.jobToken = jobToken;
    }

    /**
     * 【職責】呼叫 order-service 內部端點以取消逾時訂單。
     * 【技巧】以 RestClient 帶 X-Job-Token POST；遠端不可用時記錄 warn 而不讓排程執行緒失敗。
     * 【概念】Job-service 是遠端工作觸發者，不直接耦合 order-service 的資料庫。
     */
    public void triggerCancelStale() {
        try {
            String body = restClient.post()
                    .uri("/api/internal/jobs/cancel-stale")
                    .header("X-Job-Token", jobToken)
                    .retrieve()
                    .body(String.class);
            log.info("Triggered cancel-stale: {}", body);
        } catch (Exception ex) {
            log.warn("cancel-stale failed (order-service up?): {}", ex.getMessage());
        }
    }
}

package com.fintech.demo.order.job;

import com.fintech.demo.order.application.StaleOrderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 【職責】定時取消逾時 PENDING（Job 薄、Service 厚）。
 * 【技巧】配合同套件 Service／Controller 使用。
 * 【概念】教學 Demo 以可講清邊界為優先。
 */
@Component
public class StaleOrderCancelJob {

    private final StaleOrderService staleOrderService;

    public StaleOrderCancelJob(StaleOrderService staleOrderService) {
        this.staleOrderService = staleOrderService;
    }

    @Scheduled(cron = "${fintech.job.cancel-cron:0 */1 * * * *}")
    public void run() {
        staleOrderService.cancelStalePending();
    }
}

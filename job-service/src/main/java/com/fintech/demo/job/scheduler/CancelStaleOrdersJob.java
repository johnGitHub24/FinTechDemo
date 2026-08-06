package com.fintech.demo.job.scheduler;

import com.fintech.demo.job.application.RemoteJobTriggerService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 【職責】依 cron 週期觸發「逾時未成交訂單取消」。
 * 【技巧】{@code @Scheduled(cron = "${fintech.job.cancel-cron}")} 把頻率外置到設定檔。
 * 【概念】Job 本身不碰 DB，只呼叫 RemoteJobTriggerService → order-service internal API。
 */
@Component
public class CancelStaleOrdersJob {

    private final RemoteJobTriggerService remoteJobTriggerService;

    public CancelStaleOrdersJob(RemoteJobTriggerService remoteJobTriggerService) {
        this.remoteJobTriggerService = remoteJobTriggerService;
    }

    /**
     * 【職責】按設定 cron 啟動逾時訂單取消工作。
     * 【技巧】排程方法僅委派 RemoteJobTriggerService，頻率不硬編碼於程式。
     * 【概念】排程與業務邏輯分離可讓工作可測試、可替換且可跨服務部署。
     */
    @Scheduled(cron = "${fintech.job.cancel-cron}")
    public void run() {
        remoteJobTriggerService.triggerCancelStale();
    }
}

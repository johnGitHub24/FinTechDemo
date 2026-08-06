package com.fintech.demo.job.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * 【職責】啟用並設定 job-service 的排程執行緒池。
 * 【技巧】{@code SchedulingConfigurer} 自訂 {@link ThreadPoolTaskScheduler}，避免預設單執行緒卡住。
 * 【概念】排程與業務觸發分離：此處只管「何時跑」，真正取消在 RemoteJobTriggerService。
 */
@Configuration
@EnableScheduling
public class SchedulingConfig implements SchedulingConfigurer {

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("fintech-job-svc-");
        scheduler.initialize();
        taskRegistrar.setTaskScheduler(scheduler);
    }
}

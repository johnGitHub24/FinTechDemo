package com.fintech.demo.order.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * 【職責】多執行緒排程池（EOS：pool-size ≥ 2）。
 * 【技巧】以 @Configuration／Properties 外置環境差異。
 * 【概念】組態與業務分離，本機／Docker profile 才好切。
 */
@Configuration
@EnableScheduling
public class SchedulingConfig implements SchedulingConfigurer {

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("fintech-job-");
        scheduler.initialize();
        taskRegistrar.setTaskScheduler(scheduler);
    }
}

package com.fintech.demo.job;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 【職責】job-service 啟動入口（排程觸發逾時取消）。
 * 【技巧】{@code @SpringBootApplication} 掃描本模組；實際取消邏輯透過 HTTP 打 order internal API。
 * 【概念】把排程拆成獨立進程，避免跟交易熱路徑搶執行緒。
 */
@SpringBootApplication
public class JobServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobServiceApplication.class, args);
    }
}

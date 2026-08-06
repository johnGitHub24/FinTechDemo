package com.fintech.demo.order.dto;

/**
 * 【職責】demoTrace 單一 hop（過程步驟）。
 * 【技巧】record 不可變；ok=false 時 detail 寫失敗摘要。
 * 【概念】展演用輕量 trace，非 OpenTelemetry span。
 */
public record DemoHop(String service, Integer port, boolean ok, String detail) {

    public static DemoHop of(String service, Integer port, boolean ok, String detail) {
        return new DemoHop(service, port, ok, detail);
    }
}

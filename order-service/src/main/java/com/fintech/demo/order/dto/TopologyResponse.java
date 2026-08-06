package com.fintech.demo.order.dto;

import java.time.Instant;
import java.util.List;

/**
 * 【職責】Demo 拓撲探測回應（服務燈＋推斷 S 階）。
 * 【技巧】巢狀 record；前端同 origin 呼叫，避開跨埠 CORS。
 * 【概念】環境事實層，與單筆 demoTrace 互補。
 */
public record TopologyResponse(List<ServiceHealth> services, String inferredStage, Instant at) {

    public record ServiceHealth(String id, String label, int port, String url, boolean up) {
    }
}

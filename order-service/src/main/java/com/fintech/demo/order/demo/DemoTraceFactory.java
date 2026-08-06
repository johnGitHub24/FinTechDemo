package com.fintech.demo.order.demo;

import com.fintech.demo.order.dto.DemoHop;
import com.fintech.demo.order.dto.DemoTrace;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 【職責】組裝 create／execute／cancel 的 demoTrace。
 * 【技巧】viaGateway 時在 hops 前端插入 gateway；risk 成敗都寫 hop。
 * 【概念】輕量展演契約，讓前端 PROCESS FLOW 有事實依據。
 */
public final class DemoTraceFactory {

    private DemoTraceFactory() {
    }

    public static DemoTrace forCreate(boolean viaGateway, Long orderId, String status) {
        List<DemoHop> hops = new ArrayList<>();
        if (viaGateway) {
            hops.add(DemoHop.of("gateway", 8080, true, "route /api/** → order"));
        }
        hops.add(DemoHop.of("order-service", 8081, true, "persist PENDING order"));
        return build("CREATE_ORDER", viaGateway, orderId, status, hops);
    }

    public static DemoTrace forExecute(
            boolean viaGateway,
            Long orderId,
            String status,
            boolean riskOk,
            String riskDetail) {
        List<DemoHop> hops = new ArrayList<>();
        if (viaGateway) {
            hops.add(DemoHop.of("gateway", 8080, true, "route /api/** → order"));
        }
        hops.add(DemoHop.of("order-service", 8081, true, "execute flow"));
        hops.add(DemoHop.of("risk-service", 8082, riskOk, riskDetail));
        return build("EXECUTE", viaGateway, orderId, status, hops);
    }

    public static DemoTrace forCancel(boolean viaGateway, Long orderId, String status) {
        List<DemoHop> hops = new ArrayList<>();
        if (viaGateway) {
            hops.add(DemoHop.of("gateway", 8080, true, "route /api/** → order"));
        }
        hops.add(DemoHop.of("order-service", 8081, true, "cancel PENDING → CANCELLED"));
        return build("CANCEL", viaGateway, orderId, status, hops);
    }

    private static DemoTrace build(
            String action,
            boolean viaGateway,
            Long orderId,
            String status,
            List<DemoHop> hops) {
        return new DemoTrace(
                UUID.randomUUID().toString(),
                action,
                viaGateway,
                null,
                orderId,
                status,
                List.copyOf(hops),
                Instant.now());
    }
}

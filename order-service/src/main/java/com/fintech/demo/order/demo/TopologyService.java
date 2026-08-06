package com.fintech.demo.order.demo;

import com.fintech.demo.order.dto.TopologyResponse;
import com.fintech.demo.order.dto.TopologyResponse.ServiceHealth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 【職責】伺服器端探測各服務 actuator health，組裝拓撲燈號。
 * 【技巧】RestClient 短超時；失敗＝紅燈，不拋到 API 呼叫端。
 * 【概念】給前端 PROCESS FLOW 儀表板「環境開到哪」的事實依據。
 */
@Service
public class TopologyService {

    private final RestClient restClient = RestClient.create();
    private final String gatewayUrl;
    private final String orderSelfUrl;
    private final int orderSelfPort;
    private final String riskUrl;
    private final String jobUrl;
    private final String accountUrl;

    public TopologyService(
            @Value("${fintech.services.gateway-url:http://localhost:8080}") String gatewayUrl,
            @Value("${fintech.services.order-self-url:http://localhost:8081}") String orderSelfUrl,
            @Value("${fintech.services.order-self-port:8081}") int orderSelfPort,
            @Value("${fintech.services.risk-url:http://localhost:8082}") String riskUrl,
            @Value("${fintech.services.job-url:http://localhost:8083}") String jobUrl,
            @Value("${fintech.services.account-url:http://localhost:8084}") String accountUrl) {
        this.gatewayUrl = gatewayUrl;
        this.orderSelfUrl = orderSelfUrl;
        this.orderSelfPort = orderSelfPort;
        this.riskUrl = riskUrl;
        this.jobUrl = jobUrl;
        this.accountUrl = accountUrl;
    }

    public TopologyResponse probe() {
        List<ServiceHealth> services = new ArrayList<>();
        services.add(probeOne("gateway", "Gateway", 8080, gatewayUrl));
        services.add(new ServiceHealth("order", "Order", orderSelfPort, orderSelfUrl, true));
        services.add(probeOne("risk", "Risk", 8082, riskUrl));
        services.add(probeOne("job", "Job", 8083, jobUrl));
        services.add(probeOne("account", "Account", 8084, accountUrl));
        return new TopologyResponse(List.copyOf(services), inferStage(services), Instant.now());
    }

    static String inferStage(List<ServiceHealth> services) {
        boolean order = up(services, "order");
        boolean risk = up(services, "risk");
        boolean gateway = up(services, "gateway");
        boolean account = up(services, "account");
        if (order && risk && (gateway || account)) {
            return "S3";
        }
        if (order && risk) {
            return "S2";
        }
        if (order) {
            return "S1";
        }
        return "S0";
    }

    private static boolean up(List<ServiceHealth> services, String id) {
        return services.stream().anyMatch(s -> id.equals(s.id()) && s.up());
    }

    private ServiceHealth probeOne(String id, String label, int port, String baseUrl) {
        String healthUrl = trimSlash(baseUrl) + "/actuator/health";
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = restClient.get()
                    .uri(healthUrl)
                    .retrieve()
                    .body(Map.class);
            boolean up = body != null && "UP".equalsIgnoreCase(String.valueOf(body.get("status")));
            return new ServiceHealth(id, label, port, baseUrl, up);
        } catch (Exception ex) {
            return new ServiceHealth(id, label, port, baseUrl, false);
        }
    }

    private static String trimSlash(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}

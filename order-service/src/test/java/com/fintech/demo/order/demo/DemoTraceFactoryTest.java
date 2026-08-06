package com.fintech.demo.order.demo;

import com.fintech.demo.order.dto.DemoHop;
import com.fintech.demo.order.dto.DemoTrace;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 【職責】驗證 DemoTraceFactory 組裝 hops／viaGateway。
 */
class DemoTraceFactoryTest {

    @Test
    void executeTrace_whenRiskOk_shouldIncludeOrderAndRiskHops() {
        DemoTrace t = DemoTraceFactory.forExecute(false, 5L, "ACCEPTED", true, "notional within limit");
        assertThat(t.action()).isEqualTo("EXECUTE");
        assertThat(t.hops()).extracting(DemoHop::service)
                .containsExactly("order-service", "risk-service");
        assertThat(t.hops().get(1).ok()).isTrue();
    }

    @Test
    void executeTrace_whenViaGateway_shouldPrefixGatewayHop() {
        DemoTrace t = DemoTraceFactory.forExecute(true, 5L, "ACCEPTED", true, "ok");
        assertThat(t.viaGateway()).isTrue();
        assertThat(t.hops().get(0).service()).isEqualTo("gateway");
    }

    @Test
    void executeTrace_whenRiskFails_shouldMarkRiskHopDown() {
        DemoTrace t = DemoTraceFactory.forExecute(false, 5L, "REJECTED", false, "over limit");
        assertThat(t.hops().get(1).ok()).isFalse();
        assertThat(t.hops().get(1).detail()).contains("over limit");
    }
}

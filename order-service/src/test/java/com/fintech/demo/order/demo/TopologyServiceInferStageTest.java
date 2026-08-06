package com.fintech.demo.order.demo;

import com.fintech.demo.order.dto.TopologyResponse.ServiceHealth;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 【職責】驗證拓撲階段推斷規則 S1／S2／S3。
 */
class TopologyServiceInferStageTest {

    @Test
    void inferStage_orderOnly_isS1() {
        assertThat(TopologyService.inferStage(List.of(
                sh("order", true),
                sh("risk", false),
                sh("gateway", false),
                sh("account", false)
        ))).isEqualTo("S1");
    }

    @Test
    void inferStage_orderAndRisk_isS2() {
        assertThat(TopologyService.inferStage(List.of(
                sh("order", true),
                sh("risk", true),
                sh("gateway", false),
                sh("account", false)
        ))).isEqualTo("S2");
    }

    @Test
    void inferStage_withGateway_isS3() {
        assertThat(TopologyService.inferStage(List.of(
                sh("order", true),
                sh("risk", true),
                sh("gateway", true),
                sh("account", false)
        ))).isEqualTo("S3");
    }

    private static ServiceHealth sh(String id, boolean up) {
        return new ServiceHealth(id, id, 0, "http://x", up);
    }
}

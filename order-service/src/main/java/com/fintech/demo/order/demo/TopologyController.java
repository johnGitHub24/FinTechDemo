package com.fintech.demo.order.demo;

import com.fintech.demo.order.dto.TopologyResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 【職責】Demo 拓撲探測 API（公開，登入頁亦可呼叫）。
 * 【技巧】GET /api/demo/topology；由 {@link TopologyService} 代 ping health。
 * 【概念】避開瀏覽器跨埠 CORS；訪客可在 /login 看到 Risk 是否 UP（成交必要條件）。
 */
@RestController
@RequestMapping("/api/demo")
public class TopologyController {

    private final TopologyService topologyService;

    public TopologyController(TopologyService topologyService) {
        this.topologyService = topologyService;
    }

    @GetMapping("/topology")
    public TopologyResponse topology() {
        return topologyService.probe();
    }
}

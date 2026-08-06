package com.fintech.demo.order.demo;

import com.fintech.demo.order.dto.TopologyResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 【職責】Demo 拓撲探測 API（需 JWT，與其他 /api 一致）。
 * 【技巧】GET /api/demo/topology；由 {@link TopologyService} 代 ping health。
 * 【概念】避開瀏覽器跨埠 CORS，仍呈現環境事實燈。
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

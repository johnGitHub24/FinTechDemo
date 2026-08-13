package com.fintech.demo.gateway.web;

import com.fintech.demo.gateway.filter.RateLimitWebFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 【職責】Gateway 限流 Spring 整合：注入正式 Filter bean＋測試組態。
 * 【技巧】不走下游 HTTP 代理（本機無 order-service）；直接打 Filter。
 * 【概念】與 RateLimitWebFilterTest 的 GW-004／GW-005 成對；閾值來自 TestPropertySource。
 */
@Tag("integration")
@SpringBootTest
@TestPropertySource(properties = "fintech.gateway.rate-limit.per-second=3")
class GatewayRateLimitIntegrationTest {

    @Autowired
    private RateLimitWebFilter filter;

    /**
     * CASE GW-004：未超限請求放行（非 429）。
     */
    @Test
    void GW_004_underLimit_doesNotReturn429() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/orders");
        req.addHeader("X-Forwarded-For", "10.0.4.1");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        verify(chain, times(1)).doFilter(req, res);
        assertNotEquals(429, res.getStatus());
    }

    /**
     * CASE GW-005：超過每秒上限 → 429 + Retry-After。
     */
    @Test
    void GW_005_overLimit_returns429() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/orders");
            req.addHeader("X-Forwarded-For", "10.0.5.2");
            filter.doFilter(req, new MockHttpServletResponse(), chain);
        }
        MockHttpServletRequest blockedReq = new MockHttpServletRequest("GET", "/api/orders");
        blockedReq.addHeader("X-Forwarded-For", "10.0.5.2");
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(blockedReq, blocked, chain);

        assertEquals(429, blocked.getStatus());
        assertEquals("1", blocked.getHeader("Retry-After"));
    }
}

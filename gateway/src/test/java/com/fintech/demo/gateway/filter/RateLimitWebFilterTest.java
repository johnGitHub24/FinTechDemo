package com.fintech.demo.gateway.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 【職責】驗證 Gateway Demo 限流：通過與超限 429。
 * 【技巧】直接呼叫 filter，不啟動完整 Spring context。
 * 【概念】CASE GW-004／GW-005 與 Gateway HTTP 整合成對。
 */
class RateLimitWebFilterTest {

    private RateLimitWebFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new RateLimitWebFilter(3);
        chain = mock(FilterChain.class);
    }

    /**
     * CASE GW-004：未超限請求放行。
     */
    @Test
    void GW_004_underLimit_passesThrough() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/orders");
        req.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);
        filter.doFilter(req, res, chain);

        verify(chain, times(2)).doFilter(req, res);
        assertEquals(200, res.getStatus());
    }

    /**
     * CASE GW-005：超過每秒上限 → 429。
     */
    @Test
    void GW_005_overLimit_returns429() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/orders");
        req.setRemoteAddr("10.0.0.2");
        MockHttpServletResponse res = new MockHttpServletResponse();

        for (int i = 0; i < 3; i++) {
            filter.doFilter(req, res, chain);
        }
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(req, blocked, chain);

        verify(chain, times(3)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(chain, never()).doFilter(req, blocked);
        assertEquals(429, blocked.getStatus());
        assertEquals("1", blocked.getHeader("Retry-After"));
    }
}

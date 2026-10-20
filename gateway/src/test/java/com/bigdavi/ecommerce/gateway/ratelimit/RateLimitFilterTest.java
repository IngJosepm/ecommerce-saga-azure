package com.bigdavi.ecommerce.gateway.ratelimit;

import com.bigdavi.ecommerce.gateway.config.GatewayProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    final RateLimitFilter filter = new RateLimitFilter(new GatewayProperties.RateLimit(3, Duration.ofMinutes(1)));

    @Test
    void bloqueaConHttp429AlSuperarElLimitePorIp() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(call("10.0.0.1").getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse blocked = call("10.0.0.1");
        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isNotBlank();

        // otra IP tiene su propio bucket
        assertThat(call("10.0.0.2").getStatus()).isEqualTo(200);
    }

    @Test
    void usaLaIpRealDeXForwardedForDetrasDelIngressDeAzure() throws Exception {
        for (int i = 0; i < 3; i++) {
            call("200.1.1.1, 10.0.0.9");
        }
        assertThat(call("200.1.1.1").getStatus()).isEqualTo(429);
    }

    private MockHttpServletResponse call(String forwardedFor) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
        request.addHeader("X-Forwarded-For", forwardedFor);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}

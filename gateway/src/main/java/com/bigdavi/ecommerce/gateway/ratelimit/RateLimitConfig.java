package com.bigdavi.ecommerce.gateway.ratelimit;

import com.bigdavi.ecommerce.gateway.config.GatewayProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class RateLimitConfig {

    /** Se ejecuta antes que Spring Security: también protege /auth/login contra fuerza bruta. */
    @Bean
    FilterRegistrationBean<RateLimitFilter> rateLimitFilter(GatewayProperties properties) {
        FilterRegistrationBean<RateLimitFilter> registration =
                new FilterRegistrationBean<>(new RateLimitFilter(properties.rateLimit()));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}

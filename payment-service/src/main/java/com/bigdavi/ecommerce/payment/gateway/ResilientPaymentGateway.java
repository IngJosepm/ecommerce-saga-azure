package com.bigdavi.ecommerce.payment.gateway;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * Envuelve la pasarela con Resilience4j:
 * - TimeLimiter: si tarda más de 2 s se considera fallo.
 * - Circuit Breaker: si el 50% de las últimas 10 llamadas fallan, se "abre" 20 s y
 *   responde de inmediato sin llamar a la pasarela (no se satura un sistema que ya está caído).
 * Cualquier fallo técnico se traduce en un rechazo con motivo GATEWAY_UNAVAILABLE.
 */
@Component
public class ResilientPaymentGateway {

    static final String CIRCUIT_BREAKER_ID = "payment-gateway";
    private static final Logger log = LoggerFactory.getLogger(ResilientPaymentGateway.class);

    private final FakePaymentGateway gateway;
    private final CircuitBreaker circuitBreaker;

    public ResilientPaymentGateway(FakePaymentGateway gateway, CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.gateway = gateway;
        this.circuitBreaker = circuitBreakerFactory.create(CIRCUIT_BREAKER_ID);
    }

    public GatewayResult charge(UUID orderId, BigDecimal amount) {
        return circuitBreaker.run(() -> gateway.charge(orderId, amount), error -> {
            String reason = switch (error) {
                case CallNotPermittedException ignored -> "GATEWAY_UNAVAILABLE_CIRCUIT_OPEN";
                case TimeoutException ignored -> "GATEWAY_UNAVAILABLE_TIMEOUT";
                default -> "GATEWAY_UNAVAILABLE";
            };
            log.warn("Pago del pedido {} no procesado: {} ({})", orderId, reason, error.toString());
            return GatewayResult.declined(reason);
        });
    }

    @Configuration
    static class CircuitBreakerSettings {

        @Bean
        Customizer<Resilience4JCircuitBreakerFactory> paymentGatewayCircuitBreaker() {
            return factory -> factory.configure(builder -> builder
                    .timeLimiterConfig(TimeLimiterConfig.custom()
                            .timeoutDuration(Duration.ofSeconds(2))
                            .build())
                    .circuitBreakerConfig(CircuitBreakerConfig.custom()
                            .slidingWindowSize(10)
                            .minimumNumberOfCalls(5)
                            .failureRateThreshold(50)
                            .waitDurationInOpenState(Duration.ofSeconds(20))
                            .permittedNumberOfCallsInHalfOpenState(3)
                            .build()), CIRCUIT_BREAKER_ID);
        }
    }
}

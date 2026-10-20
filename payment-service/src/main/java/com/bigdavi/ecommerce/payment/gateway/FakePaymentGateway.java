package com.bigdavi.ecommerce.payment.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Pasarela de pagos simulada (en un caso real: Stripe, Mercado Pago, Niubiz...).
 * - Montos mayores a approval-limit se rechazan (tarjeta sin fondos).
 * - failure-rate: probabilidad de error técnico (HTTP 503).
 * - slow-rate: probabilidad de que tarde más que el timeout del Circuit Breaker.
 */
@Component
public class FakePaymentGateway {

    private final BigDecimal approvalLimit;
    private final double failureRate;
    private final double slowRate;

    public FakePaymentGateway(@Value("${payment.gateway.approval-limit:1000}") BigDecimal approvalLimit,
                              @Value("${payment.gateway.failure-rate:0.0}") double failureRate,
                              @Value("${payment.gateway.slow-rate:0.0}") double slowRate) {
        this.approvalLimit = approvalLimit;
        this.failureRate = failureRate;
        this.slowRate = slowRate;
    }

    /** orderId actúa como clave de idempotencia: una pasarela real no cobra dos veces la misma clave. */
    public GatewayResult charge(UUID orderId, BigDecimal amount) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        sleep(Duration.ofMillis(random.nextLong(50, 300)));

        if (random.nextDouble() < slowRate) {
            sleep(Duration.ofSeconds(10));
        }
        if (random.nextDouble() < failureRate) {
            throw new GatewayUnavailableException("Pasarela respondió 503 Service Unavailable");
        }
        if (amount.compareTo(approvalLimit) > 0) {
            return GatewayResult.declined("CARD_DECLINED_INSUFFICIENT_FUNDS");
        }
        return GatewayResult.approved("txn_" + orderId.toString().substring(0, 8));
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GatewayUnavailableException("Llamada interrumpida");
        }
    }
}

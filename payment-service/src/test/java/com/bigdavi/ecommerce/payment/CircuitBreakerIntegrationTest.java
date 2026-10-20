package com.bigdavi.ecommerce.payment;

import com.bigdavi.ecommerce.common.test.SagaTestcontainers;
import com.bigdavi.ecommerce.payment.gateway.GatewayResult;
import com.bigdavi.ecommerce.payment.gateway.ResilientPaymentGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Pasarela 100% caída: tras 5 fallos el circuito se abre y deja de llamarla. */
@SpringBootTest(properties = "payment.gateway.failure-rate=1.0")
@Import(SagaTestcontainers.class)
class CircuitBreakerIntegrationTest {

    @Autowired
    ResilientPaymentGateway gateway;

    @Test
    void elCircuitoSeAbreTrasCincoFallos() {
        List<String> reasons = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            GatewayResult result = gateway.charge(UUID.randomUUID(), new BigDecimal("10.00"));
            assertThat(result.approved()).isFalse();
            reasons.add(result.failureReason());
        }

        assertThat(reasons.subList(0, 5)).containsOnly("GATEWAY_UNAVAILABLE");
        assertThat(reasons.subList(5, 8)).containsOnly("GATEWAY_UNAVAILABLE_CIRCUIT_OPEN");
    }
}

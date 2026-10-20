package com.bigdavi.ecommerce.common.events;

import com.bigdavi.ecommerce.common.messaging.SagaTopology;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** (3a) El pago fue aprobado. */
public record PaymentProcessedEvent(UUID eventId, UUID orderId, UUID paymentId, BigDecimal amount,
                                    Instant occurredAt) implements SagaEvent {

    public PaymentProcessedEvent(UUID orderId, UUID paymentId, BigDecimal amount) {
        this(UUID.randomUUID(), orderId, paymentId, amount, Instant.now());
    }

    @Override
    public String routingKey() {
        return SagaTopology.PAYMENT_PROCESSED;
    }
}

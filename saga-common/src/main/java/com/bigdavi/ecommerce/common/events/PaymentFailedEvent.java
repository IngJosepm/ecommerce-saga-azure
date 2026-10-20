package com.bigdavi.ecommerce.common.events;

import com.bigdavi.ecommerce.common.messaging.SagaTopology;

import java.time.Instant;
import java.util.UUID;

/** (3b) Evento de compensación: el pago fue rechazado o la pasarela no respondió. */
public record PaymentFailedEvent(UUID eventId, UUID orderId, String reason, Instant occurredAt) implements SagaEvent {

    public PaymentFailedEvent(UUID orderId, String reason) {
        this(UUID.randomUUID(), orderId, reason, Instant.now());
    }

    @Override
    public String routingKey() {
        return SagaTopology.PAYMENT_FAILED;
    }
}

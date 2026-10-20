package com.bigdavi.ecommerce.common.events;

import com.bigdavi.ecommerce.common.messaging.SagaTopology;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** (1) Order Service creó un pedido en PENDING_PAYMENT. */
public record OrderCreatedEvent(UUID eventId, UUID orderId, String customerId, List<OrderLine> lines,
                                Instant occurredAt) implements SagaEvent {

    public OrderCreatedEvent(UUID orderId, String customerId, List<OrderLine> lines) {
        this(UUID.randomUUID(), orderId, customerId, lines, Instant.now());
    }

    @Override
    public String routingKey() {
        return SagaTopology.ORDER_CREATED;
    }
}

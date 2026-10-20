package com.bigdavi.ecommerce.common.events;

import com.bigdavi.ecommerce.common.messaging.SagaTopology;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** (2a) Inventory reservó todo el stock; amount es el total calculado con los precios del catálogo. */
public record StockReservedEvent(UUID eventId, UUID orderId, String customerId, BigDecimal amount,
                                 Instant occurredAt) implements SagaEvent {

    public StockReservedEvent(UUID orderId, String customerId, BigDecimal amount) {
        this(UUID.randomUUID(), orderId, customerId, amount, Instant.now());
    }

    @Override
    public String routingKey() {
        return SagaTopology.STOCK_RESERVED;
    }
}

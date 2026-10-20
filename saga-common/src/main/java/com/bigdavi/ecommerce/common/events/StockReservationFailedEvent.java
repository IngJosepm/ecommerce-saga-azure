package com.bigdavi.ecommerce.common.events;

import com.bigdavi.ecommerce.common.messaging.SagaTopology;

import java.time.Instant;
import java.util.UUID;

/** (2b) No había stock suficiente (o el producto no existe). */
public record StockReservationFailedEvent(UUID eventId, UUID orderId, String reason,
                                          Instant occurredAt) implements SagaEvent {

    public StockReservationFailedEvent(UUID orderId, String reason) {
        this(UUID.randomUUID(), orderId, reason, Instant.now());
    }

    @Override
    public String routingKey() {
        return SagaTopology.STOCK_RESERVATION_FAILED;
    }
}

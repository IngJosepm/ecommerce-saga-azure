package com.bigdavi.ecommerce.common.events;

import com.bigdavi.ecommerce.common.messaging.SagaTopology;

import java.time.Instant;
import java.util.UUID;

/** La reserva superó el tiempo máximo sin recibir respuesta del pago y el stock se liberó. */
public record StockReservationExpiredEvent(UUID eventId, UUID orderId, Instant occurredAt) implements SagaEvent {

    public StockReservationExpiredEvent(UUID orderId) {
        this(UUID.randomUUID(), orderId, Instant.now());
    }

    @Override
    public String routingKey() {
        return SagaTopology.STOCK_RESERVATION_EXPIRED;
    }
}

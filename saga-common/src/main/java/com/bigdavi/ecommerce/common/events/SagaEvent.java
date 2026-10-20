package com.bigdavi.ecommerce.common.events;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.util.UUID;

/**
 * Contrato común de todos los eventos de la Saga.
 * eventId sirve para la idempotencia y orderId correlaciona toda la Saga.
 */
public sealed interface SagaEvent permits OrderCreatedEvent, StockReservedEvent, StockReservationFailedEvent,
        StockReservationExpiredEvent, PaymentProcessedEvent, PaymentFailedEvent {

    UUID eventId();

    UUID orderId();

    Instant occurredAt();

    @JsonIgnore
    String routingKey();
}

package com.bigdavi.ecommerce.common.messaging;

/**
 * Nombres de la topología RabbitMQ.
 * Un único exchange "topic" donde cada evento se publica con su routing key;
 * cada microservicio declara su propia cola y se suscribe solo a lo que le interesa.
 */
public final class SagaTopology {

    public static final String EXCHANGE = "saga.events";
    public static final String DEAD_LETTER_EXCHANGE = "saga.events.dlx";

    public static final String ORDER_CREATED = "order.created";
    public static final String STOCK_RESERVED = "stock.reserved";
    public static final String STOCK_RESERVATION_FAILED = "stock.reservation-failed";
    public static final String STOCK_RESERVATION_EXPIRED = "stock.reservation-expired";
    public static final String PAYMENT_PROCESSED = "payment.processed";
    public static final String PAYMENT_FAILED = "payment.failed";

    private SagaTopology() {
    }
}

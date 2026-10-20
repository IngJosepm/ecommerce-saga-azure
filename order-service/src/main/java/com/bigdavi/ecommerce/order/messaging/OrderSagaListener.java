package com.bigdavi.ecommerce.order.messaging;

import com.bigdavi.ecommerce.common.events.PaymentFailedEvent;
import com.bigdavi.ecommerce.common.events.PaymentProcessedEvent;
import com.bigdavi.ecommerce.common.events.StockReservationExpiredEvent;
import com.bigdavi.ecommerce.common.events.StockReservationFailedEvent;
import com.bigdavi.ecommerce.common.messaging.SagaMessagingConfig;
import com.bigdavi.ecommerce.common.messaging.SagaTopology;
import com.bigdavi.ecommerce.order.application.OrderService;
import com.bigdavi.ecommerce.order.domain.OrderStatus;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/** Escucha el desenlace de la Saga y cierra el pedido. */
@Component
@RabbitListener(queues = OrderSagaListener.QUEUE)
public class OrderSagaListener {

    static final String QUEUE = "order-service.saga";

    private final OrderService orderService;

    public OrderSagaListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @RabbitHandler
    public void on(PaymentProcessedEvent event) {
        orderService.confirm(event.eventId(), event.orderId(), event.amount());
    }

    @RabbitHandler
    public void on(PaymentFailedEvent event) {
        orderService.cancel(event.eventId(), event.orderId(), OrderStatus.CANCELLED_PAYMENT_FAILED, event.reason());
    }

    @RabbitHandler
    public void on(StockReservationFailedEvent event) {
        orderService.cancel(event.eventId(), event.orderId(), OrderStatus.CANCELLED_OUT_OF_STOCK, event.reason());
    }

    @RabbitHandler
    public void on(StockReservationExpiredEvent event) {
        orderService.cancel(event.eventId(), event.orderId(), OrderStatus.CANCELLED_EXPIRED, "RESERVATION_EXPIRED");
    }

    @Configuration
    static class Topology {

        @Bean
        Declarables orderServiceQueue() {
            return SagaMessagingConfig.serviceQueue(QUEUE,
                    SagaTopology.PAYMENT_PROCESSED,
                    SagaTopology.PAYMENT_FAILED,
                    SagaTopology.STOCK_RESERVATION_FAILED,
                    SagaTopology.STOCK_RESERVATION_EXPIRED);
        }
    }
}

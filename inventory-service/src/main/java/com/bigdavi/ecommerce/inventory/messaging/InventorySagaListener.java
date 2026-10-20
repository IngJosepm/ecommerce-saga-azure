package com.bigdavi.ecommerce.inventory.messaging;

import com.bigdavi.ecommerce.common.events.OrderCreatedEvent;
import com.bigdavi.ecommerce.common.events.PaymentFailedEvent;
import com.bigdavi.ecommerce.common.events.PaymentProcessedEvent;
import com.bigdavi.ecommerce.common.messaging.SagaMessagingConfig;
import com.bigdavi.ecommerce.common.messaging.SagaTopology;
import com.bigdavi.ecommerce.inventory.application.InsufficientStockException;
import com.bigdavi.ecommerce.inventory.application.InventoryService;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queues = InventorySagaListener.QUEUE)
public class InventorySagaListener {

    static final String QUEUE = "inventory-service.saga";

    private final InventoryService inventoryService;

    public InventorySagaListener(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @RabbitHandler
    public void on(OrderCreatedEvent event) {
        try {
            inventoryService.reserve(event);
        } catch (InsufficientStockException ex) {
            inventoryService.rejectReservation(event, ex.getMessage());
        }
    }

    @RabbitHandler
    public void on(PaymentProcessedEvent event) {
        inventoryService.confirm(event.eventId(), event.orderId());
    }

    @RabbitHandler
    public void on(PaymentFailedEvent event) {
        inventoryService.compensate(event.eventId(), event.orderId());
    }

    @Configuration
    static class Topology {

        @Bean
        Declarables inventoryServiceQueue() {
            return SagaMessagingConfig.serviceQueue(QUEUE,
                    SagaTopology.ORDER_CREATED,
                    SagaTopology.PAYMENT_PROCESSED,
                    SagaTopology.PAYMENT_FAILED);
        }
    }
}

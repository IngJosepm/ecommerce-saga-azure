package com.bigdavi.ecommerce.payment.messaging;

import com.bigdavi.ecommerce.common.events.StockReservedEvent;
import com.bigdavi.ecommerce.common.messaging.SagaMessagingConfig;
import com.bigdavi.ecommerce.common.messaging.SagaTopology;
import com.bigdavi.ecommerce.payment.application.PaymentService;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queues = PaymentSagaListener.QUEUE)
public class PaymentSagaListener {

    static final String QUEUE = "payment-service.saga";

    private final PaymentService paymentService;

    public PaymentSagaListener(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @RabbitHandler
    public void on(StockReservedEvent event) {
        paymentService.process(event);
    }

    @Configuration
    static class Topology {

        @Bean
        Declarables paymentServiceQueue() {
            return SagaMessagingConfig.serviceQueue(QUEUE, SagaTopology.STOCK_RESERVED);
        }
    }
}

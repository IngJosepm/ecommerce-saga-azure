package com.bigdavi.ecommerce.common.messaging;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableScheduling
public class SagaMessagingConfig {

    private static final TopicExchange EXCHANGE = new TopicExchange(SagaTopology.EXCHANGE, true, false);
    private static final FanoutExchange DEAD_LETTER_EXCHANGE =
            new FanoutExchange(SagaTopology.DEAD_LETTER_EXCHANGE, true, false);

    @Bean
    TopicExchange sagaExchange() {
        return EXCHANGE;
    }

    @Bean
    FanoutExchange sagaDeadLetterExchange() {
        return DEAD_LETTER_EXCHANGE;
    }

    /** Solo acepta deserializar clases del paquete de eventos (evita deserialización arbitraria). */
    @Bean
    MessageConverter sagaMessageConverter() {
        return new JacksonJsonMessageConverter("com.bigdavi.ecommerce.common.events");
    }

    /**
     * Declara la cola durable de un servicio, su cola de mensajes muertos (DLQ)
     * y los bindings a las routing keys que escucha.
     */
    public static Declarables serviceQueue(String queueName, String... routingKeys) {
        Queue queue = QueueBuilder.durable(queueName)
                .deadLetterExchange(SagaTopology.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(queueName)
                .build();
        Queue deadLetterQueue = QueueBuilder.durable(queueName + ".dlq").build();

        List<Declarable> declarables = new ArrayList<>();
        declarables.add(queue);
        declarables.add(deadLetterQueue);
        declarables.add(BindingBuilder.bind(deadLetterQueue).to(DEAD_LETTER_EXCHANGE));
        for (String routingKey : routingKeys) {
            declarables.add(BindingBuilder.bind(queue).to(EXCHANGE).with(routingKey));
        }
        return new Declarables(declarables);
    }
}

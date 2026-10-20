package com.bigdavi.ecommerce.common.test;

import com.bigdavi.ecommerce.common.events.SagaEvent;
import com.bigdavi.ecommerce.common.messaging.SagaTopology;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * "Espía" del bus de eventos para los tests: una cola enlazada a todas las routing keys ("#")
 * que permite publicar eventos como si viniera otro microservicio y esperar los que se emiten.
 */
public class SagaEvents {

    private static final String CAPTURE_QUEUE = "test.capture";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final RabbitTemplate rabbitTemplate;
    private final List<SagaEvent> received = new ArrayList<>();

    SagaEvents(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        RabbitAdmin admin = new RabbitAdmin(rabbitTemplate);
        TopicExchange exchange = new TopicExchange(SagaTopology.EXCHANGE, true, false);
        // RabbitMQ 4 ya no admite colas no durables y no exclusivas: durable + purga al arrancar
        Queue queue = new Queue(CAPTURE_QUEUE, true, false, false);
        admin.declareExchange(exchange);
        admin.declareQueue(queue);
        admin.declareBinding(BindingBuilder.bind(queue).to(exchange).with("#"));
        admin.purgeQueue(CAPTURE_QUEUE, false);
    }

    /** Publica un evento como lo haría otro microservicio. */
    public void publish(SagaEvent event) {
        rabbitTemplate.convertAndSend(SagaTopology.EXCHANGE, event.routingKey(), event);
    }

    /** Espera hasta 15 s a que se publique un evento del tipo indicado para el pedido. */
    public <T extends SagaEvent> T await(Class<T> type, UUID orderId) {
        Instant deadline = Instant.now().plus(TIMEOUT);
        while (Instant.now().isBefore(deadline)) {
            Optional<T> match = find(type, orderId);
            if (match.isPresent()) {
                return match.get();
            }
            Object message = rabbitTemplate.receiveAndConvert(CAPTURE_QUEUE, 500);
            if (message instanceof SagaEvent event) {
                synchronized (received) {
                    received.add(event);
                }
            }
        }
        throw new AssertionError("No se publicó " + type.getSimpleName() + " para el pedido " + orderId);
    }

    /** Recoge lo pendiente durante un momento y devuelve todos los eventos de ese tipo para el pedido. */
    public <T extends SagaEvent> List<T> all(Class<T> type, UUID orderId, Duration wait) {
        Instant deadline = Instant.now().plus(wait);
        while (Instant.now().isBefore(deadline)) {
            Object message = rabbitTemplate.receiveAndConvert(CAPTURE_QUEUE, 200);
            if (message instanceof SagaEvent event) {
                synchronized (received) {
                    received.add(event);
                }
            }
        }
        synchronized (received) {
            return received.stream()
                    .filter(type::isInstance)
                    .map(type::cast)
                    .filter(e -> e.orderId().equals(orderId))
                    .toList();
        }
    }

    private <T extends SagaEvent> Optional<T> find(Class<T> type, UUID orderId) {
        synchronized (received) {
            return received.stream()
                    .filter(type::isInstance)
                    .map(type::cast)
                    .filter(e -> e.orderId().equals(orderId))
                    .findFirst();
        }
    }
}

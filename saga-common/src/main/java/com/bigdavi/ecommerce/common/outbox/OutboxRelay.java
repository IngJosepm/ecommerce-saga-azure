package com.bigdavi.ecommerce.common.outbox;

import com.bigdavi.ecommerce.common.messaging.SagaTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

/**
 * Publica en RabbitMQ los eventos pendientes de la tabla outbox.
 * Garantiza entrega "al menos una vez": si se cae después de publicar y antes de marcar,
 * el evento se reenvía y los consumidores lo descartan gracias a la idempotencia.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final String TYPE_ID_HEADER = "__TypeId__";

    private final OutboxRepository repository;
    private final RabbitTemplate rabbitTemplate;

    public OutboxRelay(OutboxRepository repository, RabbitTemplate rabbitTemplate) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelayString = "${saga.outbox.poll-interval:500ms}")
    @Transactional
    public void publishPending() {
        for (OutboxEvent event : repository.lockNextBatch()) {
            Message message = MessageBuilder.withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setContentEncoding(StandardCharsets.UTF_8.name())
                    .setMessageId(event.getId().toString())
                    .setHeader(TYPE_ID_HEADER, event.getEventType())
                    .build();
            rabbitTemplate.send(SagaTopology.EXCHANGE, event.getRoutingKey(), message);
            event.markPublished();
            log.info("Evento publicado {} [{}]", event.getRoutingKey(), event.getId());
        }
    }
}

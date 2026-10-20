package com.bigdavi.ecommerce.common.outbox;

import com.bigdavi.ecommerce.common.events.SagaEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Guarda el evento en la tabla outbox dentro de la MISMA transacción que el cambio de negocio.
 * Así nunca queda un pedido guardado sin su evento (ni un evento sin su pedido).
 */
@Component
public class OutboxWriter {

    private final OutboxRepository repository;
    private final JsonMapper jsonMapper;

    public OutboxWriter(OutboxRepository repository, JsonMapper jsonMapper) {
        this.repository = repository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void write(SagaEvent event) {
        repository.save(new OutboxEvent(
                event.eventId(),
                event.orderId(),
                event.getClass().getName(),
                event.routingKey(),
                jsonMapper.writeValueAsString(event)));
    }
}

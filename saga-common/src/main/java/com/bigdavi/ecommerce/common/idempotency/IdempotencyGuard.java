package com.bigdavi.ecommerce.common.idempotency;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * RabbitMQ entrega "al menos una vez", así que un evento puede llegar duplicado.
 * Se registra el eventId en la misma transacción que el procesamiento:
 * si ya existe, el evento se ignora.
 */
@Component
public class IdempotencyGuard {

    private final ProcessedEventRepository repository;

    public IdempotencyGuard(ProcessedEventRepository repository) {
        this.repository = repository;
    }

    public boolean alreadyProcessed(UUID eventId) {
        return repository.existsById(eventId);
    }

    /** @return true si es la primera vez que se ve el evento. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean markProcessed(UUID eventId) {
        if (repository.existsById(eventId)) {
            return false;
        }
        repository.saveAndFlush(new ProcessedEvent(eventId));
        return true;
    }
}

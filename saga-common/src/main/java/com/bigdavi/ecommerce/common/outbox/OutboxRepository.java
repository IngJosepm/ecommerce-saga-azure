package com.bigdavi.ecommerce.common.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * FOR UPDATE SKIP LOCKED: si hay varias réplicas del servicio,
     * cada una toma un lote distinto y no publican el mismo evento a la vez.
     */
    @Query(value = """
            select * from outbox_events
            where published_at is null
            order by created_at
            limit 50
            for update skip locked
            """, nativeQuery = true)
    List<OutboxEvent> lockNextBatch();
}

-- Tablas comunes a todos los microservicios (cada uno las crea en SU propia base de datos)

create table outbox_events (
    id           uuid primary key,
    aggregate_id uuid         not null,
    event_type   varchar(255) not null,
    routing_key  varchar(100) not null,
    payload      text         not null,
    created_at   timestamptz  not null,
    published_at timestamptz
);

create index idx_outbox_pending on outbox_events (created_at) where published_at is null;

create table processed_events (
    event_id     uuid primary key,
    processed_at timestamptz not null
);

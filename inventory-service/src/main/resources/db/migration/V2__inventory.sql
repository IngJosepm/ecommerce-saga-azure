create table products (
    id        varchar(50) primary key,
    name      varchar(200)   not null,
    price     numeric(12, 2) not null,
    available int            not null check (available >= 0)
);

create table reservations (
    id         bigserial primary key,
    order_id   uuid        not null,
    product_id varchar(50) not null references products (id),
    quantity   int         not null check (quantity > 0),
    status     varchar(20) not null,
    created_at timestamptz not null
);

create index idx_reservations_order on reservations (order_id);
create index idx_reservations_pending on reservations (created_at) where status = 'RESERVED';

-- Catálogo de ejemplo. LAPTOP-01 supera el límite de la pasarela simulada (pago rechazado)
-- y LAST-UNIT tiene 1 unidad para probar la concurrencia.
insert into products (id, name, price, available) values
    ('KB-01',     'Teclado mecánico',       89.90, 50),
    ('MOUSE-01',  'Mouse inalámbrico',      35.50, 100),
    ('MONITOR-01','Monitor 27" 4K',        420.00, 10),
    ('LAPTOP-01', 'Laptop 16" Pro',       1899.00, 5),
    ('LAST-UNIT', 'Edición limitada',       59.00, 1);

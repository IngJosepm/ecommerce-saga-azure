create table orders (
    id             uuid primary key,
    customer_id    varchar(100) not null,
    status         varchar(40)  not null,
    total_amount   numeric(12, 2),
    failure_reason varchar(255),
    created_at     timestamptz  not null,
    updated_at     timestamptz,
    version        bigint       not null default 0
);

create index idx_orders_customer on orders (customer_id, created_at desc);

create table order_items (
    id         bigserial primary key,
    order_id   uuid         not null references orders (id),
    product_id varchar(50)  not null,
    quantity   int          not null check (quantity > 0)
);

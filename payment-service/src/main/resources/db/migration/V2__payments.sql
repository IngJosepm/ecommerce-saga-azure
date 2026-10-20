create table payments (
    id                     uuid primary key,
    order_id               uuid           not null unique,
    amount                 numeric(12, 2) not null,
    status                 varchar(20)    not null,
    gateway_transaction_id varchar(100),
    failure_reason         varchar(255),
    created_at             timestamptz    not null
);

create table integration_inbox (
    message_id uuid not null,
    idempotency_key varchar(300) not null,
    event_type varchar(100) not null,
    aggregate_type varchar(100) not null,
    aggregate_id varchar(150) not null,
    payload text not null,
    received_at timestamptz not null,
    last_received_at timestamptz not null,
    duplicate_count integer not null default 0,
    constraint integration_inbox_pkey primary key (message_id),
    constraint uk_integration_inbox_idempotency unique (idempotency_key),
    constraint chk_integration_inbox_duplicate_count check (duplicate_count >= 0)
);

create index idx_integration_inbox_aggregate
    on integration_inbox (aggregate_type, aggregate_id);
create index idx_integration_inbox_received_at
    on integration_inbox (received_at);

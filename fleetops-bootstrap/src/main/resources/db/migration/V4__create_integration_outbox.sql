create table erp_shipment_receipts (
    message_id uuid not null,
    idempotency_key varchar(300) not null,
    source_system varchar(50) not null,
    source_message_id varchar(150) not null,
    shipment_reference varchar(100) not null,
    operation varchar(20) not null,
    schema_version integer not null,
    occurred_at timestamptz not null,
    correlation_id varchar(150),
    received_at timestamptz not null,
    constraint erp_shipment_receipts_pkey primary key (message_id),
    constraint uk_erp_shipment_receipts_idempotency unique (idempotency_key),
    constraint chk_erp_shipment_receipts_operation check (operation in ('UPSERT', 'CANCEL')),
    constraint chk_erp_shipment_receipts_schema_version check (schema_version >= 1)
);

create index idx_erp_shipment_receipts_shipment_reference
    on erp_shipment_receipts (shipment_reference);

create table integration_outbox (
    id uuid not null,
    idempotency_key varchar(300) not null,
    event_type varchar(100) not null,
    aggregate_type varchar(100) not null,
    aggregate_id varchar(150) not null,
    payload text not null,
    status varchar(20) not null,
    attempts integer not null default 0,
    created_at timestamptz not null,
    published_at timestamptz,
    last_error varchar(1000),
    constraint integration_outbox_pkey primary key (id),
    constraint uk_integration_outbox_idempotency unique (idempotency_key),
    constraint chk_integration_outbox_status check (status in ('PENDING', 'PUBLISHED', 'FAILED')),
    constraint chk_integration_outbox_attempts check (attempts >= 0)
);

create index idx_integration_outbox_delivery
    on integration_outbox (status, created_at);
create index idx_integration_outbox_aggregate
    on integration_outbox (aggregate_type, aggregate_id);

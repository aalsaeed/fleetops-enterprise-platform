alter table integration_outbox
    add column next_attempt_at timestamptz;

drop index if exists idx_integration_outbox_delivery;

create index idx_integration_outbox_delivery
    on integration_outbox (status, next_attempt_at, created_at);

create index idx_integration_outbox_retry_due
    on integration_outbox (next_attempt_at, created_at)
    where status = 'PENDING';

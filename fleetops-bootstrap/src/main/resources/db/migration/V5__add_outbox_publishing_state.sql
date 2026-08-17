alter table integration_outbox
    add column claimed_at timestamptz;

alter table integration_outbox
    drop constraint chk_integration_outbox_status;

alter table integration_outbox
    add constraint chk_integration_outbox_status
        check (status in ('PENDING', 'PUBLISHING', 'PUBLISHED', 'FAILED'));

create index idx_integration_outbox_publishing_claim
    on integration_outbox (claimed_at)
    where status = 'PUBLISHING';

alter table integration_inbox
    add column processing_status varchar(20) not null default 'PENDING',
    add column processing_attempts integer not null default 0,
    add column processing_claimed_at timestamptz,
    add column processed_at timestamptz,
    add column processing_last_error varchar(1000);

alter table integration_inbox
    add constraint chk_integration_inbox_processing_status
        check (processing_status in ('PENDING', 'PROCESSING', 'PROCESSED', 'FAILED')),
    add constraint chk_integration_inbox_processing_attempts
        check (processing_attempts >= 0);

create index idx_integration_inbox_processing_pending
    on integration_inbox (received_at)
    where processing_status = 'PENDING';

create index idx_integration_inbox_processing_claim
    on integration_inbox (processing_claimed_at)
    where processing_status = 'PROCESSING';

create index idx_integration_inbox_processing_failed
    on integration_inbox (last_received_at)
    where processing_status = 'FAILED';

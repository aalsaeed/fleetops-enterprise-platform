create table audit_events (
    id uuid primary key,
    occurred_at timestamptz not null,
    actor_subject varchar(255) not null,
    actor_display_name varchar(255),
    action varchar(120) not null,
    resource_type varchar(100) not null,
    resource_id varchar(255),
    outcome varchar(20) not null,
    correlation_id varchar(128) not null,
    constraint chk_audit_events_outcome check (outcome in ('SUCCESS', 'FAILURE'))
);

create table audit_event_authorities (
    audit_event_id uuid not null,
    authority varchar(100) not null,
    primary key (audit_event_id, authority),
    constraint fk_audit_event_authorities_event
        foreign key (audit_event_id) references audit_events(id) on delete restrict
);

create table audit_event_metadata (
    audit_event_id uuid not null,
    metadata_key varchar(120) not null,
    metadata_value varchar(1000) not null,
    primary key (audit_event_id, metadata_key),
    constraint fk_audit_event_metadata_event
        foreign key (audit_event_id) references audit_events(id) on delete restrict
);

create index idx_audit_events_occurred_at
    on audit_events (occurred_at desc);

create index idx_audit_events_actor_occurred_at
    on audit_events (actor_subject, occurred_at desc);

create index idx_audit_events_action_occurred_at
    on audit_events (action, occurred_at desc);

create index idx_audit_events_resource
    on audit_events (resource_type, resource_id, occurred_at desc);

create index idx_audit_events_correlation
    on audit_events (correlation_id);

create or replace function reject_audit_mutation()
returns trigger
language plpgsql
as $$
begin
    raise exception 'audit trail is append-only';
end;
$$;

create trigger trg_audit_events_immutable
before update or delete on audit_events
for each row execute function reject_audit_mutation();

create trigger trg_audit_event_authorities_immutable
before update or delete on audit_event_authorities
for each row execute function reject_audit_mutation();

create trigger trg_audit_event_metadata_immutable
before update or delete on audit_event_metadata
for each row execute function reject_audit_mutation();

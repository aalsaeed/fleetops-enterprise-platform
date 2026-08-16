create table vehicles (
    id uuid not null,
    external_reference varchar(100) not null,
    description varchar(200) not null,
    type varchar(20) not null,
    serial_number varchar(100),
    status varchar(20) not null,
    constraint vehicles_pkey primary key (id),
    constraint uk_vehicles_external_reference unique (external_reference),
    constraint chk_vehicles_type check (type in ('TRACTOR', 'TRAILER', 'BULKER')),
    constraint chk_vehicles_status check (status in ('ACTIVE', 'INACTIVE', 'MAINTENANCE', 'RETIRED'))
);

create index idx_vehicles_status on vehicles (status);
create index idx_vehicles_type on vehicles (type);

create table trips (
    id uuid not null,
    external_reference varchar(100) not null,
    driver_id uuid,
    primary_vehicle_id uuid,
    attachment_vehicle_id uuid,
    status varchar(20) not null,
    constraint trips_pkey primary key (id),
    constraint uk_trips_external_reference unique (external_reference),
    constraint chk_trips_status check (status in ('PLANNED', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    constraint chk_trips_distinct_vehicles check (
        attachment_vehicle_id is null or primary_vehicle_id is null or attachment_vehicle_id <> primary_vehicle_id
    ),
    constraint chk_trips_assignment_state check (
        (status = 'PLANNED' and driver_id is null and primary_vehicle_id is null and attachment_vehicle_id is null)
        or (status in ('ASSIGNED', 'IN_PROGRESS', 'COMPLETED') and driver_id is not null and primary_vehicle_id is not null)
        or (status = 'CANCELLED' and (
            (driver_id is null and primary_vehicle_id is null and attachment_vehicle_id is null)
            or (driver_id is not null and primary_vehicle_id is not null)
        ))
    )
);

create index idx_trips_status on trips (status);
create index idx_trips_driver_id on trips (driver_id);
create index idx_trips_primary_vehicle_id on trips (primary_vehicle_id);
create index idx_trips_attachment_vehicle_id on trips (attachment_vehicle_id);

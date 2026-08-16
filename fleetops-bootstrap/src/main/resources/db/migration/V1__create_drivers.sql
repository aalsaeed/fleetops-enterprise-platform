CREATE TABLE drivers (
    id UUID PRIMARY KEY,
    external_reference VARCHAR(100) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(16) NOT NULL,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT uk_drivers_external_reference UNIQUE (external_reference),
    CONSTRAINT chk_drivers_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'))
);

CREATE INDEX idx_drivers_status ON drivers(status);

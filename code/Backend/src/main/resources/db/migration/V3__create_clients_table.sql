-- M0397_V001: Create clients table
-- Author: Thirawat (673380039-7)
-- Description: Clients owned by freelancers with archive status and optimistic locking

CREATE TABLE clients (
    id UUID PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    company_name VARCHAR(200),
    email VARCHAR(254),
    phone VARCHAR(30),
    address TEXT,
    tax_id VARCHAR(30),
    notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_clients_owner
        FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT ck_clients_status
        CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT uk_clients_id_owner
        UNIQUE (id, owner_id)
);

CREATE INDEX idx_clients_owner_status ON clients(owner_id, status);
CREATE INDEX idx_clients_owner_name ON clients(owner_id, name);
CREATE INDEX idx_clients_email ON clients(email);

COMMENT ON TABLE clients IS 'Clients managed by freelancers';
COMMENT ON COLUMN clients.owner_id IS 'Owner used to isolate each freelancer data';
COMMENT ON COLUMN clients.status IS 'Client lifecycle status: ACTIVE or ARCHIVED';
COMMENT ON COLUMN clients.version IS 'Optimistic-lock version used by JPA';

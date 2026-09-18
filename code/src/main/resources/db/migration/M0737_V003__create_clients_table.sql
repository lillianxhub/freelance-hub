-- V003: Create clients table
-- Author: Petpinyo (673380073-7)
-- Description: Clients table for managing freelance clients

CREATE TABLE clients (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(20),
    company VARCHAR(255),
    address TEXT,
    city VARCHAR(100),
    country VARCHAR(100),
    postal_code VARCHAR(20),
    notes TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_client_owner FOREIGN KEY (owner_id)
        REFERENCES users(id) ON DELETE CASCADE
);

-- Index for owner_id (important for owner isolation)
CREATE INDEX idx_clients_owner_id ON clients(owner_id);

-- Index for active clients
CREATE INDEX idx_clients_is_active ON clients(is_active);

-- Index for email lookup
CREATE INDEX idx_clients_email ON clients(email);

-- Comments
COMMENT ON TABLE clients IS 'Clients managed by freelancers';
COMMENT ON COLUMN clients.owner_id IS 'Foreign key to users table - owner isolation';
COMMENT ON COLUMN clients.name IS 'Client full name or company name';
COMMENT ON COLUMN clients.is_active IS 'Active status for filtering';

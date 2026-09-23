-- V001: Create users table
-- Author: Petpinyo (673380073-7)
-- Description: Users table for authentication

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for faster email lookup
CREATE INDEX idx_users_email ON users(email);

-- Index for status filtering
CREATE INDEX idx_users_status ON users(status);

-- Comments
COMMENT ON TABLE users IS 'Users table for authentication and authorization';
COMMENT ON COLUMN users.id IS 'Primary key (UUID)';
COMMENT ON COLUMN users.email IS 'User email, must be unique';
COMMENT ON COLUMN users.password_hash IS 'BCrypt hashed password';
COMMENT ON COLUMN users.role IS 'User role: USER, ADMIN';
COMMENT ON COLUMN users.status IS 'User status: ACTIVE, SUSPENDED, PENDING';
COMMENT ON COLUMN users.enabled IS 'Soft delete flag';
COMMENT ON COLUMN users.version IS 'Optimistic locking version';

-- M0737_V001: Create users table
-- Author: Petpinyo (673380073-7)
-- Description: Users table for authentication and authorization

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for faster email lookup
CREATE INDEX idx_users_email ON users(email);

-- Comments
COMMENT ON TABLE users IS 'Users table for authentication and authorization';
COMMENT ON COLUMN users.id IS 'Primary key (UUID)';
COMMENT ON COLUMN users.email IS 'User email, must be unique';
COMMENT ON COLUMN users.password_hash IS 'BCrypt hashed password';
COMMENT ON COLUMN users.role IS 'User role: USER, ADMIN';
COMMENT ON COLUMN users.status IS 'User status: ACTIVE, SUSPENDED, PENDING';
COMMENT ON COLUMN users.enabled IS 'Account enabled flag';
COMMENT ON COLUMN users.version IS 'Optimistic locking version';
COMMENT ON COLUMN users.created_at IS 'Timestamp with timezone when created';
COMMENT ON COLUMN users.updated_at IS 'Timestamp with timezone when last updated';

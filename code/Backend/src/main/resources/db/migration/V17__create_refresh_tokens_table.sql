-- V17: Create refresh tokens table
-- Author: Petpinyo (673380073-7)
-- Description: Store only hashes of rotating, seven-day refresh tokens

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    family_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_refresh_tokens_expiry CHECK (expires_at > created_at)
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens(family_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

COMMENT ON TABLE refresh_tokens IS 'Rotating refresh tokens; plaintext token values are never stored';
COMMENT ON COLUMN refresh_tokens.family_id IS 'One login family, retained across rotations';
COMMENT ON COLUMN refresh_tokens.token_hash IS 'Lowercase SHA-256 hex digest of a random 256-bit token';
COMMENT ON COLUMN refresh_tokens.expires_at IS 'Fixed expiry, seven days from initial login';

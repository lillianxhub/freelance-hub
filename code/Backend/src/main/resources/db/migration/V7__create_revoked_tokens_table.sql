-- V6: Create revoked_tokens table
-- Description: Persistent server-side JWT revocation by token identifier

CREATE TABLE revoked_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    jti VARCHAR(36) NOT NULL,
    user_id UUID NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT uk_revoked_tokens_jti UNIQUE (jti),
    CONSTRAINT fk_revoked_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- The unique constraint on jti provides its lookup index.
CREATE INDEX idx_revoked_tokens_user_id ON revoked_tokens(user_id);
CREATE INDEX idx_revoked_tokens_expires_at ON revoked_tokens(expires_at);

COMMENT ON TABLE revoked_tokens IS 'JWT identifiers revoked before their expiration';
COMMENT ON COLUMN revoked_tokens.jti IS 'JWT ID only; the raw token is never stored';

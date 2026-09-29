-- V12: Align revoked token audit fields with the current data dictionary
-- Author: Petpinyo (673380073-7)

ALTER TABLE revoked_tokens
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_revoked_tokens_is_active ON revoked_tokens(is_active);

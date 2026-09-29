-- V19: Normalize user emails and enforce case-insensitive uniqueness
-- Author: Freelance Hub Team
-- Description: Abort safely if existing accounts would collide; never merge accounts automatically.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM users
        GROUP BY lower(btrim(email))
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot normalize users.email: duplicate canonical emails exist';
    END IF;
END $$;

UPDATE users
SET email = lower(btrim(email)), updated_at = CURRENT_TIMESTAMP
WHERE email <> lower(btrim(email));

CREATE UNIQUE INDEX idx_users_email_canonical_unique ON users (lower(btrim(email)));
COMMENT ON INDEX idx_users_email_canonical_unique IS 'Prevents accounts differing only by email case or outer spaces';

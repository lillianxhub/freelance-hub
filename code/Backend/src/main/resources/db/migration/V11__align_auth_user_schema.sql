-- V11: Align authentication and user profile schema with the current data dictionary
-- Author: Petpinyo (673380073-7)
-- Description: Use is_active/deleted_at and store user profile address fields directly

-- users: replace the legacy status/enabled pair with the dictionary's is_active flag.
ALTER TABLE users ADD COLUMN is_active BOOLEAN;
UPDATE users
SET is_active = (status = 'ACTIVE' AND enabled = TRUE);
ALTER TABLE users ALTER COLUMN is_active SET DEFAULT TRUE;
ALTER TABLE users ALTER COLUMN is_active SET NOT NULL;
ALTER TABLE users ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;

DROP INDEX IF EXISTS idx_users_status;
ALTER TABLE users DROP COLUMN status;
ALTER TABLE users DROP COLUMN enabled;

-- user_profiles: move the user address out of the shared address relation.
ALTER TABLE user_profiles
    ADD COLUMN address TEXT,
    ADD COLUMN subdistrict VARCHAR(100),
    ADD COLUMN district VARCHAR(100),
    ADD COLUMN province VARCHAR(100),
    ADD COLUMN postal_code VARCHAR(20),
    ADD COLUMN is_active BOOLEAN,
    ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;

UPDATE user_profiles profile
SET address = addresses.address,
    subdistrict = addresses.subdistrict,
    district = addresses.district,
    province = addresses.province,
    postal_code = addresses.postal_code
FROM addresses
WHERE profile.address_id = addresses.id;

ALTER TABLE user_profiles ALTER COLUMN is_active SET DEFAULT TRUE;
UPDATE user_profiles SET is_active = TRUE WHERE is_active IS NULL;
ALTER TABLE user_profiles ALTER COLUMN is_active SET NOT NULL;

ALTER TABLE user_profiles DROP CONSTRAINT IF EXISTS fk_user_profiles_address;
DROP INDEX IF EXISTS uk_user_profiles_address_id;
ALTER TABLE user_profiles DROP COLUMN address_id;
ALTER TABLE user_profiles DROP COLUMN timezone;

-- Use timestamptz for auth/profile audit timestamps as defined by the dictionary.
ALTER TABLE users
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE user_profiles
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE USING updated_at AT TIME ZONE 'UTC';

CREATE INDEX idx_users_is_active ON users(is_active);
CREATE INDEX idx_user_profiles_province ON user_profiles(province);
CREATE INDEX idx_user_profiles_postal_code ON user_profiles(postal_code);

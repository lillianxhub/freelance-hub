-- M0737_V002: Create user_profiles table
-- Author: Petpinyo (673380073-7)
-- Description: User profiles table with one-to-one relationship to users (user_id as PK and FK)

CREATE TABLE user_profiles (
    user_id UUID PRIMARY KEY,
    display_name VARCHAR(255),
    phone VARCHAR(20),
    address TEXT,
    avatar_url VARCHAR(500),
    timezone VARCHAR(50) DEFAULT 'UTC',
    date_format VARCHAR(20) DEFAULT 'YYYY-MM-DD',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_profile_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

-- Comments
COMMENT ON TABLE user_profiles IS 'User profile information, one-to-one with users';
COMMENT ON COLUMN user_profiles.user_id IS 'Primary key and foreign key to users table';
COMMENT ON COLUMN user_profiles.display_name IS 'Display name shown in UI';
COMMENT ON COLUMN user_profiles.phone IS 'Contact phone number';
COMMENT ON COLUMN user_profiles.address IS 'Full address';
COMMENT ON COLUMN user_profiles.avatar_url IS 'Profile avatar image URL';
COMMENT ON COLUMN user_profiles.timezone IS 'User preferred timezone (default UTC)';
COMMENT ON COLUMN user_profiles.date_format IS 'User preferred date format';
COMMENT ON COLUMN user_profiles.version IS 'Optimistic locking version';
COMMENT ON COLUMN user_profiles.created_at IS 'Timestamp with timezone when created';
COMMENT ON COLUMN user_profiles.updated_at IS 'Timestamp with timezone when last updated';

-- V002: Create user_profiles table
-- Author: Petpinyo (673380073-7)
-- Description: User profiles table with one-to-one relationship to users

CREATE TABLE user_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(20),
    address TEXT,
    city VARCHAR(100),
    country VARCHAR(100),
    postal_code VARCHAR(20),
    timezone VARCHAR(50) DEFAULT 'UTC',
    date_format VARCHAR(20) DEFAULT 'YYYY-MM-DD',
    profile_image_url TEXT,
    bio TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_profile_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

-- Index for faster user_id lookup
CREATE INDEX idx_user_profiles_user_id ON user_profiles(user_id);

-- Comments
COMMENT ON TABLE user_profiles IS 'User profile information, one-to-one with users';
COMMENT ON COLUMN user_profiles.user_id IS 'Foreign key to users table, unique';
COMMENT ON COLUMN user_profiles.display_name IS 'Display name shown in UI';
COMMENT ON COLUMN user_profiles.timezone IS 'User preferred timezone';
COMMENT ON COLUMN user_profiles.date_format IS 'User preferred date format';

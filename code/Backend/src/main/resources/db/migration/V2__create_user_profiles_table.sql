-- V002: Create user_profiles table
-- Author: Petpinyo (673380073-7)
-- Description: User profiles table with one-to-one relationship to users

CREATE TABLE user_profiles (
    user_id BIGINT PRIMARY KEY,
    display_name VARCHAR(255),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(20),
    address TEXT,
    city VARCHAR(100),
    country VARCHAR(100),
    postal_code VARCHAR(20),
    avatar_url VARCHAR(500),
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
COMMENT ON COLUMN user_profiles.first_name IS 'User first name';
COMMENT ON COLUMN user_profiles.last_name IS 'User last name';
COMMENT ON COLUMN user_profiles.phone IS 'Contact phone number';
COMMENT ON COLUMN user_profiles.address IS 'Full address';
COMMENT ON COLUMN user_profiles.city IS 'City';
COMMENT ON COLUMN user_profiles.country IS 'Country';
COMMENT ON COLUMN user_profiles.postal_code IS 'Postal/ZIP code';
COMMENT ON COLUMN user_profiles.avatar_url IS 'Profile avatar image URL';
COMMENT ON COLUMN user_profiles.timezone IS 'User preferred timezone (default UTC)';
COMMENT ON COLUMN user_profiles.date_format IS 'User preferred date format';

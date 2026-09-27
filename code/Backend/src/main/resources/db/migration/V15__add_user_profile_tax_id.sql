-- V15: Add tax identifier to user profiles
-- Author: Petpinyo (673380073-7)
-- Description: Store the optional tax identifier for the authenticated user's profile

ALTER TABLE user_profiles
    ADD COLUMN tax_id VARCHAR(30);

COMMENT ON COLUMN user_profiles.tax_id IS 'Optional tax identifier for the user profile';

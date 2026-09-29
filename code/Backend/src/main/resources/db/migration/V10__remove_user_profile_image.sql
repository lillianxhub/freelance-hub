-- V10: Remove user profile image fields
-- Author: Petpinyo (673380073-7)
-- Description: Profile image upload and URL storage are outside the MVP scope

ALTER TABLE user_profiles
    DROP COLUMN avatar_url,
    DROP COLUMN profile_image_url;

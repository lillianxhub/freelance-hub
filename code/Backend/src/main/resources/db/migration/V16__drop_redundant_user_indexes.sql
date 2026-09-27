-- V16: Drop redundant user indexes
-- Author: Petpinyo (673380073-7)
-- Description: Remove indexes already provided by the email UNIQUE constraint and user_profiles primary key

DROP INDEX IF EXISTS idx_users_email;
DROP INDEX IF EXISTS idx_user_profiles_user_id;

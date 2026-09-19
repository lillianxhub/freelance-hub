-- Add version columns for optimistic locking to existing tables

-- Add version to users table
ALTER TABLE freelance_hub.users
ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Add version to user_profiles table
ALTER TABLE freelance_hub.user_profiles
ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Add version to clients table
ALTER TABLE freelance_hub.clients
ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Add version to projects table
ALTER TABLE freelance_hub.projects
ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Add version to tasks table
ALTER TABLE freelance_hub.tasks
ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

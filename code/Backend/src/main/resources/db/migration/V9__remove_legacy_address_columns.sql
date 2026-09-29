-- V9: Remove legacy address columns after normalization
-- Author: Petpinyo (673380073-7)
-- Description: Keep address data in addresses and remove the old denormalized columns

ALTER TABLE user_profiles
    DROP COLUMN address,
    DROP COLUMN city,
    DROP COLUMN country,
    DROP COLUMN postal_code;

ALTER TABLE clients
    DROP COLUMN address;

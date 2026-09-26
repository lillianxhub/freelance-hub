-- V8: Create normalized addresses and link user profiles and clients
-- Author: Petpinyo (673380073-7)
-- Description: Store shared flat address fields in a dedicated table and backfill existing records

CREATE TABLE addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    address TEXT,
    subdistrict VARCHAR(100),
    district VARCHAR(100),
    province VARCHAR(100),
    postal_code VARCHAR(20),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_addresses_province ON addresses(province);
CREATE INDEX idx_addresses_postal_code ON addresses(postal_code);

ALTER TABLE user_profiles ADD COLUMN address_id UUID;
ALTER TABLE clients ADD COLUMN address_id UUID;

ALTER TABLE user_profiles
    ADD CONSTRAINT fk_user_profiles_address
    FOREIGN KEY (address_id) REFERENCES addresses(id) ON DELETE SET NULL;

ALTER TABLE clients
    ADD CONSTRAINT fk_clients_address
    FOREIGN KEY (address_id) REFERENCES addresses(id) ON DELETE SET NULL;

CREATE UNIQUE INDEX uk_user_profiles_address_id ON user_profiles(address_id)
    WHERE address_id IS NOT NULL;
CREATE UNIQUE INDEX uk_clients_address_id ON clients(address_id)
    WHERE address_id IS NOT NULL;

-- Keep a stable generated id per legacy row while migrating the old flat columns.
-- Legacy user_profiles.city is carried into addresses.province; subdistrict and district
-- remain NULL because the old schema did not contain enough information to infer them.
-- Legacy country is not part of the new address contract and is removed by V9.
CREATE TEMP TABLE tmp_user_profile_addresses (
    user_id UUID PRIMARY KEY,
    address_id UUID NOT NULL
) ON COMMIT DROP;

INSERT INTO tmp_user_profile_addresses (user_id, address_id)
SELECT user_id, gen_random_uuid()
FROM user_profiles
WHERE address IS NOT NULL OR city IS NOT NULL OR country IS NOT NULL OR postal_code IS NOT NULL;

INSERT INTO addresses (id, address, province, postal_code)
SELECT t.address_id, p.address, p.city, p.postal_code
FROM tmp_user_profile_addresses t
JOIN user_profiles p ON p.user_id = t.user_id;

UPDATE user_profiles p
SET address_id = t.address_id
FROM tmp_user_profile_addresses t
WHERE p.user_id = t.user_id;

CREATE TEMP TABLE tmp_client_addresses (
    client_id UUID PRIMARY KEY,
    address_id UUID NOT NULL
) ON COMMIT DROP;

INSERT INTO tmp_client_addresses (client_id, address_id)
SELECT id, gen_random_uuid()
FROM clients
WHERE address IS NOT NULL;

INSERT INTO addresses (id, address)
SELECT t.address_id, c.address
FROM tmp_client_addresses t
JOIN clients c ON c.id = t.client_id;

UPDATE clients c
SET address_id = t.address_id
FROM tmp_client_addresses t
WHERE c.id = t.client_id;

COMMENT ON TABLE addresses IS 'Normalized address values shared by user profiles and clients';
COMMENT ON COLUMN addresses.address IS 'House number, street, and other free-form address details';
COMMENT ON COLUMN addresses.subdistrict IS 'Subdistrict or khwaeng';
COMMENT ON COLUMN addresses.district IS 'District or khet';
COMMENT ON COLUMN addresses.province IS 'Province';
COMMENT ON COLUMN addresses.postal_code IS 'Postal code exposed as postalCode in the API';
COMMENT ON COLUMN user_profiles.address_id IS 'Normalized address referenced by the user profile';
COMMENT ON COLUMN clients.address_id IS 'Normalized address referenced by the client';

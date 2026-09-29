-- V13: Align client address storage with the current data dictionary
-- Description: Move client address fields into clients and remove the legacy addresses table

ALTER TABLE clients
    ADD COLUMN address TEXT,
    ADD COLUMN subdistrict VARCHAR(100),
    ADD COLUMN district VARCHAR(100),
    ADD COLUMN province VARCHAR(100),
    ADD COLUMN postal_code VARCHAR(20);

UPDATE clients client
SET address = addresses.address,
    subdistrict = addresses.subdistrict,
    district = addresses.district,
    province = addresses.province,
    postal_code = addresses.postal_code
FROM addresses
WHERE client.address_id = addresses.id;

ALTER TABLE clients DROP CONSTRAINT IF EXISTS fk_clients_address;
DROP INDEX IF EXISTS uk_clients_address_id;
ALTER TABLE clients DROP COLUMN address_id;

CREATE INDEX idx_clients_province ON clients(province);
CREATE INDEX idx_clients_postal_code ON clients(postal_code);

DROP TABLE addresses;

-- Date-format preferences are outside the MVP scope. Keep V2 immutable for
-- databases that have already applied it, and remove the column here instead.
ALTER TABLE user_profiles DROP COLUMN date_format;

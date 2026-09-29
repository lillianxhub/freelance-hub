-- V14: Align the remaining MVP tables with the current data dictionary
-- Description: Apply soft-delete fields, ownership constraints, and time-entry units

-- clients: replace the legacy lifecycle status with the standard soft-delete flag.
ALTER TABLE clients
    ADD COLUMN is_active BOOLEAN,
    ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;

UPDATE clients
SET is_active = (status = 'ACTIVE');

ALTER TABLE clients
    ALTER COLUMN is_active SET DEFAULT TRUE,
    ALTER COLUMN is_active SET NOT NULL;

ALTER TABLE clients DROP CONSTRAINT IF EXISTS ck_clients_status;
DROP INDEX IF EXISTS idx_clients_owner_status;
ALTER TABLE clients DROP COLUMN status;

CREATE INDEX idx_clients_owner_is_active ON clients(owner_id, is_active);
CREATE INDEX IF NOT EXISTS idx_clients_owner_name ON clients(owner_id, name);
CREATE INDEX IF NOT EXISTS idx_clients_email ON clients(email);

-- projects: add standard soft-delete fields and enforce owner isolation for clients.
ALTER TABLE projects
    ADD COLUMN is_active BOOLEAN,
    ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;

UPDATE projects SET is_active = TRUE;

ALTER TABLE projects
    ALTER COLUMN is_active SET DEFAULT TRUE,
    ALTER COLUMN is_active SET NOT NULL;

ALTER TABLE projects DROP CONSTRAINT IF EXISTS fk_projects_client;
ALTER TABLE projects
    ADD CONSTRAINT fk_projects_client_owner
    FOREIGN KEY (client_id, owner_id)
    REFERENCES clients(id, owner_id);

CREATE INDEX idx_projects_owner_is_active ON projects(owner_id, is_active);

-- tasks: add standard soft-delete fields and the active-record lookup index.
ALTER TABLE tasks
    ADD COLUMN is_active BOOLEAN,
    ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;

UPDATE tasks SET is_active = TRUE;

ALTER TABLE tasks
    ALTER COLUMN is_active SET DEFAULT TRUE,
    ALTER COLUMN is_active SET NOT NULL;

CREATE INDEX idx_tasks_project_is_active ON tasks(project_id, is_active);

-- time_entries: store duration in seconds as required by the dictionary.
ALTER TABLE time_entries
    DROP CONSTRAINT IF EXISTS chk_time_entries_completion;

ALTER TABLE time_entries
    ADD COLUMN duration_seconds BIGINT;

UPDATE time_entries
SET duration_seconds = duration_minutes::BIGINT * 60
WHERE duration_minutes IS NOT NULL;

ALTER TABLE time_entries DROP COLUMN duration_minutes;

ALTER TABLE time_entries
    ADD COLUMN is_active BOOLEAN,
    ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;

UPDATE time_entries SET is_active = TRUE;

ALTER TABLE time_entries
    ALTER COLUMN is_active SET DEFAULT TRUE,
    ALTER COLUMN is_active SET NOT NULL;

ALTER TABLE time_entries
    ADD CONSTRAINT chk_time_entries_completion
    CHECK (
        (
            entry_type = 'TIMER'
            AND (
                (ended_at IS NULL AND duration_seconds IS NULL)
                OR
                (ended_at IS NOT NULL AND duration_seconds > 0)
            )
        )
        OR
        (
            entry_type = 'MANUAL'
            AND ended_at IS NOT NULL
            AND duration_seconds > 0
        )
    );

CREATE INDEX idx_time_entries_owner_is_active ON time_entries(owner_id, is_active);

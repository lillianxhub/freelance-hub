-- V006: Create time_entries table
-- Author: Petpinyo (673380073-7)
-- Description: Time entries table for time tracking

CREATE TABLE time_entries (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    task_id BIGINT,
    description TEXT,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    duration_minutes INTEGER,
    is_billable BOOLEAN NOT NULL DEFAULT TRUE,
    hourly_rate DECIMAL(10, 2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_time_entry_owner FOREIGN KEY (owner_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_time_entry_project FOREIGN KEY (project_id)
        REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_time_entry_task FOREIGN KEY (task_id)
        REFERENCES tasks(id) ON DELETE SET NULL,
    CONSTRAINT chk_time_entry_end_after_start CHECK (end_time IS NULL OR end_time > start_time)
);

-- Index for owner_id (important for owner isolation)
CREATE INDEX idx_time_entries_owner_id ON time_entries(owner_id);

-- Index for project_id (for filtering by project)
CREATE INDEX idx_time_entries_project_id ON time_entries(project_id);

-- Index for task_id (for filtering by task)
CREATE INDEX idx_time_entries_task_id ON time_entries(task_id);

-- Index for date range queries
CREATE INDEX idx_time_entries_start_time ON time_entries(start_time);

-- Index for billable filtering
CREATE INDEX idx_time_entries_is_billable ON time_entries(is_billable);

-- Index for finding running timers (end_time IS NULL)
CREATE INDEX idx_time_entries_running ON time_entries(owner_id, end_time) WHERE end_time IS NULL;

-- Comments
COMMENT ON TABLE time_entries IS 'Time tracking entries for projects and tasks';
COMMENT ON COLUMN time_entries.owner_id IS 'Foreign key to users table - owner isolation';
COMMENT ON COLUMN time_entries.project_id IS 'Foreign key to projects table';
COMMENT ON COLUMN time_entries.task_id IS 'Optional foreign key to tasks table';
COMMENT ON COLUMN time_entries.end_time IS 'NULL means timer is still running';
COMMENT ON COLUMN time_entries.duration_minutes IS 'Calculated duration in minutes';

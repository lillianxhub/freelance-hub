-- V006: Create time entries table
-- Description: Timer and manual work entries recorded against projects and tasks

CREATE TABLE time_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    owner_id UUID NOT NULL,
    project_id UUID NOT NULL,
    task_id UUID,

    description TEXT,
    entry_type VARCHAR(10) NOT NULL,

    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at TIMESTAMP WITH TIME ZONE,
    duration_minutes INTEGER,
    locked_at TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_time_entries_owner
        FOREIGN KEY (owner_id)
        REFERENCES users(id),

    CONSTRAINT fk_time_entries_project_owner
        FOREIGN KEY (project_id, owner_id)
        REFERENCES projects(id, owner_id),

    CONSTRAINT fk_time_entries_task_project
        FOREIGN KEY (task_id, project_id)
        REFERENCES tasks(id, project_id),

    CONSTRAINT chk_time_entries_entry_type
        CHECK (
            entry_type IN ('TIMER', 'MANUAL')
        ),

    CONSTRAINT chk_time_entries_time_range
        CHECK (
            ended_at IS NULL
            OR ended_at > started_at
        ),

    CONSTRAINT chk_time_entries_completion
        CHECK (
            (
                entry_type = 'TIMER'
                AND (
                    (
                        ended_at IS NULL
                        AND duration_minutes IS NULL
                    )
                    OR
                    (
                        ended_at IS NOT NULL
                        AND duration_minutes > 0
                    )
                )
            )
            OR
            (
                entry_type = 'MANUAL'
                AND ended_at IS NOT NULL
                AND duration_minutes > 0
            )
        ),

    CONSTRAINT chk_time_entries_locked_at
        CHECK (
            locked_at IS NULL
            OR ended_at IS NOT NULL
        )
);

CREATE INDEX idx_time_entries_owner_started_at
    ON time_entries(owner_id, started_at);

CREATE INDEX idx_time_entries_project_started_at
    ON time_entries(project_id, started_at);

CREATE INDEX idx_time_entries_task_started_at
    ON time_entries(task_id, started_at);

CREATE UNIQUE INDEX uk_time_entries_owner_running_timer
    ON time_entries(owner_id)
    WHERE entry_type = 'TIMER'
      AND ended_at IS NULL;

COMMENT ON TABLE time_entries
    IS 'Timer and manual work entries recorded against projects and tasks';

COMMENT ON COLUMN time_entries.owner_id
    IS 'User who owns the time entry';

COMMENT ON COLUMN time_entries.project_id
    IS 'Project against which the work was recorded';

COMMENT ON COLUMN time_entries.task_id
    IS 'Optional task within the selected project';

COMMENT ON COLUMN time_entries.entry_type
    IS 'TIMER or MANUAL';

COMMENT ON COLUMN time_entries.started_at
    IS 'UTC instant when the recorded work started';

COMMENT ON COLUMN time_entries.ended_at
    IS 'UTC instant when the recorded work ended; null for a running timer';

COMMENT ON COLUMN time_entries.duration_minutes
    IS 'Completed duration rounded up to whole minutes';

COMMENT ON COLUMN time_entries.locked_at
    IS 'Time when the completed entry was locked from further changes';

COMMENT ON COLUMN time_entries.version
    IS 'Optimistic locking version';

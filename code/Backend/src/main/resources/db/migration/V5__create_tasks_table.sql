-- V005: Create tasks table
-- Author: Kantavit (673380027-4)
-- Description: Tasks contained within projects

CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    project_id UUID NOT NULL,

    name VARCHAR(180) NOT NULL,
    description TEXT,

    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    sort_order INTEGER NOT NULL,

    completed_at TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_tasks_project
        FOREIGN KEY (project_id)
        REFERENCES projects(id),

    CONSTRAINT uk_tasks_project_sort_order
        UNIQUE (project_id, sort_order),

    CONSTRAINT uk_tasks_id_project
        UNIQUE (id, project_id),

    CONSTRAINT chk_tasks_sort_order
        CHECK (
            sort_order >= 0
        ),

    CONSTRAINT chk_tasks_status
        CHECK (
            status IN (
                'OPEN',
                'IN_PROGRESS',
                'COMPLETED'
            )
        ),

    CONSTRAINT chk_tasks_completed_at
        CHECK (
            (
                status = 'COMPLETED'
                AND completed_at IS NOT NULL
            )
            OR
            (
                status <> 'COMPLETED'
                AND completed_at IS NULL
            )
        )
);

CREATE INDEX idx_tasks_project_status
    ON tasks(project_id, status);

COMMENT ON TABLE tasks
    IS 'Tasks contained within projects';

COMMENT ON COLUMN tasks.id
    IS 'Unique task identifier';

COMMENT ON COLUMN tasks.project_id
    IS 'Project containing the task';

COMMENT ON COLUMN tasks.name
    IS 'Task name';

COMMENT ON COLUMN tasks.description
    IS 'Optional task description';

COMMENT ON COLUMN tasks.status
    IS 'OPEN, IN_PROGRESS, or COMPLETED';

COMMENT ON COLUMN tasks.sort_order
    IS 'Task position within its project';

COMMENT ON COLUMN tasks.completed_at
    IS 'Time when the task was completed';

COMMENT ON COLUMN tasks.created_at
    IS 'Time when the task was created';

COMMENT ON COLUMN tasks.updated_at
    IS 'Time when the task was last updated';

COMMENT ON COLUMN tasks.version
    IS 'Optimistic locking version';

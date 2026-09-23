-- V004: Create projects table
-- Author: Kantavit (673380027-4)
-- Description: Projects managed by users for clients

CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    owner_id UUID NOT NULL,
    client_id UUID NOT NULL,

    name VARCHAR(180) NOT NULL,
    description TEXT,

    start_date DATE,
    end_date DATE,

    color VARCHAR(7),
    target_minutes INTEGER,

    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',

    created_at TIMESTAMP WITH TIME ZONE NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_projects_owner
        FOREIGN KEY (owner_id)
        REFERENCES users(id),

    CONSTRAINT fk_projects_client
        FOREIGN KEY (client_id)
        REFERENCES clients(id),

    CONSTRAINT uk_projects_id_owner
        UNIQUE (id, owner_id),

    CONSTRAINT chk_projects_dates
        CHECK (
            start_date IS NULL
            OR end_date IS NULL
            OR end_date >= start_date
        ),

    CONSTRAINT chk_projects_color
        CHECK (
            color IS NULL
            OR color LIKE '#______'
        ),

    CONSTRAINT chk_projects_target_minutes
        CHECK (
            target_minutes IS NULL
            OR target_minutes > 0
        ),

    CONSTRAINT chk_projects_status
        CHECK (
            status IN (
                'PLANNED',
                'ACTIVE',
                'ON_HOLD',
                'COMPLETED',
                'ARCHIVED'
            )
        )
);

CREATE INDEX idx_projects_owner_status
    ON projects(owner_id, status);

CREATE INDEX idx_projects_client_status
    ON projects(client_id, status);

CREATE INDEX idx_projects_owner_start_date
    ON projects(owner_id, start_date);

COMMENT ON TABLE projects
    IS 'Projects managed by users for clients';

COMMENT ON COLUMN projects.id
    IS 'Unique project identifier';

COMMENT ON COLUMN projects.owner_id
    IS 'User who owns the project';

COMMENT ON COLUMN projects.client_id
    IS 'Client associated with the project';

COMMENT ON COLUMN projects.name
    IS 'Project name';

COMMENT ON COLUMN projects.description
    IS 'Optional project description';

COMMENT ON COLUMN projects.start_date
    IS 'Optional project start date';

COMMENT ON COLUMN projects.end_date
    IS 'Optional project end date';

COMMENT ON COLUMN projects.color
    IS 'Display color in #RRGGBB format';

COMMENT ON COLUMN projects.target_minutes
    IS 'Target working time stored in minutes';

COMMENT ON COLUMN projects.status
    IS 'PLANNED, ACTIVE, ON_HOLD, COMPLETED, or ARCHIVED';

COMMENT ON COLUMN projects.created_at
    IS 'Time when the project was created';

COMMENT ON COLUMN projects.updated_at
    IS 'Time when the project was last updated';

COMMENT ON COLUMN projects.version
    IS 'Optimistic locking version';

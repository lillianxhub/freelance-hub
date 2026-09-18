-- V005: Create tasks table
-- Author: Petpinyo (673380073-7)
-- Description: Tasks table for managing project tasks

CREATE TABLE tasks (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'TODO',
    priority VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    estimated_hours DECIMAL(10, 2),
    due_date DATE,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_task_owner FOREIGN KEY (owner_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_project FOREIGN KEY (project_id)
        REFERENCES projects(id) ON DELETE CASCADE
);

-- Index for owner_id (important for owner isolation)
CREATE INDEX idx_tasks_owner_id ON tasks(owner_id);

-- Index for project_id (for filtering by project)
CREATE INDEX idx_tasks_project_id ON tasks(project_id);

-- Index for status filtering
CREATE INDEX idx_tasks_status ON tasks(status);

-- Index for priority filtering
CREATE INDEX idx_tasks_priority ON tasks(priority);

-- Index for due date queries
CREATE INDEX idx_tasks_due_date ON tasks(due_date);

-- Comments
COMMENT ON TABLE tasks IS 'Tasks within projects';
COMMENT ON COLUMN tasks.owner_id IS 'Foreign key to users table - owner isolation';
COMMENT ON COLUMN tasks.project_id IS 'Foreign key to projects table';
COMMENT ON COLUMN tasks.status IS 'Task status: TODO, IN_PROGRESS, COMPLETED, CANCELLED';
COMMENT ON COLUMN tasks.priority IS 'Task priority: LOW, MEDIUM, HIGH, URGENT';

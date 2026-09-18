-- V004: Create projects table
-- Author: Petpinyo (673380073-7)
-- Description: Projects table for managing freelance projects

CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    client_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    hourly_rate DECIMAL(10, 2),
    estimated_hours DECIMAL(10, 2),
    start_date DATE,
    end_date DATE,
    is_billable BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_project_owner FOREIGN KEY (owner_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_client FOREIGN KEY (client_id)
        REFERENCES clients(id) ON DELETE CASCADE
);

-- Index for owner_id (important for owner isolation)
CREATE INDEX idx_projects_owner_id ON projects(owner_id);

-- Index for client_id (for filtering by client)
CREATE INDEX idx_projects_client_id ON projects(client_id);

-- Index for status filtering
CREATE INDEX idx_projects_status ON projects(status);

-- Index for date range queries
CREATE INDEX idx_projects_dates ON projects(start_date, end_date);

-- Comments
COMMENT ON TABLE projects IS 'Projects managed by freelancers for clients';
COMMENT ON COLUMN projects.owner_id IS 'Foreign key to users table - owner isolation';
COMMENT ON COLUMN projects.client_id IS 'Foreign key to clients table';
COMMENT ON COLUMN projects.status IS 'Project status: ACTIVE, COMPLETED, ON_HOLD, CANCELLED';
COMMENT ON COLUMN projects.hourly_rate IS 'Hourly rate for this project';
COMMENT ON COLUMN projects.is_billable IS 'Whether time entries are billable';

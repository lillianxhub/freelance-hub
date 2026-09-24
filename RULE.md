# Database Migration Naming Rules

## Author

Petpinyo (673380073-7)

## Flyway Migration Naming Convention

### Standard Format

```
V{version}__{description}.sql
```

**Example:**
```
V1__create_users_table.sql
```

### Components

1. **Prefix: `V`**
    - Always uppercase `V`
    - Required by Flyway for a versioned migration

2. **Version Number**
    - Use a unique numeric version, for example `1`, `2`, `3`, or `1.1`
    - Do not reuse a version already applied to a database
    - Coordinate the next version with the team before creating a file
    - Version gaps are allowed by Flyway; uniqueness and ordering are what matter

3. **Description Separator: `__`**
    - Use exactly two underscores between version and description

4. **Description**
    - Lowercase with underscores (snake_case)
    - Clear, concise description of what the migration does
    - Use action verbs: `create`, `add`, `alter`, `drop`, `rename`
    - Examples:
        - `create_users_table`
        - `add_email_index`
        - `alter_projects_add_status`

### Complete Examples

```
V1__create_users_table.sql
V2__create_user_profiles_table.sql
V3__create_clients_table.sql
V4__create_projects_table.sql
V5__create_tasks_table.sql
V6__create_time_entries_table.sql
V7__add_indexes_for_performance.sql
V8__alter_users_add_last_login.sql
```

## Migration File Structure

Each migration file should follow this structure:

```sql
-- V{version}: Brief title
-- Author: {Your Name} ({Student ID})
-- Description: Detailed description of changes

-- SQL statements here
CREATE TABLE ...;

-- Indexes
CREATE INDEX ...;

-- Comments for documentation
COMMENT ON TABLE ... IS '...';
COMMENT ON COLUMN ... IS '...';
```

**Example:**
```sql
-- V1: Create users table
-- Author: Petpinyo (673380073-7)
-- Description: Users table for authentication and authorization

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    ...
);
```

## Best Practices

### 1. One Logical Change Per Migration

- Each migration should do ONE thing (create one table, add one feature)
- Don't mix multiple unrelated changes

### 2. Include Proper Constraints

- Foreign keys with appropriate `ON DELETE` behavior
- Check constraints for data validation
- Unique constraints where needed

### 3. Add Indexes Strategically

- Index foreign key columns (important for joins and owner isolation)
- Index columns used in WHERE clauses
- Index columns used for sorting (ORDER BY)

### 4. Document Everything

- Add comments to explain table purposes
- Add comments to explain important columns
- Use SQL `COMMENT ON` statements

### 5. Never Modify Existing Migrations

- Once a migration is committed, NEVER change it
- Create a new migration to fix issues
- Example: If `V3__create_clients_table.sql` has a bug, create `V6__fix_clients_constraint.sql` to fix it

### 6. Test Before Committing

- Run migration on local database
- Verify tables are created correctly
- Check indexes exist
- Verify constraints work

## Owner Isolation Pattern

All user-owned tables must include `owner_id`:

```sql
CREATE TABLE example_table (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,  -- Always include this
    -- other columns...

    CONSTRAINT fk_example_owner FOREIGN KEY (owner_id)
        REFERENCES users(id) ON DELETE CASCADE
);

-- Always index owner_id for performance
CREATE INDEX idx_example_owner_id ON example_table(owner_id);
```

## Common Patterns

### Creating a New Table

```
V{number}__create_{table_name}_table.sql
```

**Example:** `V1__create_users_table.sql`

### Adding Columns

```
V{number}__alter_{table_name}_add_{column_name}.sql
```

**Example:** `V7__alter_users_add_last_login.sql`

### Adding Indexes

```
V{number}__add_index_{table_name}_{column_name}.sql
```

**Example:** `V8__add_index_users_email.sql`

### Creating Relationships

```
V{number}__add_foreign_key_{table_name}_to_{referenced_table}.sql
```

**Example:** `V9__add_foreign_key_projects_to_clients.sql`

## Rollback Strategy

Flyway Community does not automatically run `U` undo migrations. For this project, use a new forward migration to correct a deployed schema, or restore a tested database backup. Do not edit a migration that has already been applied.

If the team has Flyway Teams/Enterprise and explicitly enables undo migrations, the optional naming format is:

```
U{version}__{description}.sql
```

**Example:**

```
U1__create_users_table.sql  -- Undoes V1__create_users_table.sql
```

Undo migration reverses the changes:

```sql
-- U1: Undo create users table
-- Author: Petpinyo (673380073-7)
-- Description: Rollback users table creation

DROP TABLE IF EXISTS users CASCADE;
```

## Version Control

1. **Commit migrations immediately** after verifying they work
2. **Never force-push** migrations that others have already applied
3. **Coordinate** with team before creating new migrations
4. **Use sequential numbers** to avoid conflicts

## Migration Checklist

Before committing a migration, verify:

- [ ] File name follows `V{version}__{description}.sql` format
- [ ] Author name and student ID are included in the SQL header comment
- [ ] Version number is unique and coordinated with the team
- [ ] Single underscore separators are used correctly
- [ ] Description is clear and uses snake_case
- [ ] Migration includes author comment with student ID
- [ ] All tables include `owner_id` where applicable
- [ ] Foreign keys have proper `ON DELETE` behavior
- [ ] Indexes are created for foreign keys
- [ ] Comments are added for documentation
- [ ] Migration tested successfully on local database
- [ ] No sensitive data in migration file

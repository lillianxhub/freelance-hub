# Database Migration Naming Rules

## Author

Petpinyo (673380073-7)

## Flyway Migration Naming Convention

### Standard Format

```
M{student_id_last_4}_V{version}_{description}.sql
```

**Example:**
```
M0737_V001_create_users_table.sql
```

### Components

1. **Prefix: `M`**
    - Always uppercase `M`
    - Stands for "Migration"

2. **Student ID (Last 4 digits)**
    - Format: `0737` (from student ID 6733800**73-7**)
    - Identifies the author of the migration
    - Always 4 digits, zero-padded if needed

3. **Version Separator: `_V`**
    - Single underscore followed by uppercase `V`
    - Separates student ID from version number

4. **Version Number**
    - Format: `001`, `002`, `003`, ... (3 digits, zero-padded)
    - Must be sequential and unique
    - Never reuse or skip version numbers
    - Examples: `V001`, `V002`, `V006`

5. **Description Separator: `_`**
    - Single underscore separates version from description

6. **Description**
    - Lowercase with underscores (snake_case)
    - Clear, concise description of what the migration does
    - Use action verbs: `create`, `add`, `alter`, `drop`, `rename`
    - Examples:
        - `create_users_table`
        - `add_email_index`
        - `alter_projects_add_status`

### Complete Examples

```
M0737_V001_create_users_table.sql
M0737_V002_create_user_profiles_table.sql
M0737_V003_create_clients_table.sql
M0737_V004_create_projects_table.sql
M0737_V005_create_tasks_table.sql
M0737_V006_create_time_entries_table.sql
M0737_V007_add_indexes_for_performance.sql
M0737_V008_alter_users_add_last_login.sql
```

## Migration File Structure

Each migration file should follow this structure:

```sql
-- M{student_id}_V{number}: Brief title
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
-- M0737_V001: Create users table
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
- Example: If M0737_V003 has a bug, create M0737_V007 to fix it

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
M{student_id}_V{number}_create_{table_name}_table.sql
```

**Example:** `M0737_V001_create_users_table.sql`

### Adding Columns

```
M{student_id}_V{number}_alter_{table_name}_add_{column_name}.sql
```

**Example:** `M0737_V007_alter_users_add_last_login.sql`

### Adding Indexes

```
M{student_id}_V{number}_add_index_{table_name}_{column_name}.sql
```

**Example:** `M0737_V008_add_index_users_email.sql`

### Creating Relationships

```
M{student_id}_V{number}_add_foreign_key_{table_name}_to_{referenced_table}.sql
```

**Example:** `M0737_V009_add_foreign_key_projects_to_clients.sql`

## Rollback Strategy

Flyway supports rollback with `U` prefix (undo migrations):

```
M{student_id}_U{version}_{description}.sql
```

**Example:**

```
M0737_U001_create_users_table.sql  -- Undoes M0737_V001
```

Undo migration reverses the changes:

```sql
-- M0737_U001: Undo create users table
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

- [ ] File name follows `M{student_id}_V{number}_{description}.sql` format
- [ ] Student ID is correct (0737 for Petpinyo)
- [ ] Version number is sequential (next available number)
- [ ] Single underscore separators are used correctly
- [ ] Description is clear and uses snake_case
- [ ] Migration includes author comment with student ID
- [ ] All tables include `owner_id` where applicable
- [ ] Foreign keys have proper `ON DELETE` behavior
- [ ] Indexes are created for foreign keys
- [ ] Comments are added for documentation
- [ ] Migration tested successfully on local database
- [ ] No sensitive data in migration file

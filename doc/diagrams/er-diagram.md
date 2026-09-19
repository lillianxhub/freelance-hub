# Freelance Hub MVP — ER Diagram

แผนภาพนี้เป็น logical database schema สำหรับ MVP เท่านั้น โดย Dashboard และ Analytics
คำนวณจาก `time_entries` จึงไม่ต้องมีตารางสรุปแยกในระยะแรก

```mermaid
erDiagram
    USERS {
        uuid id PK
        varchar email UK
        varchar password_hash
        varchar role
        boolean enabled
        timestamptz created_at
        timestamptz updated_at
        bigint version
    }
    USER_PROFILES {
        uuid user_id PK,FK
        varchar display_name
        varchar first_name
        varchar last_name
        varchar phone
        text address
        varchar city
        varchar country
        varchar postal_code
        varchar avatar_url
        varchar timezone
        varchar date_format
        timestamptz created_at
        timestamptz updated_at
        bigint version
    }
    CLIENTS {
        uuid id PK
        uuid owner_id FK
        varchar name
        varchar company_name
        varchar email
        varchar phone
        text address
        varchar tax_id
        text notes
        varchar status
        timestamptz created_at
        timestamptz updated_at
        bigint version
    }
    PROJECTS {
        uuid id PK
        uuid owner_id FK
        uuid client_id FK
        varchar name
        text description
        date start_date
        date end_date
        varchar color
        integer target_minutes
        varchar status
        timestamptz created_at
        timestamptz updated_at
        bigint version
    }
    TASKS {
        uuid id PK
        uuid project_id FK
        varchar name
        text description
        varchar status
        integer sort_order
        timestamptz completed_at
        timestamptz created_at
        timestamptz updated_at
        bigint version
    }
    TIME_ENTRIES {
        uuid id PK
        uuid owner_id FK
        uuid project_id FK
        uuid task_id FK
        text description
        varchar entry_type
        timestamptz started_at
        timestamptz ended_at
        integer duration_minutes
        timestamptz locked_at
        timestamptz created_at
        timestamptz updated_at
        bigint version
    }

    USERS ||--|| USER_PROFILES : has
    USERS ||--o{ CLIENTS : owns
    USERS ||--o{ PROJECTS : owns
    USERS ||--o{ TIME_ENTRIES : owns
    CLIENTS ||--o{ PROJECTS : has
    PROJECTS ||--o{ TASKS : contains
    PROJECTS ||--o{ TIME_ENTRIES : records
    TASKS o|--o{ TIME_ENTRIES : categorizes
```

## Cardinality และกติกาสำคัญ

- `users` 1 — 1 `user_profiles`: ใช้ `user_profiles.user_id` เป็นทั้ง PK และ FK เพื่อบังคับ One-to-One จริง
- `users` 1 — N `clients`, `projects`, `time_entries`: `owner_id` ใช้แยกข้อมูลของผู้ใช้และช่วยให้ query ด้าน security ตรงไปตรงมา
- `clients` 1 — N `projects`: ทุกโปรเจกต์ต้องมีลูกค้าหนึ่งราย
- `projects` 1 — N `tasks` และ `time_entries`: time entry ต้องมีโปรเจกต์เสมอ
- `tasks` 0..1 — N `time_entries`: การเลือก task เป็น optional แต่ถ้าเลือก task ต้องอยู่ใน project เดียวกับ time entry
- Unique partial index ที่ `time_entries(owner_id) WHERE ended_at IS NULL` บังคับให้ผู้ใช้มี running timer ได้สูงสุดหนึ่งรายการ
- Composite FK `(client_id, owner_id)` และ `(project_id, owner_id)` ป้องกันการผูกข้อมูลข้ามเจ้าของ ส่วน `(task_id, project_id)` ป้องกันการเลือก task ข้าม project
- Archive ใช้สถานะ `ARCHIVED` ไม่ลบ `clients` หรือ `projects` ที่มีประวัติ เพื่อรักษา time entries
- Timestamp ทั้งหมดใช้ `timestamptz` และบันทึกเป็น UTC
- เก็บเวลาเป้าหมายและเวลาทำงานเป็นนาทีจำนวนเต็ม ป้องกันความคลาดเคลื่อนจากเลขทศนิยม

## Enum/check values

| Column                    | Allowed values                                          |
| ------------------------- | ------------------------------------------------------- |
| `users.role`              | `FREELANCER` (เตรียม `ADMIN` ไว้สำหรับระยะถัดไป)        |
| `clients.status`          | `ACTIVE`, `ARCHIVED`                                    |
| `projects.status`         | `PLANNED`, `ACTIVE`, `ON_HOLD`, `COMPLETED`, `ARCHIVED` |
| `tasks.status`            | `OPEN`, `IN_PROGRESS`, `COMPLETED`                      |
| `time_entries.entry_type` | `TIMER`, `MANUAL`                                       |

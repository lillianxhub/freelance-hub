# Data Dictionary - Freelance Hub MVP

เอกสารนี้อ้างอิงจาก DBML รุ่นปรับปรุงล่าสุดของ MVP โดยใช้ soft delete เป็นมาตรฐานร่วมกัน:

- `is_active = true` หมายถึง record ยังใช้งานอยู่
- `is_active = false` หมายถึง record ถูก archive หรือ soft delete
- `deleted_at` เก็บเวลาที่ soft delete และเป็น `NULL` สำหรับ record ปกติ
- ตาราง Finance, Invoice, Payment, Income, Expense และ Analytics Snapshot ไม่อยู่ใน MVP

> หมายเหตุ: DBML ต้นทางสะกด field นี้เป็น `deleate_at`; เอกสารและ schema ที่ใช้งานจริงกำหนดชื่อมาตรฐานเป็น `deleted_at`

> หมายเหตุ: schema นี้เป็น target schema ตาม DBML ล่าสุด ดังนั้น Flyway migration และ JPA Entity ต้องปรับให้ตรงก่อนเปิดใช้ `ddl-auto=validate` ใน Backend

## `users`

| Column | Type | Null | Constraint / Default | Description |
|---|---|---:|---|---|
| `id` | `uuid` | No | PK, `gen_random_uuid()` | รหัสผู้ใช้ |
| `email` | `varchar(255)` | No | Unique | อีเมลสำหรับเข้าสู่ระบบ |
| `password_hash` | `varchar(255)` | No | | รหัสผ่านที่ hash แล้ว |
| `role` | `varchar(50)` | No | `USER` | บทบาท `USER` หรือ `ADMIN` |
| `is_active` | `boolean` | No | `true` | สถานะการใช้งานบัญชี |
| `version` | `bigint` | No | `0` | Optimistic locking |
| `created_at` | `timestamptz` | No | `now()` | เวลาสร้างบัญชี |
| `updated_at` | `timestamptz` | No | `now()` | เวลาแก้ไขล่าสุด |
| `deleted_at` | `timestamptz` | Yes | `NULL` | เวลา soft delete |

Indexes: `idx_users_is_active`; unique index จาก `email`

## `user_profiles`

| Column | Type | Null | Constraint / Default | Description |
|---|---|---:|---|---|
| `user_id` | `uuid` | No | PK, FK -> `users.id` | Shared primary key แบบ 1:1 |
| `display_name` | `varchar(255)` | Yes | | ชื่อที่แสดง |
| `first_name` | `varchar(100)` | Yes | | ชื่อ |
| `last_name` | `varchar(100)` | Yes | | นามสกุล |
| `phone` | `varchar(20)` | Yes | | เบอร์โทรศัพท์ |
| `address` | `text` | Yes | | บ้านเลขที่และรายละเอียดที่อยู่ |
| `subdistrict` | `varchar(100)` | Yes | | ตำบลหรือแขวง |
| `district` | `varchar(100)` | Yes | | อำเภอหรือเขต |
| `province` | `varchar(100)` | Yes | | จังหวัด |
| `postal_code` | `varchar(20)` | Yes | | รหัสไปรษณีย์ |
| `date_format` | `varchar(20)` | Yes | `YYYY-MM-DD` | รูปแบบวันที่ |
| `bio` | `text` | Yes | | ประวัติย่อ |
| `is_active` | `boolean` | No | `true` | สถานะ Profile |
| `version` | `bigint` | No | `0` | Optimistic locking |
| `created_at` | `timestamptz` | No | `now()` | เวลาสร้าง Profile |
| `updated_at` | `timestamptz` | No | `now()` | เวลาแก้ไขล่าสุด |
| `deleted_at` | `timestamptz` | Yes | `NULL` | เวลา soft delete |

Indexes: `idx_user_profiles_province`, `idx_user_profiles_postal_code`

## `clients`

| Column | Type | Null | Constraint / Default | Description |
|---|---|---:|---|---|
| `id` | `uuid` | No | PK, `gen_random_uuid()` | รหัสลูกค้า |
| `owner_id` | `uuid` | No | FK -> `users.id` | เจ้าของข้อมูล |
| `name` | `varchar(150)` | No | | ชื่อลูกค้า |
| `company_name` | `varchar(200)` | Yes | | ชื่อบริษัท |
| `email` | `varchar(254)` | Yes | | อีเมล |
| `phone` | `varchar(30)` | Yes | | เบอร์โทรศัพท์ |
| `address` | `text` | Yes | | รายละเอียดที่อยู่ |
| `subdistrict` | `varchar(100)` | Yes | | ตำบลหรือแขวง |
| `district` | `varchar(100)` | Yes | | อำเภอหรือเขต |
| `province` | `varchar(100)` | Yes | | จังหวัด |
| `postal_code` | `varchar(20)` | Yes | | รหัสไปรษณีย์ |
| `tax_id` | `varchar(30)` | Yes | | เลขประจำตัวผู้เสียภาษี |
| `notes` | `text` | Yes | | หมายเหตุภายใน |
| `is_active` | `boolean` | No | `true` | สถานะการใช้งานหรือ Archive |
| `created_at` | `timestamptz` | No | `now()` | เวลาสร้าง |
| `updated_at` | `timestamptz` | No | `now()` | เวลาแก้ไขล่าสุด |
| `deleted_at` | `timestamptz` | Yes | `NULL` | เวลา soft delete |
| `version` | `bigint` | No | `0` | Optimistic locking |

Indexes: unique `(id, owner_id)`, `(owner_id, is_active)`, `(owner_id, name)`, `email`, `province`, `postal_code`

## `projects`

| Column | Type | Null | Constraint / Default | Description |
|---|---|---:|---|---|
| `id` | `uuid` | No | PK, `gen_random_uuid()` | รหัส Project |
| `owner_id` | `uuid` | No | FK -> `users.id` | เจ้าของ Project |
| `client_id` | `uuid` | No | Composite FK with `owner_id` | ลูกค้าของ Project |
| `name` | `varchar(180)` | No | | ชื่อ Project |
| `description` | `text` | Yes | | รายละเอียด |
| `start_date` | `date` | Yes | | วันที่เริ่ม |
| `end_date` | `date` | Yes | `end_date >= start_date` | วันที่สิ้นสุด |
| `color` | `varchar(7)` | Yes | `#RRGGBB` | สีแสดงผล |
| `target_minutes` | `integer` | Yes | `> 0` | เป้าหมายเวลาทำงาน |
| `status` | `varchar(20)` | No | `PLANNED` | Project lifecycle |
| `is_active` | `boolean` | No | `true` | สถานะ record |
| `created_at` | `timestamptz` | No | `now()` | เวลาสร้าง |
| `updated_at` | `timestamptz` | No | `now()` | เวลาแก้ไขล่าสุด |
| `deleted_at` | `timestamptz` | Yes | `NULL` | เวลา soft delete |
| `version` | `bigint` | No | `0` | Optimistic locking |

Indexes: unique `(id, owner_id)`, `(owner_id, status)`, `(client_id, status)`, `(owner_id, start_date)`, `(owner_id, is_active)`

## `tasks`

| Column | Type | Null | Constraint / Default | Description |
|---|---|---:|---|---|
| `id` | `uuid` | No | PK, `gen_random_uuid()` | รหัส Task |
| `project_id` | `uuid` | No | FK -> `projects.id` | Project เจ้าของ Task |
| `name` | `varchar(180)` | No | | ชื่องาน |
| `description` | `text` | Yes | | รายละเอียด |
| `status` | `varchar(20)` | No | `OPEN` | Task lifecycle |
| `sort_order` | `integer` | No | `>= 0` | ลำดับใน Project |
| `completed_at` | `timestamptz` | Yes | Required when completed | เวลาที่เสร็จ |
| `is_active` | `boolean` | No | `true` | สถานะ record |
| `created_at` | `timestamptz` | No | `now()` | เวลาสร้าง |
| `updated_at` | `timestamptz` | No | `now()` | เวลาแก้ไขล่าสุด |
| `deleted_at` | `timestamptz` | Yes | `NULL` | เวลา soft delete |
| `version` | `bigint` | No | `0` | Optimistic locking |

Indexes: `(project_id, sort_order)`, unique `(id, project_id)`, `(project_id, status)`, `(project_id, is_active)`

## `time_entries`

| Column | Type | Null | Constraint / Default | Description |
|---|---|---:|---|---|
| `id` | `uuid` | No | PK, `gen_random_uuid()` | รหัส Time Entry |
| `owner_id` | `uuid` | No | FK through composite Project FK | เจ้าของรายการ |
| `project_id` | `uuid` | No | Composite FK -> `projects` | Project ที่ทำงาน |
| `task_id` | `uuid` | Yes | Composite FK -> `tasks` | Task ที่เกี่ยวข้อง |
| `description` | `text` | Yes | | รายละเอียดงาน |
| `entry_type` | `varchar(10)` | No | `TIMER` หรือ `MANUAL` | ประเภทการบันทึก |
| `started_at` | `timestamptz` | No | | เวลาเริ่ม |
| `ended_at` | `timestamptz` | Yes | `> started_at` | เวลาจบ; NULL ขณะ Timer ทำงาน |
| `duration_seconds` | `bigint` | Yes | `> 0` | ระยะเวลาเป็นวินาที |
| `locked_at` | `timestamptz` | Yes | `NULL` | เวลาที่ล็อกรายการไม่ให้แก้ไขหรือลบ |
| `is_active` | `boolean` | No | `true` | สถานะ record |
| `created_at` | `timestamptz` | No | `now()` | เวลาสร้าง |
| `updated_at` | `timestamptz` | No | `now()` | เวลาแก้ไขล่าสุด |
| `deleted_at` | `timestamptz` | Yes | `NULL` | เวลา soft delete |
| `version` | `bigint` | No | `0` | Optimistic locking |

Constraints: completed entry ต้องมี `ended_at` และ `duration_seconds`; running timer ต้องมี `ended_at = NULL`; ผู้ใช้หนึ่งคนมี running timer ได้สูงสุดหนึ่งรายการ

Indexes: `(owner_id, started_at)`, `(project_id, started_at)`, `(task_id, started_at)`, `(owner_id, is_active)` และ partial unique index ของ running timer

## `revoked_tokens`

| Column | Type | Null | Constraint / Default | Description |
|---|---|---:|---|---|
| `id` | `uuid` | No | PK, `gen_random_uuid()` | รหัสรายการ revoke |
| `jti` | `varchar(36)` | No | Unique | JWT ID |
| `user_id` | `uuid` | No | FK -> `users.id` | เจ้าของ Token |
| `expires_at` | `timestamptz` | No | | วันหมดอายุ Token |
| `revoked_at` | `timestamptz` | No | | เวลาที่ revoke |
| `is_active` | `boolean` | No | `true` | ยังใช้ตรวจสอบการ revoke |
| `created_at` | `timestamptz` | No | `now()` | เวลาสร้าง record |
| `updated_at` | `timestamptz` | No | `now()` | เวลาแก้ไขล่าสุด |
| `deleted_at` | `timestamptz` | Yes | `NULL` | เวลา soft delete |

Indexes: `user_id`, `expires_at`, `is_active`; unique `jti`

## Enum และ Check Values

| Field | Allowed values |
|---|---|
| `users.role` | `USER`, `ADMIN` |
| `projects.status` | `PLANNED`, `ACTIVE`, `ON_HOLD`, `COMPLETED`, `ARCHIVED` |
| `tasks.status` | `OPEN`, `IN_PROGRESS`, `COMPLETED` |
| `time_entries.entry_type` | `TIMER`, `MANUAL` |

## Relationships

| Relationship | Cardinality | Rule |
|---|---|---|
| `users` -> `user_profiles` | 1:1 | `user_profiles.user_id` เป็น PK/FK |
| `users` -> `clients` | 1:N | ทุก Client ต้องมี Owner |
| `users` -> `projects` | 1:N | Owner isolation |
| `users` -> `time_entries` | 1:N | Owner isolation |
| `users` -> `revoked_tokens` | 1:N | ลบ User แล้ว revoke records cascade |
| `clients` -> `projects` | 1:N | ใช้ composite FK `(client_id, owner_id)` |
| `projects` -> `tasks` | 1:N | Task ต้องอยู่ใน Project เดียว |
| `projects` -> `time_entries` | 1:N | ใช้ composite FK `(project_id, owner_id)` |
| `tasks` -> `time_entries` | 0..1:N | ใช้ composite FK `(task_id, project_id)` |

Dashboard และ Productivity Insights เป็น query/projection จาก `projects`, `tasks` และ `time_entries` โดยไม่สร้างตาราง Analytics แยกใน MVP

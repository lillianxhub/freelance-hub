# Data Dictionary - Freelance Hub MVP

เอกสารฉบับส่งมอบตรวจจาก Flyway migrations V1-V20 และ JPA entities ณ commit `131305f` วันที่ 9 ตุลาคม 2026 ตารางด้านล่างเป็น schema หลังใช้ migrations ครบ ไม่ใช่เฉพาะ CREATE TABLE รุ่นแรก

- `is_active = true` หมายถึง record ยังใช้งานอยู่
- ความหมายของ `is_active` และ `deleted_at` ต้องอ่านตาม feature ไม่ใช่ถือว่าสองฟิลด์เปลี่ยนพร้อมกันเสมอ
- Client archive เปลี่ยน `is_active=false` โดยไม่ตั้ง `deleted_at`; Client soft delete ตั้งเฉพาะ `deleted_at` และคง `is_active` เดิม
- Project status `ARCHIVED` ทำให้ inactive แต่ยังไม่ลบ; Project/Task/completed Time Entry soft delete ตั้ง inactive พร้อม `deleted_at`
- `deleted_at` เก็บเวลาที่ soft delete และเป็น `NULL` สำหรับ record ปกติ
- ตาราง Finance, Invoice, Payment, Income, Expense และ Analytics Snapshot ไม่อยู่ใน MVP

> หมายเหตุ: DBML ต้นทางสะกด field นี้เป็น `deleate_at`; เอกสารและ schema ที่ใช้งานจริงกำหนดชื่อมาตรฐานเป็น `deleted_at`

> แหล่ง schema: `code/Backend/src/main/resources/db/migration/` โดย V11/V13/V14 ปรับ activation/address/time units, V15 เพิ่ม profile tax_id, V17/V18 เปลี่ยน token storage, V19 normalize email และ V20 ลบ date_format ส่วน `addresses`/`revoked_tokens` เป็นตารางเก่าที่ไม่อยู่ใน schema สุดท้าย

## `users`

| Column | Type | Null | Constraint / Default | Description |
|---|---|---:|---|---|
| `id` | `uuid` | No | PK, `gen_random_uuid()` | รหัสผู้ใช้ |
| `email` | `varchar(255)` | No | Unique, normalized lowercase/trim | อีเมลสำหรับเข้าสู่ระบบ |
| `password_hash` | `varchar(255)` | No | | รหัสผ่านที่ hash แล้ว |
| `role` | `varchar(50)` | No | `USER` | บทบาท `USER` หรือ `ADMIN` |
| `is_active` | `boolean` | No | `true` | สถานะการใช้งานบัญชี |
| `version` | `bigint` | No | `0` | Optimistic locking |
| `created_at` | `timestamptz` | No | `now()` | เวลาสร้างบัญชี |
| `updated_at` | `timestamptz` | No | `now()` | เวลาแก้ไขล่าสุด |
| `deleted_at` | `timestamptz` | Yes | `NULL` | เวลา soft delete |

Indexes: `idx_users_is_active`; unique index จาก `email`; `idx_users_email_canonical_unique` บน `lower(btrim(email))` เพื่อกัน email ซ้ำต่างตัวพิมพ์หรือช่องว่างหัวท้าย

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
| `tax_id` | `varchar(30)` | Yes | | เลขประจำตัวผู้เสียภาษี |
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
| `is_active` | `boolean` | No | `true` | ACTIVE/ARCHIVED; soft delete ไม่เปลี่ยนฟิลด์นี้ |
| `created_at` | `timestamptz` | No | `now()` | เวลาสร้าง |
| `updated_at` | `timestamptz` | No | `now()` | เวลาแก้ไขล่าสุด |
| `deleted_at` | `timestamptz` | Yes | `NULL` | เวลา soft delete; Client API ตัดรายการนี้ออกไม่ว่า is_active เป็นค่าใด |
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
| `color` | `varchar(7)` | Yes | DB CHECK แบบ `#______`; API validate hex `#RRGGBB` | สีแสดงผล |
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
| `sort_order` | `integer` | No | `>= 0`; unique ภายใน Project ร่วมกับ `project_id` | ลำดับใน Project |
| `completed_at` | `timestamptz` | Yes | Required when completed | เวลาที่เสร็จ |
| `is_active` | `boolean` | No | `true` | สถานะ record |
| `created_at` | `timestamptz` | No | `now()` | เวลาสร้าง |
| `updated_at` | `timestamptz` | No | `now()` | เวลาแก้ไขล่าสุด |
| `deleted_at` | `timestamptz` | Yes | `NULL` | เวลา soft delete |
| `version` | `bigint` | No | `0` | Optimistic locking |

Indexes: unique `(project_id, sort_order)`, unique `(id, project_id)`, `(project_id, status)`, `(project_id, is_active)`

## `time_entries`

| Column | Type | Null | Constraint / Default | Description |
|---|---|---:|---|---|
| `id` | `uuid` | No | PK, `gen_random_uuid()` | รหัส Time Entry |
| `owner_id` | `uuid` | No | FK -> users.id และส่วนหนึ่งของ composite Project FK | เจ้าของรายการ |
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

Application rules: completed entry มี `ended_at` และ `duration_seconds > 0`; running timer มี `ended_at = NULL` และ `duration_seconds = NULL` ส่วน DB มี CHECK ชนิด/ช่วงเวลา/สถานะ completion และ `locked_at` ต้องไม่ถูกตั้งบน running timer

DB CHECK ของ completed duration ใช้ `duration_seconds > 0` บนคอลัมน์ที่ nullable; PostgreSQL CHECK ไม่เท่ากับ NOT NULL จึงไม่ควรอ้างว่า constraint นี้เพียงอย่างเดียวกัน completed duration ที่เป็น NULL ได้ กฎ Entity/Service เป็นหลักฐานส่วนเพิ่มเติม

Indexes: `(owner_id, started_at)`, `(project_id, started_at)`, `(task_id, started_at)`, `(owner_id, is_active)` และ partial unique index ของ running timer

## `refresh_tokens`

| Column | Type | Null | Constraint / Default | Description |
|---|---|---:|---|---|
| `id` | `uuid` | No | PK, `gen_random_uuid()` | รหัสรายการ refresh token |
| `user_id` | `uuid` | No | FK -> `users.id`, cascade delete | เจ้าของ token |
| `family_id` | `uuid` | No | | กลุ่ม token จากการ login ครั้งเดียว |
| `token_hash` | `varchar(64)` | No | Unique | SHA-256 hex ของ token สุ่ม 256 บิต; ไม่เก็บ token จริง |
| `created_at` | `timestamptz` | No | `now()` | เวลาสร้าง token |
| `expires_at` | `timestamptz` | No | `> created_at` | อายุ family เริ่มจาก register/login; ค่าเริ่มต้น 7 วันตาม app.auth.refresh-expiration-ms และไม่ต่ออายุเมื่อ rotate |
| `used_at` | `timestamptz` | Yes | | เวลาที่หมุน token; ใช้ซ้ำถือเป็น replay |
| `revoked_at` | `timestamptz` | Yes | | เวลาที่เพิกถอน family |

Indexes: `user_id`, `family_id`, `expires_at`; unique `token_hash` มี index ของตัวเอง

## Enum และ Check Values

รายการนี้เป็นค่าที่ application enum รองรับ; Project/Task/Entry Type มี DB CHECK แต่ users.role ไม่มี CHECK จำกัดให้เหลือสองค่านี้ใน migration ปัจจุบัน

| Field | Allowed values |
|---|---|
| `users.role` | `USER`, `ADMIN` |
| `projects.status` | `PLANNED`, `ACTIVE`, `ON_HOLD`, `COMPLETED`, `ARCHIVED` |
| `tasks.status` | `OPEN`, `IN_PROGRESS`, `COMPLETED` |
| `time_entries.entry_type` | `TIMER`, `MANUAL` |

## Relationships

| Relationship | Cardinality | Rule |
|---|---|---|
| `users` -> `user_profiles` | 1:0..1 | Shared PK/FK จำกัดไม่เกินหนึ่ง profile; registration สร้าง profile แต่ FK ไม่บังคับทุก user ต้องมีแถวลูก |
| `users` -> `clients` | 1:N | ทุก Client ต้องมี Owner |
| `users` -> `projects` | 1:N | Owner isolation |
| `users` -> `time_entries` | 1:N | Owner isolation |
| `users` -> `refresh_tokens` | 1:N | ลบ User แล้ว refresh records cascade |
| `clients` -> `projects` | 1:N | ใช้ composite FK `(client_id, owner_id)` |
| `projects` -> `tasks` | 1:N | Task ต้องอยู่ใน Project เดียว |
| `projects` -> `time_entries` | 1:N | ใช้ composite FK `(project_id, owner_id)` |
| `tasks` -> `time_entries` | 0..1:N | ใช้ composite FK `(task_id, project_id)` |

Dashboard และ Productivity Insights เป็น query/projection จาก `projects`, `tasks` และ `time_entries` โดยไม่สร้างตาราง Analytics แยกใน MVP

## ข้อมูลที่คำนวณและ Infrastructure

- `Client.status` คำนวณจาก `is_active`; `totalTrackedSeconds` เป็น aggregate ใน GET responses ไม่ใช่คอลัมน์ clients
- Task progress, tracked hours, utilization และ Project usage percentage คำนวณจาก Task/Time Entry กับ `target_minutes` ไม่เก็บซ้ำในตาราง Project
- `flyway_schema_history` เป็น metadata ของ migrations; `local_seed_records` เป็น mapping สำหรับ local seed เมื่อเปิดใช้ ไม่ใช่ business tables ใน ER ของ MVP
- Unique running-timer index ใช้เงื่อนไข `entry_type='TIMER' AND ended_at IS NULL` ไม่ใช่ index ที่กรอง `is_active`; cancel timer ลบ running row จริง ส่วน completed entry ใช้ soft delete
- Schema constraints กับ API authorization เป็นคนละชั้น: composite FKs รักษาความสัมพันธ์ owner/project/task แต่ Service ยังต้องตรวจ current-user ownership ก่อนทุก use case
- Project.status กับ is_active ไม่มี DB CHECK บังคับให้สอดคล้องกัน; V14 กำหนด is_active=true ให้ Project ที่มีอยู่ทั้งหมด แม้เคยมี status=ARCHIVED จึงไม่ควรอ้างว่าค่าสอดคล้องเสมอจาก migrations เพียงอย่างเดียว กฎการเปลี่ยนสถานะใน Entity ใช้กับคำสั่งที่ application เรียกภายหลัง
- ยังไม่ได้รัน migrations/ตรวจฐานข้อมูล live ใหม่ในงานรวมเอกสารรอบนี้; test report และผล Flyway CI ต้องตรวจแยกก่อนส่ง

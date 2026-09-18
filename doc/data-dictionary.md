# Data Dictionary — Freelance Hub MVP

ชนิดข้อมูลด้านล่างออกแบบสำหรับ PostgreSQL 16 และ JPA/Hibernate โดยใช้ UUID เป็น primary key
และใช้ `timestamptz` เก็บเวลาแบบ UTC

## `users`

| Column | Type | Null | Constraint / Index | Description |
|---|---|---:|---|---|
| `id` | `uuid` | No | PK | รหัสผู้ใช้ |
| `email` | `varchar(254)` | No | unique จากค่า lowercase | อีเมลสำหรับเข้าสู่ระบบ; application ต้อง trim/lowercase ก่อนบันทึก |
| `password_hash` | `varchar(255)` | No | | รหัสผ่านที่ hash แล้วเท่านั้น |
| `role` | `varchar(20)` | No | CHECK role | บทบาทผู้ใช้ |
| `enabled` | `boolean` | No | default `true` | สถานะบัญชี |
| `created_at` | `timestamptz` | No | | เวลาสร้าง |
| `updated_at` | `timestamptz` | No | | เวลาแก้ไขล่าสุด |
| `version` | `bigint` | No | default `0` | Optimistic lock |

Indexes: unique index `ux_users_email_lower` บน `lower(email)`

## `user_profiles`

| Column | Type | Null | Constraint / Index | Description |
|---|---|---:|---|---|
| `user_id` | `uuid` | No | PK, FK → `users.id` | Shared primary key ทำให้เป็น One-to-One |
| `display_name` | `varchar(120)` | No | | ชื่อที่แสดง |
| `phone` | `varchar(30)` | Yes | | เบอร์โทรศัพท์ |
| `address` | `text` | Yes | | ที่อยู่ |
| `avatar_url` | `varchar(500)` | Yes | | URL รูปโปรไฟล์ |
| `timezone` | `varchar(50)` | No | default `Asia/Bangkok` | IANA timezone เช่น `Asia/Bangkok` |
| `date_format` | `varchar(20)` | No | default `dd/MM/yyyy` | รูปแบบวันที่ที่แสดง |
| `created_at` | `timestamptz` | No | | เวลาสร้าง |
| `updated_at` | `timestamptz` | No | | เวลาแก้ไขล่าสุด |
| `version` | `bigint` | No | default `0` | Optimistic lock |

FK delete policy: `ON DELETE CASCADE` ใช้ได้เฉพาะการลบบัญชีผู้ใช้ทั้งระบบ

## `clients`

| Column | Type | Null | Constraint / Index | Description |
|---|---|---:|---|---|
| `id` | `uuid` | No | PK | รหัสลูกค้า |
| `owner_id` | `uuid` | No | FK → `users.id` | เจ้าของข้อมูล |
| `name` | `varchar(150)` | No | | ชื่อผู้ติดต่อ/ชื่อลูกค้า |
| `company_name` | `varchar(200)` | Yes | | ชื่อบริษัท |
| `email` | `varchar(254)` | Yes | | อีเมลติดต่อ |
| `phone` | `varchar(30)` | Yes | | เบอร์โทรศัพท์ |
| `address` | `text` | Yes | | ที่อยู่ |
| `tax_id` | `varchar(30)` | Yes | | เลขประจำตัวผู้เสียภาษี |
| `notes` | `text` | Yes | | หมายเหตุภายใน |
| `status` | `varchar(20)` | No | CHECK `ACTIVE/ARCHIVED` | สถานะลูกค้า |
| `created_at` | `timestamptz` | No | | เวลาสร้าง |
| `updated_at` | `timestamptz` | No | | เวลาแก้ไขล่าสุด |
| `version` | `bigint` | No | default `0` | Optimistic lock |

Indexes: `(owner_id, status)`, `(owner_id, lower(name))`; unique `(id, owner_id)` สำหรับ composite FK

## `projects`

| Column | Type | Null | Constraint / Index | Description |
|---|---|---:|---|---|
| `id` | `uuid` | No | PK | รหัสโปรเจกต์ |
| `owner_id` | `uuid` | No | FK → `users.id` | เจ้าของข้อมูล |
| `client_id` | `uuid` | No | composite FK → `clients(id, owner_id)` | ลูกค้าของโปรเจกต์และต้องเป็นเจ้าของเดียวกัน |
| `name` | `varchar(180)` | No | | ชื่อโปรเจกต์ |
| `description` | `text` | Yes | | รายละเอียด |
| `start_date` | `date` | Yes | | วันที่เริ่ม |
| `end_date` | `date` | Yes | CHECK end ≥ start | วันที่สิ้นสุด |
| `color` | `varchar(7)` | Yes | CHECK รูปแบบ `#RRGGBB` | สีที่ใช้แสดง |
| `currency` | `char(3)` | No | | ISO 4217 เช่น `THB`; เตรียมรองรับ post-MVP |
| `target_minutes` | `integer` | Yes | CHECK > 0 | เป้าหมายเวลา; UI แปลงเป็นชั่วโมง |
| `status` | `varchar(20)` | No | CHECK project status | สถานะโปรเจกต์ |
| `created_at` | `timestamptz` | No | | เวลาสร้าง |
| `updated_at` | `timestamptz` | No | | เวลาแก้ไขล่าสุด |
| `version` | `bigint` | No | default `0` | Optimistic lock |

Indexes: `(owner_id, status)`, `(client_id, status)`, `(owner_id, start_date)`; unique `(id, owner_id)` สำหรับ composite FK

Delete policy: `RESTRICT`; เปลี่ยนเป็น `ARCHIVED` เมื่อมีข้อมูลอ้างอิง

## `tasks`

| Column | Type | Null | Constraint / Index | Description |
|---|---|---:|---|---|
| `id` | `uuid` | No | PK | รหัสงานย่อย |
| `project_id` | `uuid` | No | FK → `projects.id` | โปรเจกต์เจ้าของ task |
| `name` | `varchar(180)` | No | | ชื่องานย่อย |
| `description` | `text` | Yes | | รายละเอียด |
| `status` | `varchar(20)` | No | CHECK task status | สถานะงาน |
| `sort_order` | `integer` | No | CHECK ≥ 0 | ลำดับในโปรเจกต์ |
| `completed_at` | `timestamptz` | Yes | | เวลาปิดงาน |
| `created_at` | `timestamptz` | No | | เวลาสร้าง |
| `updated_at` | `timestamptz` | No | | เวลาแก้ไขล่าสุด |
| `version` | `bigint` | No | default `0` | Optimistic lock |

Indexes: `(project_id, status)`, unique `(project_id, sort_order)`, unique `(id, project_id)` สำหรับ composite FK

Delete policy: `RESTRICT` เมื่อมี time entry; งานที่มีประวัติควรปิดแทนการลบ

## `time_entries`

| Column | Type | Null | Constraint / Index | Description |
|---|---|---:|---|---|
| `id` | `uuid` | No | PK | รหัสรายการเวลา |
| `owner_id` | `uuid` | No | FK → `users.id` | เจ้าของรายการ |
| `project_id` | `uuid` | No | composite FK → `projects(id, owner_id)` | โปรเจกต์และต้องเป็นเจ้าของเดียวกัน |
| `task_id` | `uuid` | Yes | composite FK → `tasks(id, project_id)` | งานย่อย optional และต้องอยู่ใน project เดียวกัน |
| `description` | `text` | Yes | | รายละเอียดงาน |
| `entry_type` | `varchar(10)` | No | CHECK `TIMER/MANUAL` | แหล่งที่มาของรายการ |
| `started_at` | `timestamptz` | No | | เวลาเริ่ม UTC |
| `ended_at` | `timestamptz` | Yes | CHECK > `started_at` | `NULL` หมายถึง timer กำลังทำงาน |
| `duration_minutes` | `integer` | Yes | CHECK > 0 | `NULL` ขณะกำลังจับเวลา; คำนวณเมื่อหยุด |
| `locked_at` | `timestamptz` | Yes | | ถ้ามีค่า ห้ามแก้ไข/ลบ |
| `created_at` | `timestamptz` | No | | เวลาสร้าง |
| `updated_at` | `timestamptz` | No | | เวลาแก้ไขล่าสุด |
| `version` | `bigint` | No | default `0` | Optimistic lock ป้องกัน stop ซ้ำ |

Checks:

- Running entry: `ended_at IS NULL AND duration_minutes IS NULL AND entry_type = 'TIMER'`
- Completed entry: `ended_at IS NOT NULL AND duration_minutes > 0`
- `ended_at > started_at` เมื่อ `ended_at` ไม่เป็น `NULL`

Indexes:

- unique partial index `(owner_id) WHERE ended_at IS NULL` เพื่อบังคับหนึ่ง running timer ต่อผู้ใช้
- `(owner_id, started_at DESC)` สำหรับ daily/weekly/date-range view
- `(project_id, started_at DESC)` และ `(task_id, started_at DESC)` สำหรับ filter/analytics

Delete policy: FK ทั้งหมด `RESTRICT`; อนุญาตลบเฉพาะรายการที่ `locked_at IS NULL` ผ่าน service

## Relationship, cascade และ fetch summary

| Relationship | Cardinality | Database delete | JPA cascade | Fetch |
|---|---|---|---|---|
| User → UserProfile | 1:1 | Profile cascade เฉพาะเมื่อลบบัญชี | `PERSIST`, `MERGE` | `LAZY` |
| User → Client | 1:N | `RESTRICT` | ไม่มี | `LAZY` |
| User → Project | 1:N | `RESTRICT` | ไม่มี | `LAZY` |
| User → TimeEntry | 1:N | `RESTRICT` | ไม่มี | `LAZY` |
| Client → Project | 1:N | `RESTRICT` | ไม่มี | `LAZY` |
| Project → Task | 1:N | `RESTRICT` | `PERSIST`, `MERGE` เฉพาะ aggregate workflow | `LAZY` |
| Project → TimeEntry | 1:N | `RESTRICT` | ไม่มี | `LAZY` |
| Task → TimeEntry | 1:N optional | `RESTRICT` | ไม่มี | `LAZY` |

Dashboard และ productivity metrics เป็น query/projection จาก completed `time_entries`
(`ended_at IS NOT NULL`) ภายใต้ owner และช่วงวันที่เดียวกัน ไม่สร้างตาราง analytics ใน MVP


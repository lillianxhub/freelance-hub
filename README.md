# Freelance Hub

Freelance Hub คือระบบบริหารงานสำหรับ Freelancer ที่รวมการจัดการลูกค้า โปรเจกต์ และงานย่อยไว้ในที่เดียว
ระบบรองรับการจับเวลาทำงานแบบ real-time และ manual entry พร้อมสรุปชั่วโมงการทำงาน
ผู้ใช้สามารถวิเคราะห์เวลาทำงานและดู productivity insights ผ่าน Dashboard
โปรเจกต์พัฒนาด้วย React + Vite สำหรับ Frontend และ Spring Boot REST API สำหรับ Backend โดยใช้ PostgreSQL บน Supabase ตาม Layered Architecture

> **Project Status:** กำลังพัฒนา

## สมาชิกกลุ่ม

| ลำดับ | ชื่อ-นามสกุล          | รหัสนักศึกษา | Section   | Branch                    | หน้าที่รับผิดชอบ                    |
| ----: | --------------------- | ------------ | --------- | ------------------------- | ----------------------------------- |
|     1 | เพชรภิญโญ ธนศิรินรากร | 673380073-7  | Section 2 | `petpinyo_673380073-7_02` | Authentication, Security, PM/DevOps |
|     2 | ถิรวัฒน์ อุจินา       | 673380039-7  | Section 2 | `thirawat_673380039-7_02` | Client Management                   |
|     3 | กันตวิชญ์ นาคนวล      | 673380027-4  | Section 1 | `kantavit_673380027-4_01` | Project และ Task Management         |
|     4 | กรมภัฏ พิริยะ         | 673380262-4  | Section 2 | `kompat_673380262-4_02`   | Time Tracking                       |
|     5 | ณัฏฐดนย์ สาริกา       | 673380511-9  | Section 2 | `nattadol_673380511-9_02` | Dashboard, Analytics และ Frontend   |

## Tech Stack

| ส่วน              | เทคโนโลยี                                                       |
| ----------------- | --------------------------------------------------------------- |
| Backend           | Java 17, Spring Boot 4.0.0                                      |
| Build Tool        | Maven                                                           |
| Database          | PostgreSQL 16                                                   |
| ORM               | Spring Data JPA / Hibernate                                     |
| Web / API         | Spring MVC, RESTful API                                         |
| Frontend          | React 19, Vite 8, React Router, Recharts                        |
| API Documentation | OpenAPI / Swagger UI เปิดเฉพาะ local, dev และ staging           |
| Testing           | JUnit 5, Mockito, Spring Boot Test                              |
| Version Control   | Git + Github                                                    |
| Deployment        | Vercel (Frontend) + Render (Backend) + Supabase (Database)      |

## System Architecture

ระบบใช้ **Layered Architecture** โดยแต่ละ request ต้องไหลตามลำดับและห้าม Controller เรียก Repository โดยตรง

```text
Presentation Layer (React UI / REST Controller)
                              |
                              v
Service Layer (Business Logic / Validation / Transaction)
                              |
                              v
Repository Layer (Spring Data JPA / Data Access)
                              |
                              v
Domain Layer (Entity / Value Object / Enum)
                              |
                              v
                         PostgreSQL
```

ใช้ DTO และ Mapper แยก API contract ออกจาก JPA Entity และใช้ Constructor Injection สำหรับ dependency ทั้งหมด

## Database Design (ER Diagram)

ฐานข้อมูล MVP ประกอบด้วย 7 ตารางหลัก ได้แก่ `users`, `user_profiles`, `addresses`, `clients`, `projects`, `tasks` และ `time_entries` ซึ่งครบจำนวนขั้นต่ำตามข้อกำหนดรายวิชา โดย `addresses` เป็นตารางกลางสำหรับข้อมูลที่อยู่ของ User Profile และ Client

ความสัมพันธ์หลัก:

- One-to-One: `users` → `user_profiles`
- `user_profiles` เก็บ field ที่อยู่โดยตรงตาม Data Dictionary; `clients` ยังใช้ `addresses` ในช่วง migration ของ Client
- One-to-Many: `clients` → `projects`
- One-to-Many: `projects` → `tasks` และ `time_entries`
- ตาราง `invoices`, `invoice_items`, `payments` และ `transactions` จะเพิ่มใน Post-MVP

ดู schema และความสัมพันธ์ฉบับออกแบบได้ที่ [ER Diagram](doc/diagrams/er-diagram.md)
และโครงสร้าง JPA Entity ที่ [Domain Class Diagram](doc/diagrams/class-diagram.md)

ดูรายละเอียด field และ constraint ที่ [Data Dictionary](doc/data-dictionary.md)

## Installation & Setup

### Prerequisites

- Git
- Docker Desktop หรือ Docker Engine พร้อม Docker Compose
- JDK 17 กรณีต้องการรันโดยไม่ใช้ Docker

### Clone Repository

```bash
git clone https://github.com/lillianxhub/freelance-hub.git
cd freelance-hub/code
```

### Environment Variables

Backend ใช้ `application.properties` เป็น config กลาง ส่วน Frontend ใช้ Vite environment variables และ Docker Compose ของ Backend โหลดค่าฐานข้อมูล dev จาก `code/Backend/.env`

PowerShell:

```powershell
cd code/Backend
Copy-Item .env.example .env
```

macOS/Linux:

```bash
cd code/Backend
cp .env.example .env
```

| Variable                     | Description                         | Development Default   |
| ---------------------------- | ----------------------------------- | --------------------- |
| `POSTGRES_DB`                | ชื่อฐานข้อมูลสำหรับ Docker          | `freelance_hub`       |
| `POSTGRES_USER`              | ผู้ใช้ PostgreSQL สำหรับ Docker     | `freelance_hub`       |
| `POSTGRES_PASSWORD`          | รหัสผ่าน PostgreSQL สำหรับ Docker   | ต้องเปลี่ยนค่า        |
| `SPRING_JPA_DEFAULT_SCHEMA`  | Schema ของ JPA และ Flyway           | `public`              |
| `SPRING_DATASOURCE_URL`      | JDBC URL สำหรับ production          | Secret ของ deployment |
| `SPRING_DATASOURCE_USERNAME` | Database username สำหรับ production | Secret ของ deployment |
| `SPRING_DATASOURCE_PASSWORD` | Database password สำหรับ production | Secret ของ deployment |
| `JWT_SECRET`                 | Secret สำหรับลงนาม JWT              | Secret ของ deployment |
| `CORS_ALLOWED_ORIGINS`       | Origin ที่อนุญาตสำหรับ CORS และ refresh/logout คั่นด้วย comma | `http://localhost:5173,http://localhost:8080` |
| `REFRESH_COOKIE_SECURE`      | เปิด Secure cookie เมื่อใช้ HTTPS | `false` ใน Docker local; `true` บน Render |
| `OPENAPI_ENABLED`            | เปิด OpenAPI/Swagger เฉพาะ local, dev หรือ staging; production ให้ใช้ `false` หรือไม่กำหนด | `true` ผ่าน Docker Compose |

ห้าม commit ไฟล์ `.env` หรือ production credentials ลง Git

Flyway จะรัน migration อัตโนมัติเมื่อ application container เริ่มทำงาน โดยอ่านไฟล์จาก
`src/main/resources/db/migration` ที่บรรจุอยู่ใน JAR ไม่ต้องติดตั้ง Flyway CLI

## How to Run

### Local Development ด้วย Docker Compose

ต้องติดตั้ง Docker Desktop และสร้างไฟล์ `code/Backend/.env` จาก `.env.example` ก่อน จากนั้นรัน Backend จากโฟลเดอร์ `code/Backend/`:

```bash
docker compose up --build
```

เมื่อเริ่มระบบแล้ว:

- Backend API: <http://localhost:8080>
- PostgreSQL: `localhost:5432`
- Flyway จะรัน migration ที่ยังไม่เคยรันโดยอัตโนมัติก่อน Backend เริ่มทำงาน

รัน Frontend แยกจากโฟลเดอร์ `code/Frontend/`:

```bash
npm install
npm run dev
```

Vite จะเปิดที่ <http://localhost:5173>

รันแบบ background:

```bash
docker compose up --build -d
```

ดู log ของ application:

```bash
docker compose logs -f app
```

หยุด container โดยเก็บข้อมูล PostgreSQL ไว้:

```bash
docker compose down
```

### Seed ข้อมูลตัวอย่างสำหรับ Local

Java seeder สร้างข้อมูลตัวอย่าง User → Client → Project → Task ใน PostgreSQL ของ Docker สำหรับทดลองใช้งานเท่านั้น ไม่ทำงานตอนเปิด Backend ตามปกติ และรันซ้ำได้โดยไม่สร้างข้อมูลซ้ำ Seeder ยอมต่อฐานข้อมูลเฉพาะ `localhost`, `127.0.0.1` หรือ service `postgres` ใน Docker Compose และไม่ทำงานใน CI/Render

จากโฟลเดอร์ `code/Backend/` ให้ build และเปิด app เวอร์ชันล่าสุดก่อน เพื่อให้โค้ดตรงกับ Flyway migration ที่รันแล้ว:

```bash
docker compose up -d postgres
docker compose build app
docker compose up -d --no-deps app
```

ตั้งรหัสผ่านสำหรับบัญชีทดสอบอย่างน้อย 8 ตัวอักษร (ใช้เฉพาะ local):

```powershell
# Windows PowerShell
$env:LOCAL_SEED_PASSWORD = 'your-local-test-password'
```

```bash
# macOS / Linux
export LOCAL_SEED_PASSWORD='your-local-test-password'
```

จากนั้นใช้คำสั่งเดียวกันทุกระบบ:

```bash
docker compose run --rm --no-deps -e LOCAL_SEED_RUN=true -e LOCAL_SEED_PASSWORD app
```

บัญชีตัวอย่างใช้ email `local.seed@example.test` (หรือกำหนด `LOCAL_SEED_EMAIL` ก่อนรัน) และรหัสผ่านจาก `LOCAL_SEED_PASSWORD` โดย Seeder จะไม่แสดงรหัสผ่านใน log และจะหยุดหาก email เดิมมีรหัสผ่านไม่ตรงกัน ชุดข้อมูลประกอบด้วย profile, clients, projects, tasks และ time entries สำหรับทดสอบหน้ารายการและตัวกรอง การรันซ้ำจะอ้างอิง ID เดิมและไม่เขียนทับข้อมูลที่แก้จาก UI

ปกติ Seeder จะไม่สร้าง timer ที่กำลังทำงาน หากต้องการลอง Stop/Cancel ให้ตั้ง `LOCAL_SEED_RUNNING_TIMER=true` ก่อนรัน โดยจะไม่สร้าง timer ซ้ำหากบัญชีนี้มี timer เปิดอยู่แล้ว Seeder ใช้ได้เฉพาะ PostgreSQL local ที่ผ่านการตรวจ host และสร้างตาราง mapping เฉพาะฐานข้อมูล local เท่านั้น ห้ามใช้ข้อมูลรับรอง production

## API Documentation

OpenAPI และ Swagger UI ปิดเป็นค่าเริ่มต้นใน application configuration เพื่อไม่ให้เปิดบน production
โดยตั้ง `OPENAPI_ENABLED=true` เฉพาะ local development หรือ staging เท่านั้น

- Base URL: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

| Resource     | Endpoint             |
| ------------ | -------------------- |
| Register     | `/api/auth/register` |
| Login        | `/api/auth/login`    |
| Refresh token | `/api/auth/refresh` |
| Logout       | `/api/auth/logout`   |
| My profile   | `GET/PATCH /api/users/me` |
| Change password | `/api/users/me/password` |
| Clients      | `/api/clients`       |
| Projects     | `/api/projects`      |
| Time entries | `/api/time-entries`  |
| Timer        | `/api/timer`         |
| Analytics    | `/api/analytics`     |

Income, Expense, Invoice และ Payment เป็น **Post-MVP** และยังไม่มี endpoint ในขอบเขต MVP

ข้อมูลที่อยู่ของ User Profile และ Client ใช้ API contract เดียวกันและส่งเป็น flat fields
โดยเก็บฟิลด์ที่อยู่ไว้ใน `user_profiles` และ `clients`:

```json
{
  "address": "ที่อยู่",
  "subdistrict": "ตำบล",
  "district": "อำเภอ",
  "province": "จังหวัด",
  "postalCode": "รหัสไปรษณีย์"
}
```

การเปลี่ยนรหัสผ่านใช้ `PATCH /api/users/me/password` พร้อม bearer JWT และ body
`{"oldPassword":"รหัสผ่านเดิม","newPassword":"รหัสผ่านใหม่"}`; MVP ยังไม่มี forgot/reset password
หากรหัสผ่านเดิมผิด API ตอบ `401` พร้อมข้อความ `รหัสผ่านไม่ถูกต้อง`

การสมัครและ login จะตัดช่องว่างหัวท้ายและแปลง email เป็นตัวพิมพ์เล็กก่อนค้นหา/บันทึก
`PATCH /api/users/me` เปลี่ยนเฉพาะฟิลด์ที่ส่งมา (ไม่ใช้ `PUT`)
หาก login ผิดเกิน 10 ครั้งต่อ email ภายใน 15 นาที หรือเกิน 300 ครั้งต่อนาทีต่อ IP
API ตอบ `429` พร้อม `Retry-After` และ `LOGIN_RATE_LIMITED`; ตัวนับอยู่ในหน่วยความจำ
ของ Backend instance เดียวและจะเริ่มใหม่เมื่อ restart

Access JWT มีอายุ 15 นาทีและส่งใน `data.token`; refresh token อยู่ใน cookie
`fh_refresh` (`HttpOnly`, `SameSite=Lax`, `Secure` บน HTTPS) มีอายุสูงสุด 7 วัน
`POST /api/auth/refresh` หมุน cookie และคืน access token ใหม่ การ logout เพิกถอน
refresh-token family และตอบ `204`; access token เดิมอาจใช้ต่อได้จนหมดอายุ (ไม่เกิน 15 นาที)
และไม่มีการอัปโหลดหรือเปลี่ยนรูปโปรไฟล์
Refresh/logout ต้องมี `Origin` ที่อนุญาต หรือ `Referer` ที่อนุญาตเมื่อไม่มี `Origin`;
หากไม่มีทั้งคู่หรือไม่ตรง API ตอบ `403`

รายละเอียด API และ business rules อยู่ใน [REQUIREMENTS.md](REQUIREMENTS.md)

## How to Run Tests

รัน Backend automated tests จาก `code/Backend/`:

Windows:

```powershell
.\mvnw.cmd test
```

macOS/Linux:

```bash
./mvnw test
```

สร้าง package พร้อมรัน tests:

```bash
npm --prefix ../Frontend run build
./mvnw clean verify
```

Maven test result อยู่ใน `code/Backend/target/surefire-reports/` และ Frontend build ต้องผ่าน `npm run build` ส่วน test plan, exported report และหลักฐานการทดสอบสำหรับส่งงานเก็บใน `test/reports/`

## Deployment URL

| Environment          | URL                                                      | Status       |
| -------------------- | -------------------------------------------------------- | ------------ |
| Frontend (Vercel)    | `https://freelance-hub-self.vercel.app/`                  | HTTP 200 checked 2026-09-29 |
| Backend staging (Render) | `https://freelance-hub-backend-staging.onrender.com`  | `/actuator/health` returned `UP` 2026-09-29 |
| Backend production (Render) | `https://freelance-hub-g8y5.onrender.com` | URL confirmed by owner; health check timed out 2026-09-29 |
| Swagger UI (Staging) | `https://freelance-hub-backend-staging.onrender.com/swagger-ui.html` | HTTP 200 checked 2026-09-29 |

Frontend ให้ deploy บน Vercel โดยกำหนด Root Directory เป็น `code/Frontend`, Build Command เป็น `npm run build` และ Output Directory เป็น `dist` พร้อมตั้ง `BACKEND_ORIGIN` เป็น HTTPS origin ของ Render (ไม่มี `/` ปิดท้าย) ทั้ง Preview และ Production โดยช่วงทดสอบใช้ Render staging เดียวกัน เพื่อให้ Vercel proxy `/api` ไป Backend ผ่าน `code/Frontend/vercel.ts`; ไม่ตั้ง `VITE_API_BASE_URL` เป็น URL ข้ามโดเมน
สำหรับการทดสอบ staging ให้ตั้ง `BACKEND_ORIGIN=https://freelance-hub-backend-staging.onrender.com`;
เมื่อจะใช้ Backend production ให้เปลี่ยนเป็น `https://freelance-hub-g8y5.onrender.com` และ redeploy Vercel

Backend ให้ deploy บน **Render Web Service** โดยกำหนด Root Directory เป็น `code/Backend`, Runtime เป็น Docker และใช้ `Dockerfile` ของ Backend พร้อม environment variables สำหรับ Supabase

Production ใช้ Supabase PostgreSQL และกำหนด `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` และ `JWT_SECRET` ผ่าน secret/environment settings ของ Backend provider ห้ามใส่ service role key ใน Frontend
ตั้ง `OPENAPI_ENABLED=true` เฉพาะ Render staging; production ให้ตั้ง `OPENAPI_ENABLED=false` หรือไม่กำหนด เพื่อปิด `/v3/api-docs` และ Swagger UI
ตั้ง `REFRESH_COOKIE_SECURE=true` บน Render ที่ใช้ HTTPS และกำหนด `CORS_ALLOWED_ORIGINS`
เป็น origin แบบเต็มที่คั่นด้วย comma สำหรับ staging เช่น
`https://freelance-hub-self.vercel.app,https://freelance-hub-backend-staging.onrender.com`
(ส่วน Backend production ใช้ `https://freelance-hub-self.vercel.app,https://freelance-hub-g8y5.onrender.com`)
(รวม Vercel Preview origins ที่ใช้จริง) เพื่อให้ refresh/logout และ Swagger staging ทำงาน;
local ใช้ `http://localhost:5173,http://localhost:8080` และ Docker ใช้ค่า cookie `false` จาก `.env.example`
ก่อน deploy migration V19 ให้สำรองฐานข้อมูลและตรวจ email ที่ซ้ำกันหลัง `lower(btrim(email))`;
หากพบต้องแก้เป็นรายบัญชีก่อน เพราะ migration จะหยุดโดยไม่รวมบัญชีอัตโนมัติ

```sql
SELECT lower(btrim(email)) AS canonical_email, count(*) AS account_count
FROM users
GROUP BY lower(btrim(email))
HAVING count(*) > 1;
```

> **TODO:** ตรวจ end-to-end login/refresh/logout บน staging หลัง deploy โค้ดล่าสุด,
> ตรวจ production health อีกครั้ง, ระบุ Supabase project และขั้นตอน deploy จริงก่อนส่งงาน

## Project Structure

```text
freelance-hub/
├── code/
│   ├── Backend/                  # Spring Boot REST API
│   │   ├── .mvn/
│   │   ├── src/
│   │   │   ├── main/
│   │   │   ├── java/th/ac/kku/freelance_hub/
│   │   │   │   ├── config/
│   │   │   │   ├── controller/
│   │   │   │   ├── service/
│   │   │   │   ├── repository/
│   │   │   │   ├── domain/entity/
│   │   │   │   ├── domain/enums/
│   │   │   │   ├── domain/valueobject/
│   │   │   │   ├── dto/request/
│   │   │   │   ├── dto/response/
│   │   │   │   ├── event
│   │   │   │   ├── mapper/
│   │   │   │   ├── exception/
│   │   │   │   └── security/
│   │   │   └── resources/
│   │   │       ├── db/migration/
│   │   │       ├── static/
│   │   │       └── templates/
│   │   │   └── test/
│   │   ├── Dockerfile
│   │   ├── docker-compose.yml
│   │   ├── .dockerignore
│   │   ├── .env.example
│   │   ├── pom.xml
│   │   └── mvnw
│   └── Frontend/                 # React + Vite SPA
│       ├── src/
│       ├── public/
│       ├── package.json
│       ├── package-lock.json
│       └── vite.config.js
├── test/                         # Test plan, reports, and evidence
├── doc/                          # Documents, diagrams, and slides
│   ├── diagrams/
│   └── slide/
├── img/                          # Images and media
├── README.md
└── REQUIREMENTS.md
```

## เอกสารโครงการ

- [Software Requirements Specification](REQUIREMENTS.md)
- [MVP Team Schedule](SCHEDULE.md)
- [SOLID Analysis](doc/solid-analysis.md)
- [Design Patterns](doc/design-patterns.md)
- [Data Dictionary](doc/data-dictionary.md)
- [Use Case Description](doc/use-case-description.md)
- [Diagram Checklist](doc/diagrams/README.md)

## Git Workflow

- `main`: production และ version ที่ส่งมอบ
- `develop`: integration branch
- branch ส่วนตัว: `ชื่อ_รหัสนักศึกษา_section`
- รวมงานผ่าน Pull Request และมี reviewer อย่างน้อย 1 คน
- สมาชิกแต่ละคนต้องมี meaningful commits อย่างน้อย 15 ครั้งและใช้บัญชี GitHub ของตนเอง

Commit message format: `<type>: <สิ่งที่ทำ>`

ตัวอย่าง `feat: add time tracking API`, `fix: prevent duplicate timer`, `test: add project service tests` และ `docs: update ER diagram`

## License

TODO: เลือก License และเพิ่มไฟล์ `LICENSE` ก่อนเผยแพร่โครงการ

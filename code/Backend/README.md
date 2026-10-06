# Freelance Hub Backend

Spring Boot REST API สำหรับจัดการบัญชีผู้ใช้ ลูกค้า โปรเจกต์ งานย่อย เวลา Dashboard และ Reports ของ Freelancer แต่ละบัญชี Backend ใช้ PostgreSQL, Spring Data JPA และ Flyway โดยแยก Controller, Service, Repository, Domain และ DTO/Mapper ตาม Layered Architecture

ขอบเขต MVP ยังไม่รวม Invoice, Payment หรือระบบบัญชี ดูภาพรวมทีมและ deployment URL ใน [README หลัก](../../README.md) และข้อกำหนดใน [REQUIREMENTS.md](../../REQUIREMENTS.md)

## สิ่งที่ต้องติดตั้ง

- Docker Engine/Desktop พร้อม Docker Compose สำหรับรัน Backend และ PostgreSQL พร้อมกัน
- หรือ JDK 17 และ PostgreSQL สำหรับรัน Backend บนเครื่อง
- โปรเจกต์มี Maven Wrapper (`mvnw` / `mvnw.cmd`) ไม่ต้องติดตั้ง Maven แยก

## ตั้งค่า Local Environment

จากโฟลเดอร์ `code/Backend` คัดลอกไฟล์ตัวอย่างแล้วเปลี่ยน `POSTGRES_PASSWORD` กับ `JWT_SECRET` ก่อนเริ่มระบบ:

```bash
cp .env.example .env
```

บน Windows PowerShell ใช้ `Copy-Item .env.example .env` แทน ห้าม commit `.env` หรือ secret สำหรับ production

| ตัวแปร | หน้าที่ | ค่า local ที่เกี่ยวข้อง |
| --- | --- | --- |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | ฐานข้อมูลใน Docker Compose | ดู `.env.example`; เปลี่ยนรหัสผ่านตัวอย่าง |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | ต่อ PostgreSQL เมื่อรัน Backend แยกหรือบน cloud | local ใช้ `localhost:5432`; cloud ใช้ค่าจาก provider |
| `JWT_SECRET` | ลงนาม access JWT | ต้องกำหนดค่าให้ปลอดภัย |
| `JWT_EXPIRATION`, `REFRESH_EXPIRATION_MS` | อายุ token หน่วยมิลลิวินาที | ค่าเริ่มต้น 15 นาทีและ 7 วัน |
| `CORS_ALLOWED_ORIGINS` | origin ที่อนุญาตสำหรับ CORS และ cookie actions | local: `http://localhost:5173,http://localhost:8080` |
| `REFRESH_COOKIE_SECURE` | กำหนด Secure flag ของ refresh cookie | `false` เฉพาะ HTTP local; `true` บน HTTPS |
| `OPENAPI_ENABLED` | เปิด Swagger UI/OpenAPI | ค่าใน `.env.example` เป็น `true`; ค่าเริ่มต้นของแอปเป็น `false` |
| `SPRING_JPA_DEFAULT_SCHEMA`, `SPRING_FLYWAY_SCHEMAS` | schema ที่ JPA และ Flyway ใช้ | `public` |

ค่าทั้งหมดอ่านจาก [application.properties](src/main/resources/application.properties) และตัวอย่างอยู่ใน [.env.example](.env.example)

## รัน Backend

วิธีที่แนะนำสำหรับ local คือรันจาก `code/Backend`:

```bash
docker compose up --build
```

Compose เปิด PostgreSQL และ Backend ที่ `http://localhost:8080` ตาม port ค่าเริ่มต้น รอให้ PostgreSQL health check ผ่านก่อนเปิด Backend และ Flyway จะรัน migration ก่อนเริ่มรับ request

คำสั่งที่ใช้บ่อย:

```bash
docker compose up --build -d
docker compose logs -f app
docker compose down
```

`docker compose down` เก็บ volume ฐานข้อมูลไว้ หากจะรันบนเครื่องแทน container ให้เปิด PostgreSQL ก่อน จากนั้นใช้:

```bash
./mvnw spring-boot:run
```

Windows ใช้ `.\mvnw.cmd spring-boot:run` สคริปต์ `application.properties` โหลด `.env` ในโฟลเดอร์นี้แบบ optional เมื่อรันจาก `code/Backend`

### ข้อมูลตัวอย่างสำหรับ local

หลังเปิด PostgreSQL แล้ว รัน `./seed-local.sh` หรือ `bash ./seed-local.sh` เพื่อสร้างข้อมูลตัวอย่าง user, profile, clients, projects, tasks และ time entries ค่าเริ่มต้นคือ `local.seed@example.com` / `localseed1234` สำหรับ local เท่านั้น เปลี่ยนด้วย `LOCAL_SEED_EMAIL` และ `LOCAL_SEED_PASSWORD` ได้ Seeder ไม่ทำงานเองเมื่อเริ่ม Backend ตามปกติและไม่ควรใช้กับฐานข้อมูล production

## โครงสร้างและฐานข้อมูล

```text
src/main/java/th/ac/kku/freelance_hub/
├── controller/       # REST endpoints และ HTTP response
├── service/          # Use cases, business rules, transactions
│   └── impl/
├── repository/       # Spring Data JPA และ report queries
├── domain/           # Entity, enum, value object
├── dto/              # Request และ response contracts
├── mapper/           # แปลง Entity กับ DTO
├── security/         # JWT, refresh cookie, origin validation, login limit
├── config/           # Security, CORS, OpenAPI
├── exception/        # Error handling
└── common/response/  # รูปแบบ API response กลาง
```

Flyway migrations อยู่ใน [src/main/resources/db/migration](src/main/resources/db/migration/) หลัง migration ล่าสุดมี 7 ตารางหลัก: `users`, `user_profiles`, `refresh_tokens`, `clients`, `projects`, `tasks` และ `time_entries` ข้อมูลที่อยู่ของ user profile และ client ใช้ `Address` แบบ `@Embedded` ในตารางเดิม ตาราง `addresses` รุ่นเก่าถูกลบใน V13 และ `revoked_tokens` ถูกแทนด้วย `refresh_tokens` ใน V17–V18 ดู [ER Diagram](../../doc/diagrams/er-diagram.md) และ [Data Dictionary](../../doc/data-dictionary.md)

## API และ Authentication

เมื่อตั้ง `OPENAPI_ENABLED=true` เปิด Swagger UI ที่ `http://localhost:8080/swagger-ui.html` หรือ OpenAPI JSON ที่ `http://localhost:8080/v3/api-docs` ค่าเริ่มต้นของแอปปิดสอง endpoint นี้สำหรับ production

| ส่วน | Endpoint หลัก |
| --- | --- |
| Auth | `POST /api/auth/register`, `/login`, `/refresh`, `/logout` |
| User | `GET/PATCH /api/users/me`, `PATCH /api/users/me/password` |
| Clients | `/api/clients`, `/api/clients/{id}` |
| Projects และ Tasks | `/api/projects`, `/api/projects/{id}`, `/api/projects/{projectId}/tasks`, `/api/tasks/{taskId}` |
| Time Entries และ Timer | `/api/time-entries`, `/api/time-entries/summary`, `/api/timer/{start,current,stop}` |
| Dashboard และ Reports | `GET /api/dashboard`, `/api/dashboard/activity`, `/api/reports/{summary,distribution,projects,work-trend,work-pattern}` |

Protected endpoints รับ `Authorization: Bearer <access-token>` Access JWT มีอายุเริ่มต้น 15 นาที; refresh token อยู่ใน cookie `fh_refresh` แบบ HttpOnly และหมุนเมื่อเรียก `/api/auth/refresh` การ logout เพิกถอน refresh-token family ส่วน access JWT ที่ออกแล้วอาจใช้ต่อได้จนหมดอายุ การ refresh/logout ต้องมี `Origin` หรือ `Referer` ที่อยู่ใน allowlist และ login ถูกจำกัดจำนวนครั้งต่อ email/IP

API ใช้รูปแบบ response กลาง การตรวจ request ด้วย Bean Validation และ exception handler ดูรายละเอียด Report API ที่ [reports-summary-api.md](../../doc/reports-summary-api.md)

## ทดสอบ

จาก `code/Backend`:

```bash
./mvnw verify
```

Windows ใช้ `.\mvnw.cmd verify` ผล JUnit อยู่ใน `target/surefire-reports/` GitHub Actions รัน Maven `verify`, Flyway migrate/validate กับ PostgreSQL ว่าง, build Docker image, health check, ตรวจตาราง และ smoke test endpoint สมัครสมาชิก ก่อนรวมผลเป็น `Backend gate`

## Deploy

Render ใช้ [Dockerfile](Dockerfile) โดยตั้ง environment variables สำหรับ PostgreSQL, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS` และ `REFRESH_COOKIE_SECURE=true` URL staging/production อยู่ใน [README หลัก](../../README.md#deployment-url) GitHub Actions เรียก Render staging deploy hook หลัง push เข้า `dev` และ production deploy hook หลัง push เข้า `main` เมื่อ Backend gate ผ่าน

ก่อน deploy migration V19 กับฐานข้อมูลเดิม ให้สำรองข้อมูลและตรวจ email ที่ซ้ำกันหลัง `lower(btrim(email))` ตาม [migration V19](src/main/resources/db/migration/V19__normalize_user_emails.sql)

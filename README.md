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
| API Documentation | OpenAPI / Swagger UI — TODO: เพิ่ม dependency และ configuration |
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

ฐานข้อมูล MVP ประกอบด้วย 6 ตารางหลัก ได้แก่ `users`, `user_profiles`, `clients`, `projects`, `tasks` และ `time_entries` ซึ่งครบจำนวนขั้นต่ำตามข้อกำหนดรายวิชา

ความสัมพันธ์หลัก:

- One-to-One: `users` → `user_profiles`
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

## API Documentation

- Base URL: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

| Resource     | Endpoint             |
| ------------ | -------------------- |
| Register     | `/api/auth/register` |
| Login        | `/api/auth/login`    |
| Logout       | `/api/auth/logout`   |
| Clients      | `/api/clients`       |
| Projects     | `/api/projects`      |
| Time entries | `/api/time-entries`  |
| Timer        | `/api/timer`         |
| Analytics    | `/api/analytics`     |

Income, Expense, Invoice และ Payment เป็น **Post-MVP** และยังไม่มี endpoint ในขอบเขต MVP

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
| Frontend (Vercel)    | TODO: `https://your-frontend.vercel.app`                 | Not deployed |
| Backend API (Render) | TODO: `https://your-backend.onrender.com`                | Not deployed |
| Swagger UI           | TODO: `https://your-backend.example.com/swagger-ui.html` | Not deployed |

Frontend ให้ deploy บน Vercel โดยกำหนด Root Directory เป็น `code/Frontend`, Build Command เป็น `npm run build` และ Output Directory เป็น `dist` พร้อม `VITE_API_URL` ชี้ไปยัง Backend

Backend ให้ deploy บน **Render Web Service** โดยกำหนด Root Directory เป็น `code/Backend`, Runtime เป็น Docker และใช้ `Dockerfile` ของ Backend พร้อม environment variables สำหรับ Supabase

Production ใช้ Supabase PostgreSQL และกำหนด `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` และ `JWT_SECRET` ผ่าน secret/environment settings ของ Backend provider ห้ามใส่ service role key ใน Frontend

> **TODO:** ระบุ Vercel URL, Render URL, Supabase project และขั้นตอน deploy จริงก่อนส่งงาน

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

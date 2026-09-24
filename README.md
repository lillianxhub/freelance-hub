# Freelance Hub

Freelance Hub คือระบบบริหารงานสำหรับ Freelancer ที่รวมการจัดการลูกค้า โปรเจกต์ และงานย่อยไว้ในที่เดียว
ระบบรองรับการจับเวลาทำงานแบบ real-time และ manual entry พร้อมสรุปชั่วโมงการทำงาน
ผู้ใช้สามารถวิเคราะห์เวลาทำงานและดู productivity insights ผ่าน Dashboard
โปรเจกต์พัฒนาด้วย Spring Boot, Thymeleaf และ PostgreSQL ตาม Layered Architecture

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
| Frontend          | React                                                           |
| API Documentation | OpenAPI / Swagger UI — TODO: เพิ่ม dependency และ configuration |
| Testing           | JUnit 5, Mockito, Spring Boot Test                              |
| Version Control   | Git + Github                                                    |
| Deployment        | Supabase + vercel                                               |

## System Architecture

ระบบใช้ **Layered Architecture** โดยแต่ละ request ต้องไหลตามลำดับและห้าม Controller เรียก Repository โดยตรง

```text
Presentation Layer (REST Controller / Web Controller / Thymeleaf)
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

Backend ใช้ `application.properties` เป็น config กลาง ส่วน Docker Compose โหลดค่าฐานข้อมูล dev จาก
`code/Backend/.env` อัตโนมัติ คัดลอกไฟล์ตัวอย่างแล้วเปลี่ยนค่าให้ตรงกับเครื่องของตนเอง

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

ต้องติดตั้ง Docker Desktop และสร้างไฟล์ `code/Backend/.env` จาก `.env.example` ก่อน จากนั้นรันคำสั่งทั้งหมดจากโฟลเดอร์ `code/Backend/`:

```bash
docker compose up --build
```

เมื่อเริ่มระบบแล้ว:

- Web application: <http://localhost:8080>
- PostgreSQL: `localhost:5432`
- Flyway จะรัน migration ที่ยังไม่เคยรันโดยอัตโนมัติก่อนแอปเริ่มทำงาน

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

- Base URL: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

> **TODO:** Swagger/OpenAPI ยังต้องเพิ่มใน `pom.xml` และ configuration ก่อน URL ข้างต้นจะใช้งานได้

| Resource       | Endpoint               |
| -------------- | ---------------------- |
| Authentication | `/api/v1/auth`         |
| Clients        | `/api/v1/clients`      |
| Projects       | `/api/v1/projects`     |
| Time entries   | `/api/v1/time-entries` |
| Timer          | `/api/v1/timer`        |
| Analytics      | `/api/v1/analytics`    |

Income, Expense, Invoice และ Payment เป็น **Post-MVP** และยังไม่มี endpoint ในขอบเขต MVP

รายละเอียด API และ business rules อยู่ใน [REQUIREMENTS.md](REQUIREMENTS.md)

## How to Run Tests

รัน automated tests จาก `code/`:

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
./mvnw clean verify
```

Maven test result อยู่ใน `code/target/surefire-reports/` ส่วน test plan, exported report และหลักฐานการทดสอบสำหรับส่งงานเก็บใน `test/reports/`

## Deployment URL

| Environment | URL                                                  | Status       |
| ----------- | ---------------------------------------------------- | ------------ |
| Production  | TODO: `https://your-app.example.com`                 | Not deployed |
| Swagger UI  | TODO: `https://your-app.example.com/swagger-ui.html` | Not deployed |

การ deploy ให้กำหนด **Root Directory เป็น `code`** ระบบ Cloud จะพบ `Dockerfile`, `pom.xml`, Maven Wrapper และ source code ครบโดยไม่ต้อง deploy `doc/`, `test/` หรือ `img/`

Production ควรใช้ Managed PostgreSQL และกำหนด `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` และ `SPRING_DATASOURCE_PASSWORD` ผ่าน secret/environment settings ของผู้ให้บริการ

> **TODO:** ระบุ Cloud provider, public URL, database provider และขั้นตอน deploy จริงก่อนส่งงาน

## Project Structure

```text
freelance-hub/
├── code/                         # Deployable Spring Boot application
│   ├── .mvn/
│   ├── src/
│   │   ├── main/
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
│   │   └── test/
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── .dockerignore
│   ├── .env.example
│   ├── pom.xml
│   └── mvnw
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

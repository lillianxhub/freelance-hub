# Freelance Hub

<p align="center"><img src="code/Frontend/public/logo-light.svg" alt="Freelance Hub logo" width="220" /></p>

Freelance Hub เป็นเว็บแอปสำหรับ Freelancer ที่จัดการลูกค้า โปรเจกต์ และงานย่อยในที่เดียว
ผู้ใช้จับเวลาทำงานหรือบันทึกย้อนหลัง แล้วดูเวลาที่ใช้เทียบกับเป้าหมายของแต่ละโปรเจกต์ได้
Dashboard และ Reports ช่วยให้เห็นภาระงานและรูปแบบการใช้เวลาโดยไม่ต้องรวบรวมข้อมูลจากหลายเครื่องมือ
ระบบใช้ React, Spring Boot และ PostgreSQL โดยแยกข้อมูลของผู้ใช้แต่ละบัญชี

> สถานะ: มีระบบที่ deploy แล้วสำหรับขอบเขต MVP ด้านการจัดการงานและเวลา ยังไม่รวม Invoice, Payment หรือระบบบัญชี และยังมีข้อจำกัด/หลักฐานที่ต้องตรวจเพิ่มตาม [เอกสารส่งมอบ](doc/README.md)

## สมาชิกกลุ่ม

| ลำดับ | ชื่อ-นามสกุล | รหัสนักศึกษา | Section | Branch | หน้าที่รับผิดชอบ |
| ---: | --- | --- | --- | --- | --- |
| 1 | เพชรภิญโญ ธนศิรินรากร | 673380073-7 | 2 | `petpinyo_673380073-7_02` | Authentication, User, Security และ CI/CD |
| 2 | ถิรวัฒน์ อุจินา | 673380039-7 | 2 | `thirawat_673380039-7_02` | Client Management |
| 3 | กันตวิชญ์ นาคนวล | 673380027-4 | 1 | `kantavit_673380027-4_01` | Project และ Task Management |
| 4 | กรมภัฏ พิริยะ | 673380262-4 | 2 | `kompat_673380262-4_02` | Time Tracking |
| 5 | ณัฏฐดนย์ สาริกา | 673380511-9 | 2 | `nattadol_673380511-9_02` | Dashboard, Analytics และ Frontend |

## Tech Stack

| ส่วน | เทคโนโลยี |
| --- | --- |
| Backend | Java 17, Spring Boot 4.0.0, Spring MVC, Spring Security |
| Build และ ORM | Maven Wrapper, Spring Data JPA / Hibernate |
| Database | PostgreSQL 16, Flyway migrations |
| Frontend | React 19, TypeScript, Vite 8, Tailwind CSS, React Router, Recharts |
| API documentation | OpenAPI / Swagger UI เมื่อเปิด `OPENAPI_ENABLED` |
| Testing | JUnit Jupiter, Mockito, Spring Boot Test, Node.js test runner, ESLint, TypeScript |
| Deployment | Vercel (Frontend), Render (Backend), Supabase PostgreSQL |

## System Architecture

Frontend เรียก REST API ผ่าน path `/api` บน origin เดียวกัน ส่วน Backend แยกชั้นตาม Layered Architecture:

```text
React UI → REST Controller → Service → Repository → PostgreSQL
                             ↕
                    Request/Response DTO + Mapper
```

Controller รับ HTTP request และส่งต่อให้ Service; Service จัดการ business rules และ transaction; Repository เข้าถึงข้อมูลผ่าน JPA โดย Controller ไม่เรียก Repository ตรง ๆ Spring Security ตรวจ access JWT ก่อนเข้า endpoint ที่ต้องล็อกอิน และ Flyway จัดการ schema เมื่อ Backend เริ่มทำงาน

เอกสารสถาปัตยกรรมเพิ่มเติม: [Class Diagram](doc/diagrams/class-diagram.md), [Domain Model](doc/diagrams/domain-model.md), [Deployment Diagram](doc/diagrams/deployment-diagram.md) และ [ดัชนีเอกสารฉบับรวม](doc/README.md) ส่วนต้นฉบับรายบุคคลยังคงไว้ใน `doc/V1/`

## Database Design (ER Diagram)

Schema ปัจจุบันหลัง Flyway migration มี 7 ตารางหลัก: `users`, `user_profiles`, `refresh_tokens`, `clients`, `projects`, `tasks` และ `time_entries`

| ความสัมพันธ์ | การใช้งาน |
| --- | --- |
| `users` 1:0..1 `user_profiles` | Schema อนุญาตไม่เกินหนึ่งโปรไฟล์; registration สร้างโปรไฟล์ให้ผู้ใช้ |
| `users` 1:N `refresh_tokens` และ `clients` | session และลูกค้าที่ผู้ใช้เป็นเจ้าของ |
| `clients` 1:N `projects` | โปรเจกต์ของลูกค้า |
| `projects` 1:N `tasks` และ `time_entries` | งานย่อยและเวลาที่บันทึก |

ข้อมูลที่อยู่ของ `user_profiles` และ `clients` ใช้ `Address` value object แบบ `@Embedded` และเก็บเป็นคอลัมน์ในตารางนั้น ๆ; migration V13 ลบตาราง `addresses` เดิมแล้ว ข้อมูลสัมพันธ์ด้วย foreign key และ index ตาม [ER Diagram](doc/diagrams/er-diagram.md) และ [Data Dictionary](doc/data-dictionary.md) ส่วน migration อยู่ใน [db/migration](code/Backend/src/main/resources/db/migration/)

## Installation & Setup

ต้องมี Git, Docker Engine/Desktop พร้อม Docker Compose และ Node.js 22 (ตรงกับ Frontend CI) สำหรับรัน Frontend หากรัน Backend โดยไม่ใช้ Docker ให้ติดตั้ง JDK 17 ด้วย โปรเจกต์มี Maven Wrapper จึงไม่จำเป็นต้องติดตั้ง Maven แยก

```bash
git clone https://github.com/lillianxhub/freelance-hub.git
cd freelance-hub
cp code/Backend/.env.example code/Backend/.env
```

บน Windows PowerShell ใช้ `Copy-Item code/Backend/.env.example code/Backend/.env` แทน `cp` แล้วแก้ `POSTGRES_PASSWORD` และ `JWT_SECRET` ใน `.env` ก่อนรัน อย่า commit `.env` หรือ production credentials

| ตัวแปร | ใช้สำหรับ | ค่า local ใน `.env.example` |
| --- | --- | --- |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | ฐานข้อมูล Docker | `freelance_hub`, `freelance_hub`, รหัสผ่านตัวอย่างที่ต้องเปลี่ยน |
| `JWT_SECRET` | ลงนาม access JWT | ค่า local ตัวอย่างที่ต้องเปลี่ยน |
| `CORS_ALLOWED_ORIGINS` | origin ที่อนุญาต | `http://localhost:5173,http://localhost:8080` |
| `REFRESH_COOKIE_SECURE` | cookie ผ่าน HTTPS | `false` สำหรับ local; `true` สำหรับ HTTPS |
| `OPENAPI_ENABLED` | เปิด Swagger UI | `true` ใน `.env.example`; ค่าเริ่มต้นของแอปคือ `false` |
| `VITE_API_BASE_URL` | path ของ Frontend API | `/api` |
| `BACKEND_ORIGIN` | Backend ที่ Vite/Vercel proxy ไป | local: `http://localhost:8080`; บน Vercel ใช้ HTTPS origin ของ Render |

ตัวแปรฐานข้อมูลสำหรับ Backend ที่รันแยกหรือบน Render ได้แก่ `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` และ `SPRING_DATASOURCE_PASSWORD` ดูค่าอื่นได้ใน [application.properties](code/Backend/src/main/resources/application.properties) และ [.env.example](code/Backend/.env.example)

## How to Run

จาก root ของ repository เปิด Backend และ PostgreSQL ด้วย Docker Compose:

```bash
cd code/Backend
docker compose up --build
```

Backend อยู่ที่ `http://localhost:8080` และ PostgreSQL เปิดที่ port `5432` ตามค่าเริ่มต้น Flyway จะรัน migration ก่อนแอปรับ request หากต้องการรัน Backend บนเครื่องแทน Docker ให้เปิด PostgreSQL และตั้ง environment variables ให้ตรงกับฐานข้อมูลก่อนใช้ `./mvnw spring-boot:run` จาก `code/Backend` (Windows PowerShell ใช้ `.\mvnw.cmd spring-boot:run`)

เปิดอีก terminal แล้วรัน Frontend:

```bash
cd code/Frontend
npm ci
npm run dev
```

เปิด `http://localhost:5173` และสมัครบัญชีใหม่ Vite proxy `/api` ไป Backend local; Frontend เก็บ access token ในหน่วยความจำ และ refresh token อยู่ใน HttpOnly cookie หากใช้ `docker compose up -d` สามารถดู log ด้วย `docker compose logs -f app` และหยุดด้วย `docker compose down` โดย volume PostgreSQL ยังอยู่

### ข้อมูลตัวอย่างสำหรับ local

จาก `code/Backend` รัน `./seed-local.sh` (หรือ `bash ./seed-local.sh`) หลังเปิด PostgreSQL แล้ว สคริปต์ใช้ `local.seed@example.com` / `localseed1234` เป็นค่าเริ่มต้นสำหรับ local เท่านั้น เปลี่ยนได้ด้วย `LOCAL_SEED_EMAIL` และ `LOCAL_SEED_PASSWORD`; seeder จะไม่ทำงานเองเมื่อเปิด Backend ตามปกติ ดูเงื่อนไขและวิธีใช้ใน [seed-local.sh](code/Backend/seed-local.sh)

## API Documentation

สำหรับ local เมื่อตั้ง `OPENAPI_ENABLED=true` ให้เปิด [Swagger UI](http://localhost:8080/swagger-ui.html) ซึ่ง redirect ไป `/swagger-ui/index.html`; OpenAPI JSON อยู่ที่ `/v3/api-docs` ส่วน Swagger ที่ deploy แล้วอยู่ในหัวข้อ [Deployment URL](#deployment-url) ด้านล่าง โดย production ปิด Swagger/OpenAPI ไว้ตาม configuration ของโครงการ

ทดสอบ endpoint ที่ต้องล็อกอินโดยสมัคร/เข้าสู่ระบบผ่าน Authentication API แล้วนำ access token ใส่ปุ่ม **Authorize** ใน Swagger UI

| ส่วน | Endpoint หลัก |
| --- | --- |
| Authentication | `POST /api/auth/register`, `/login`, `/refresh`, `/logout` |
| User | `GET/PATCH /api/users/me`, `PATCH /api/users/me/password` |
| Clients | `/api/clients`, `/api/clients/{id}` |
| Projects และ Tasks | `/api/projects`, `/api/projects/{id}`, `/api/projects/{projectId}/tasks`, `/api/tasks/{taskId}` |
| Time Tracking | `/api/time-entries`, `/api/time-entries/summary`, `/api/timer/start`, `/api/timer/current`, `/api/timer/stop` |
| Dashboard และ Reports | `GET /api/dashboard`, `GET /api/dashboard/activity`, `GET /api/reports/{summary,distribution,projects,work-trend,work-pattern}` |

Endpoint ที่ต้องล็อกอินใช้ `Authorization: Bearer <access-token>` โดย access JWT มีอายุเริ่มต้น 15 นาที Refresh token อยู่ใน cookie `fh_refresh` (HttpOnly, SameSite=Lax และ Secure บน HTTPS) มีอายุเริ่มต้น 7 วัน อายุ token ปรับได้ผ่าน configuration การ refresh หมุน token ใหม่; logout เพิกถอน refresh-token family และตอบ `204` Access JWT เดิมยังอาจใช้ได้จนหมดอายุ การ refresh/logout ตรวจ `Origin` หรือ `Referer` ที่อนุญาต และ login มี rate limit ต่อ email/IP

API ใช้ response format กลางและ validation; ดู endpoint, business rules และขอบเขต MVP เพิ่มเติมใน [REQUIREMENTS.md](REQUIREMENTS.md) และ [Report API](doc/reports-summary-api.md) ระบบยังไม่มี Invoice, Payment, forgot/reset password หรืออัปโหลดรูปโปรไฟล์

## How to Run Tests

Backend (จาก `code/Backend`):

```bash
./mvnw verify
```

Windows ใช้ `.\mvnw.cmd verify` ผล JUnit อยู่ใน `code/Backend/target/surefire-reports/`

Frontend (จาก `code/Frontend`):

```bash
npm ci
npm run lint
npm run typecheck
npm test
npm run build
```

แผนทดสอบและหลักฐานการทดสอบอยู่ใน [test/](test/README.md) GitHub Actions รัน Backend `verify`, ตรวจ Flyway กับ PostgreSQL, build Docker image และ smoke test endpoint สมัครสมาชิก ส่วน Frontend รัน lint, typecheck, unit tests และ build โดยมี `Backend gate` กับ `Frontend gate` เป็น status checks ดูผลรันที่เก็บไว้และข้อจำกัดใน [Test Report](test/test-report.md) และ [Coverage Report](test/coverage-report.md); ผล local ไม่ใช่หลักฐานว่า CI หรือการทดสอบบนระบบ deploy ผ่านทั้งหมด

## Deployment URL

| จุดเข้าใช้งาน | URL |
| --- | --- |
| เว็บแอป (Vercel) | [เปิด Freelance Hub](https://freelance-hub-self.vercel.app/) |
| API Documentation — staging | [เปิด Swagger UI](https://freelance-hub-backend-staging.onrender.com/swagger-ui/index.html) |

ตรวจการเข้าถึงวันที่ **9 ตุลาคม 2026 (Asia/Bangkok)**: เว็บแอปเปิดหน้าเข้าสู่ระบบได้, Swagger staging โหลดรายการ API และ OpenAPI JSON ได้ และ health endpoint ของ Backend staging/production ตอบ `UP` ส่วน Swagger UI/OpenAPI JSON ของ production ตอบ `404` ตามการปิดเอกสาร API ใน environment นั้น ลิงก์ Swagger ข้างต้นจึงเป็นของ staging ไม่ใช่ production

การตรวจนี้ยืนยันการเข้าถึงหน้าเว็บ/เอกสารและ health เท่านั้น ไม่ใช่ผลทดสอบการสมัครสมาชิก เข้าสู่ระบบ หรือ business flows บน cloud ควรตรวจซ้ำก่อนส่งงานและก่อนนำเสนอ

Vercel ใช้ Root Directory `code/Frontend`, Build Command `npm run build` และ Output Directory `dist` ตั้ง `VITE_API_BASE_URL=/api` และ `BACKEND_ORIGIN` เป็น HTTPS origin ของ Render ที่ต้องการใช้ [vercel.ts](code/Frontend/vercel.ts) จะ proxy `/api` ไป Backend ผ่าน origin เดียวกับ Frontend

Render ใช้ `code/Backend/Dockerfile` และ Supabase PostgreSQL; ตั้ง database credentials, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS` และ `REFRESH_COOKIE_SECURE=true` ใน environment settings GitHub Actions ตรวจ PR เข้า `dev`/`main`; เมื่อ push เข้า `dev` และ Backend gate ผ่านจะเรียก staging deploy hook ส่วน push เข้า `main` จะเรียก production deploy hook Frontend deployment ใช้ Vercel Git integration

ก่อนใช้ migration V19 กับฐานข้อมูลที่มีข้อมูลเดิม ให้สำรองข้อมูลและตรวจ email ที่ซ้ำกันหลัง `lower(btrim(email))` ตาม [migration V19](code/Backend/src/main/resources/db/migration/V19__normalize_user_emails.sql)

## Project Structure

```text
freelance-hub/
├── .github/workflows/           # Backend และ Frontend CI/CD
├── code/
│   ├── Backend/                 # Spring Boot, Docker, Maven Wrapper
│   │   └── src/
│   │       ├── main/java/th/ac/kku/freelance_hub/
│   │       │   ├── controller/  # REST endpoints
│   │       │   ├── service/     # Business logic
│   │       │   ├── repository/  # JPA data access
│   │       │   ├── domain/      # Entity, value object, enum
│   │       │   ├── dto/         # Request และ response contracts
│   │       │   └── mapper/      # Entity ↔ DTO
│   │       ├── main/resources/db/migration/
│   │       └── test/            # JUnit และ Spring tests
│   └── Frontend/                # React + Vite SPA
│       └── src/
│           ├── Authentication/, Profile/
│           ├── ClientManagement/, ProjectManagement/
│           ├── TimeTracking/, Analytics/
│           ├── api/, services/, types/
│           └── components/, hooks/, lib/, utils/
├── doc/                         # เอกสารฉบับรวมสำหรับส่งมอบ
│   ├── README.md               # ดัชนีเอกสารและข้อจำกัดที่ยังต้องตรวจ
│   ├── solid-analysis.md
│   ├── design-patterns.md
│   ├── use-case-description.md
│   ├── V1/                     # ต้นฉบับรายบุคคล: SOLID, DESIGN, USECASE, CICD
│   ├── diagrams/               # รวม Sequence 6 scenarios ใน sequence-diagram.md
│   └── slide/                  # ปัจจุบันมี README; ยังไม่ได้จัดทำสไลด์นำเสนอ
├── test/                         # Test plan และหลักฐาน
├── img/                          # รูปภาพประกอบ
├── README.md
└── REQUIREMENTS.md
```

เอกสารฉบับรวมและสถานะสิ่งส่งมอบดู [doc/README.md](doc/README.md) โดย `solid-analysis.md`, `design-patterns.md` และ `use-case-description.md` ปรับให้ตรงกับ implementation ที่ตรวจล่าสุดแล้ว ไม่ใช่โครงร่าง ส่วน `doc/V1/` เก็บต้นฉบับรายบุคคลไว้เพื่ออ้างอิงที่มา; API contract ปัจจุบันให้อ้างฉบับรวมและ Swagger ไม่ใช่ V1 ทุกไฟล์

## Git Workflow

`main` ใช้สำหรับ production และรุ่นส่งมอบ, `dev` ใช้รวมงาน, ส่วนแต่ละคนทำงานใน branch รูปแบบ `ชื่อ_รหัสนักศึกษา_section` แล้วเปิด Pull Request พร้อม reviewer อย่างน้อย 1 คนก่อนรวมงาน ตั้ง commit message แบบ `<type>: <สิ่งที่ทำ>` เช่น `feat: add report API` หรือ `docs: update README`

ใบงานรายวิชาระบุชื่อ integration branch เป็น `develop` แต่ repository นี้ใช้ `dev` และ workflow ปัจจุบันอ้าง `dev`; ทีมควรยืนยันชื่อ branch ที่อาจารย์ยอมรับก่อนส่งงาน

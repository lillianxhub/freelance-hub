# Freelance Hub

ระบบจัดการงานสำหรับ Freelancer ที่รวมการติดตามเวลาทำงาน การจัดการลูกค้าและโปรเจกต์ การคำนวณรายได้ การออกใบแจ้งหนี้ และ productivity analytics ไว้ในที่เดียว

> **สถานะโครงการ:** อยู่ระหว่างเริ่มต้นพัฒนา ปัจจุบัน repository มี Spring Boot skeleton และเอกสาร requirements ฟีเจอร์ธุรกิจด้านล่างเป็นขอบเขตที่วางแผนไว้สำหรับ MVP

## ฟีเจอร์หลักของ MVP

- จัดการลูกค้าหลายราย โปรเจกต์ และงานย่อย
- จับเวลาแบบ real-time และเพิ่ม time entry ย้อนหลัง
- แยกเวลาที่เรียกเก็บเงินได้และเรียกเก็บไม่ได้
- รองรับงานแบบรายชั่วโมงและ fixed price
- คำนวณรายได้จากเวลาและอัตราค่าจ้าง
- สร้าง invoice จากรายการเวลาและส่งออกเป็น PDF
- ติดตาม invoice แบบ draft, issued, paid, overdue และ void
- dashboard สรุปเวลา รายได้ billable utilization และสถานะโปรเจกต์
- วิเคราะห์ productivity ตามช่วงเวลา ลูกค้า และโปรเจกต์

รายละเอียดทั้งหมดอยู่ใน [REQUIREMENTS.md](./REQUIREMENTS.md)

## Technology Stack

| ส่วน | เทคโนโลยี |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4.2.0-SNAPSHOT |
| Web | Spring MVC / REST API |
| Persistence | Spring Data JPA |
| Database | PostgreSQL |
| Build | Maven Wrapper |
| Test | Spring Boot Test, JPA Test, MVC Test |

## สถาปัตยกรรมที่วางแผนไว้

โครงการใช้แนวทาง **modular monolith** และจัด package ตาม feature เพื่อให้แต่ละ domain แยกจากกันชัดเจน แต่ยัง build และ deploy เป็น application เดียว

```text
src/main/java/th/ac/kku/freelance_hub/
├─ common/          # config, security, exception และ web utilities
├─ auth/            # authentication และ authorization
├─ user/            # profile และ user settings
├─ client/          # ข้อมูลลูกค้า
├─ project/         # โปรเจกต์และ task
├─ timetracking/    # timer และ time entry
├─ invoice/         # invoice, line item, payment และ PDF
├─ finance/         # รายรับและค่าใช้จ่าย
└─ analytics/       # dashboard และรายงาน
```

แต่ละ feature สามารถแบ่งชั้นย่อยเป็น `api`, `application`, `domain` และ `infrastructure` โดยไม่ส่ง JPA entity ออกผ่าน API โดยตรง

## สิ่งที่ต้องติดตั้ง

- JDK 17
- PostgreSQL 15 หรือใหม่กว่า
- Git

ไม่จำเป็นต้องติดตั้ง Maven แยก เนื่องจากโครงการมี Maven Wrapper ให้แล้ว

ตรวจสอบ Java:

```powershell
java -version
```

## การติดตั้งและรันโครงการ

### 1. Clone repository

```powershell
git clone <repository-url>
cd freelance-hub
```

### 2. สร้างฐานข้อมูล

ตัวอย่างด้วย PostgreSQL CLI:

```sql
CREATE DATABASE freelance_hub;
CREATE USER freelance_hub_user WITH PASSWORD 'change-me';
GRANT ALL PRIVILEGES ON DATABASE freelance_hub TO freelance_hub_user;
```

### 3. ตั้งค่าการเชื่อมต่อฐานข้อมูล

ไฟล์ `application.properties` ในปัจจุบันมีเพียงชื่อ application จึงต้องกำหนด datasource ก่อนเริ่มใช้ JPA สามารถตั้งค่าผ่าน environment variables ใน PowerShell ได้ดังนี้:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/freelance_hub"
$env:SPRING_DATASOURCE_USERNAME = "freelance_hub_user"
$env:SPRING_DATASOURCE_PASSWORD = "change-me"
```

ไม่ควร commit รหัสผ่านหรือ secret ลง repository สำหรับ production ควรใช้ secret manager หรือ environment variables ของระบบ deploy

### 4. รัน application

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

macOS/Linux:

```bash
./mvnw spring-boot:run
```

เมื่อเริ่มสำเร็จ application จะให้บริการโดยค่าเริ่มต้นที่ `http://localhost:8080`

> โครงการอ้างอิง Spring Boot รุ่น `4.2.0-SNAPSHOT` จึงอาจต้องเชื่อมต่อ Spring Snapshot Repository ในการดาวน์โหลด dependency ครั้งแรก

## การ Build และทดสอบ

รัน automated tests:

```powershell
.\mvnw.cmd test
```

สร้าง executable JAR:

```powershell
.\mvnw.cmd clean package
```

ไฟล์ที่ build แล้วจะอยู่ภายใน `target/` และรันได้ด้วย:

```powershell
java -jar target/freelance-hub-0.0.1-SNAPSHOT.jar
```

## API ที่วางแผนไว้

REST API ใช้ prefix `/api/v1` โดยแบ่ง resource หลักดังนี้:

| Resource | ตัวอย่าง Endpoint | หน้าที่ |
|---|---|---|
| Authentication | `/api/v1/auth` | สมัครและเข้าสู่ระบบ |
| Clients | `/api/v1/clients` | จัดการลูกค้า |
| Projects | `/api/v1/projects` | จัดการโปรเจกต์และ task |
| Time entries | `/api/v1/time-entries` | บันทึกและค้นหาเวลาทำงาน |
| Timer | `/api/v1/timer` | เริ่ม หยุด และดู timer ปัจจุบัน |
| Invoices | `/api/v1/invoices` | สร้าง ออก ยกเลิก และดาวน์โหลด invoice |
| Analytics | `/api/v1/analytics` | KPI และข้อมูลวิเคราะห์ |
| Reports | `/api/v1/reports` | ส่งออกรายงาน |

Endpoint เหล่านี้เป็น API contract ระดับสูงที่ยังต้องพัฒนา ดูรายละเอียดและ business rules ใน [REQUIREMENTS.md](./REQUIREMENTS.md#8-api-ระดับสูง)

## Business Rules สำคัญ

- ผู้ใช้มี timer ที่กำลังทำงานได้ครั้งละหนึ่งรายการ
- เก็บวันเวลาในฐานข้อมูลเป็น UTC และแสดงตาม timezone ของผู้ใช้
- time entry เก็บ rate snapshot เพื่อไม่ให้การเปลี่ยนราคาแก้รายได้ย้อนหลัง
- time entry หนึ่งรายการต้องไม่ถูกเรียกเก็บซ้ำ
- invoice ที่ออกแล้วเก็บ snapshot ของผู้ขาย ลูกค้า ภาษี และรายการ
- invoice ที่ออกแล้วแก้ยอดไม่ได้ หากต้องแก้ให้ void และสร้างฉบับใหม่
- ยอดเงินต่างสกุลจะไม่ถูกรวมกันโดยไม่มีการแปลงอัตราแลกเปลี่ยน
- ผู้ใช้เข้าถึงได้เฉพาะข้อมูลที่ตนเองเป็นเจ้าของ

## แนวทางการพัฒนา

### ลำดับการพัฒนา

1. **Foundation:** database migration, authentication, user, client และ project
2. **Time Tracking:** task, time entry, timer และการคำนวณรายได้
3. **Invoice & Finance:** invoice lifecycle, payment และ PDF
4. **Analytics:** dashboard, productivity insights, export และ hardening

### หลักการสำหรับโค้ด

- ใช้ Flyway สำหรับเปลี่ยน schema และไม่ใช้ `ddl-auto=update` ใน production
- validation และ authorization ต้องอยู่ที่ server เสมอ
- ใช้ DTO สำหรับ request/response และกำหนด transaction boundary ใน application service
- จำนวนเงินใช้ `BigDecimal` พร้อม ISO 4217 currency code
- เขียน unit test สำหรับ calculation และ integration test สำหรับ workflow หลัก
- ห้าม log password, token, secret หรือข้อมูลการเงินที่ละเอียดอ่อน

## โครงสร้าง repository ปัจจุบัน

```text
freelance-hub/
├─ src/
│  ├─ main/
│  │  ├─ java/th/ac/kku/freelance_hub/
│  │  └─ resources/
│  └─ test/
├─ REQUIREMENTS.md
├─ HELP.md
├─ pom.xml
├─ mvnw
└─ mvnw.cmd
```

## Documentation

- [Software Requirements Specification](./REQUIREMENTS.md)
- [Spring Boot generated help](./HELP.md)

## การมีส่วนร่วม

ก่อนเริ่มพัฒนา feature ใหม่:

1. อ่าน requirement และ acceptance criteria ที่เกี่ยวข้อง
2. สร้าง branch จาก branch หลัก เช่น `feature/time-tracking`
3. เพิ่ม migration และ automated tests ที่จำเป็น
4. รัน `mvnw.cmd test` ให้ผ่านก่อนเปิด pull request
5. อัปเดตเอกสารเมื่อ API หรือ business rule เปลี่ยน

รูปแบบ commit message ที่แนะนำ:

```text
feat: add time entry creation
fix: prevent duplicate running timers
docs: update invoice requirements
test: cover invoice total calculation
```

## License

ยังไม่ได้กำหนด License สำหรับโครงการนี้ ก่อนนำไปเผยแพร่หรือใช้งานภายนอกควรเพิ่มไฟล์ `LICENSE` และระบุเงื่อนไขการใช้งานให้ชัดเจน

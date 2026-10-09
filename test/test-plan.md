# Test Plan - Freelance Hub

ขอบเขต release/source `ca77d74` วันที่ 9 ตุลาคม 2026 ครอบคลุม Authentication/Profile, Client, Project/Task, Time Tracking, Dashboard/Reports และ shared response/security contracts แผนนี้แยก automated tests ที่รันแล้วออกจาก manual/production checks ที่ยังไม่ได้ทำ

ผลจริงและคำสั่งรันดู [Test Report](test-report.md); coverage ดู [Coverage Report](coverage-report.md) การมี test case ในแผนไม่ใช่หลักฐานว่าผ่านแล้ว

## Automated regression matrix

| หมวด | สิ่งที่ตรวจ | Test source หลัก |
|---|---|---|
| Auth/Profile | Register/login, normalized email, password change, refresh rotation/replay, logout และ owner จาก JWT | AuthServiceTest, AuthControllerTest, UserAuthIntegrationTest, UserServiceTest, RefreshTokenServiceTest |
| Security/error contract | ไม่มี/ผิด JWT, origin validation, CORS, login limit, error fields และ trace ID | JwtAuthenticationFailureIntegrationTest, CorsSecurityIntegrationTest, LoginAttemptLimiterTest, ApiErrorContractTest, HttpStatusContractIntegrationTest |
| Client | CRUD/PUT/PATCH, isActive/archive cascade, soft delete, prefix search, one-based page, include และ totalTrackedSeconds | ClientControllerTest, ClientServiceImplTest, ClientRepositoryTest, ClientIntegrationTest, ClientMapperTest |
| Project/Task | Ownership, transition, running timer guard, Task completion, reorder และ Project completion locking | ProjectStateTest, ProjectServiceImplTest, TaskServiceImplTest, ProjectRepositoryTest, TaskRepositoryTest, TaskControllerTest, TaskDetailControllerTest |
| Timer/Time Entry | Start/stop/cancel, manual input, duration seconds, nullable Task, filter bounds, locked/deleted entry และ owner isolation | TimeEntryTest, TimerServiceImplTest, TimeEntryServiceImplTest, TimeEntryRepositoryTest, TimeEntryIntegrationTest |
| Progress events | Threshold 80%/100%, no target, after-commit workflow และ log listener | ProjectProgressThresholdsTest, TimerStoppedProgressListenerTest, ProjectProgressThresholdListenerTest |
| Dashboard/Reports | Empty data, owner isolation, date bounds, filters, aggregation, pagination และ DTO contract | DashboardIntegrationTest, ReportControllerTest, ReportServiceImplTest, ReportIntegrationTest |
| OpenAPI | API schemas/contracts และ production setting ที่ปิด docs | OpenApiContractTest, OpenApiProductionSecurityTest |
| Frontend API/utilities | JWT retry/refresh, error/meta parsing, request deduplication, task mapping, report params, seconds formatting, date filtering, CSV escaping และ Vercel rewrites | src/api/*.test.ts, src/services/*.test.ts, src/lib/*.test.ts, src/utils/*.test.ts |

Backend test source อยู่ใน `code/Backend/src/test/java/th/ac/kku/freelance_hub/`; Frontend test source อยู่ใน `code/Frontend/src/` Tests .tsx ใต้ Analytics ไม่ถูกเลือกโดย npm test script ปัจจุบัน จึงไม่ถือว่ารัน React component tests ทั้งหมดแล้ว

## ขั้นตอนรันซ้ำ

Backend ใช้ JDK 17 ตาม pom/CI และ Maven Wrapper จาก code/Backend:

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress verify
```

ถ้าเครื่องใช้ JDK 23 แบบเครื่องที่จัดรายงานนี้ ใช้ annotation-processing override เฉพาะ command โดยไม่ต้องแก้ pom:

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress "-Dmaven.compiler.proc=full" verify
```

Frontend จาก code/Frontend:

```powershell
npm.cmd ci
npm.cmd run lint
npm.cmd run typecheck
npm.cmd test
npm.cmd run build
```

รัน dependency installation ให้ตรง package-lock.json ก่อนตรวจ gates; อย่าใช้ npm audit fix --force เป็นขั้นตอนของแผนนี้ เพราะอาจเปลี่ยน dependency contract ของทีม

## Manual acceptance / deployment checks ที่ต้องรันเพิ่ม

| ID | Scenario | ขั้นตอนและผลที่คาดหวัง | สถานะรอบนี้ |
|---|---|---|---|
| M-01 | PostgreSQL migrations | ใช้ฐาน PostgreSQL ว่างแบบ disposable รัน V1–V20 แล้ว validate; ตรวจ constraints/indexes และ 7 business tables | ไม่ได้รัน |
| M-02 | Cloud health | เปิด URL ที่กำหนดใน README และ GET /actuator/health; แนบเวลา/environment และผลจริง | ไม่ได้รัน |
| M-03 | Browser session | Local/staging test accounts: register/login/reload/refresh/logout; ตรวจ HttpOnly/SameSite/Secure ตาม environment | ไม่ได้รันด้วย browser |
| M-04 | Cross-account isolation | สร้าง User A/B ในฐานทดสอบ; ใช้ B เรียก Client/Project/Task/Time Entry ของ A ต้องไม่เห็นข้อมูล; รายงานต้องเป็นของ B | API automated tests รันแล้ว; manual ยังไม่ได้รัน |
| M-05 | Client → Project archive | ตั้ง Client inactive แล้วตรวจ Project ที่ยังไม่ deleted เป็น ARCHIVED; เปิด Client ไม่ restore Project; DELETE Client คง isActive | Automated regression รันแล้ว; browser ยังไม่ได้รัน |
| M-06 | Timer concurrency | ส่ง start สองคำขอพร้อมกันของ owner เดียวบน PostgreSQL; ต้องมี running row เดียวและคำขออื่นได้ conflict | ไม่ได้ stress test PostgreSQL |
| M-07 | Completion/locking | ทำ active Tasks ให้เสร็จ หยุด timer แล้วปิด Project; existing entries มี lockedAt และแก้/ลบได้ 409 | H2 integration รันแล้ว; PostgreSQL/browser ยังไม่ได้รัน |
| M-08 | Date/time boundaries | ข้อมูลใกล้เที่ยงคืนและข้ามวัน; เทียบ Bangkok day bounds และการลง duration ตาม startedAt ไม่ใช่ split overlap | Automated regression รันแล้ว; UI ยังไม่ได้รัน |
| M-09 | Reports/CSV | เปลี่ยน Client/Project/date/page/groupBy; CSV ต้องเป็น Project ของหน้าปัจจุบัน และตัวเลขตรงกับกฎของ Reports | API/utilities รันแล้ว; browser download ยังไม่ได้ตรวจ |
| M-10 | Empty UI และ nullable Task | บัญชีไม่มีข้อมูลแสดง 0/empty state; Timer/Entry ที่ไม่มี Task แสดงได้โดยไม่ crash | API tests รันแล้ว; browser/UI ยังไม่ได้รัน |

ใช้ disposable/local test database และ test accounts เท่านั้น ไม่ใช้แผนนี้เป็นการอนุญาต reset/seed production database หรือ deploy ระบบ ใน test profile H2 สร้าง/ลบ schema ใน memory และปิด Flyway จึงไม่ทำให้ข้อมูล PostgreSQL/Supabase ของผู้ใช้หาย

## เกณฑ์สรุปผล

- Automated regression: คำสั่งจบด้วย exit code 0 และไม่มี failing/error cases
- Release verification: ต้องตรวจ Java/Node versions ตาม CI, PostgreSQL migrations, browser acceptance และ public deployment แยกจาก local automated results
- Coverage เป็นผลของ instrumented/executed code เท่านั้น ไม่ใช้จำนวน tests แทนเปอร์เซ็นต์ และไม่ใช้ coverage บางไฟล์อ้างว่า whole-project coverage
- ข้อจำกัด requirement ที่มีอยู่ยังคงระบุใน [Use Case Description](../doc/use-case-description.md); tests ผ่านไม่ทำให้ feature ที่ยังไม่ implement กลายเป็นเสร็จ

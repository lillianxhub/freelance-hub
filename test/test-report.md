# Test Report - Freelance Hub

## ผลล่าสุด: 10 ตุลาคม 2026

รันบน source `0ecd483`; โค้ดและ workflows ตรงกับ `dbcc4b9` ของ branch ปัจจุบัน สภาพแวดล้อม local: OpenJDK 21 (compile target 17), Node 26.10.0 และ npm 11.19.1

| การตรวจ | ผล |
|---|---|
| Backend `./mvnw --offline --batch-mode --no-transfer-progress verify` | ผ่าน 471 tests, ไม่พบ failure/error/skipped |
| Frontend `npm test` | ผ่าน 11 test files |
| Frontend `node --import tsx <test-file>` ทีละไฟล์ | ผ่าน 33 test cases รวม 11 ไฟล์ |
| Frontend `npm run lint`, `npm run typecheck`, `npm run build` | ผ่านทั้งหมด |

Node 26 รายงาน `npm test` เป็นจำนวนไฟล์ จึงรันแต่ละไฟล์แยกเพื่อยืนยันจำนวน test cases รอบนี้ไม่ได้เก็บ coverage หรือทดสอบ PostgreSQL, browser และ cloud

## ผลเดิม: 9 ตุลาคม 2026

ผลทดสอบจริงวันที่ 9 ตุลาคม 2026 บน source `ca77d74` (`thirawat_673380039-7_02`) ก่อนเพิ่มเอกสารชุดนี้ Working tree ไม่มี production-code changes ไม่ใช้ผลจาก branch/commit อื่นแทนผล release นี้

## 1. สภาพแวดล้อม

| รายการ | ค่าในรอบนี้ |
|---|---|
| OS | Windows 11; PowerShell |
| Java / compile target | Oracle JDK 23.0.2; Maven compiler release 17 |
| Maven | Wrapper, Apache Maven 3.9.16 |
| Backend | Spring Boot 4.0.0, JUnit/Mockito/Spring tests ผ่าน Surefire 3.5.4 |
| Test database | H2 in-memory, PostgreSQL compatibility mode; create-drop, Flyway disabled |
| Node / npm | Node.js v24.12.0 / npm 11.11.0 |
| Frontend | Node test runner + tsx; TypeScript; Vite 8.3.0 |
| CI reference | Backend Java 17; Frontend Node 22 ตาม workflows ไม่ใช่ versions ที่รัน local รอบนี้ |

รอบนี้ไม่มีการ reset/seed PostgreSQL หรือ Supabase และไม่ได้ deploy ระบบ การ create-drop เกิดใน H2 test database เท่านั้น

## 2. ผลคำสั่ง

| Check | ผล | รายละเอียด |
|---|---|---|
| Backend Maven verify | PASS, exit 0 | 446 tests, failures 0, errors 0, skipped 0; BUILD SUCCESS และสร้าง executable JAR |
| Frontend npm test | PASS, exit 0 | 30 tests ผ่าน, fail 0, skipped 0 |
| Frontend typecheck | PASS, exit 0 | tsc --noEmit |
| Frontend production build | PASS, exit 0 | Vite build สำเร็จ 3489 modules, 10.62 วินาที |
| Frontend lint | BLOCKED / command exit 1 | Local node_modules ไม่มี @eslint/js; ESLint โหลด configuration ไม่ได้ ยังไม่ใช่ผล lint ของ source |
| Frontend instrumented test run | PASS, exit 0 | 30 tests; coverage เฉพาะ 11 imported production .ts files |
| Backend code coverage | NOT COLLECTED | ไม่มี JaCoCo configuration/agent ในรอบนี้ ไม่ใส่เปอร์เซ็นต์สมมุติ |
| PostgreSQL migrations / Docker smoke / cloud health | NOT RUN | Local Maven H2 tests ไม่แทนการตรวจเหล่านี้ |
| Browser E2E / React component suite | NOT RUN | npm test ปัจจุบันไม่ได้เลือก Analytics .test.tsx และไม่ได้เปิด browser |

ผลรวม automated test cases ที่รัน: Backend 446 + Frontend 30 = 476 cases ผ่าน แต่ **ยังไม่ถือว่า quality gates ทุกตัวผ่าน** เพราะ lint ยังตรวจ source ไม่ได้และ release checks บางชนิดยังไม่ได้รัน

## 3. Backend command และผลแยกชั้น

รันจาก code/Backend:

```powershell
.\mvnw.cmd --offline --batch-mode --no-transfer-progress "-Dmaven.compiler.proc=full" verify
```

ใช้ offline เพราะ dependencies ของ Maven อยู่ใน cache แล้ว ใช้ proc=full สำหรับ Lombok annotation processing บน JDK 23 โดยไม่แก้ pom.xml บนเครื่อง/CI ที่ใช้ Java 17 ให้รัน .\mvnw.cmd verify ตามปกติแล้วบันทึกผล release ซ้ำ

Surefire สรุป `Tests run: 446, Failures: 0, Errors: 0, Skipped: 0`; Maven จบ BUILD SUCCESS เวลา 17:39:07 Asia/Bangkok, ใช้เวลารวม 57.962 วินาที

| Test package/layer | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| common response | 4 | 0 | 0 | 0 |
| controller | 114 | 0 | 0 | 0 |
| domain | 27 | 0 | 0 | 0 |
| dto | 4 | 0 | 0 | 0 |
| event | 3 | 0 | 0 | 0 |
| application context | 1 | 0 | 0 | 0 |
| integration | 51 | 0 | 0 | 0 |
| mapper | 15 | 0 | 0 | 0 |
| repository | 27 | 0 | 0 | 0 |
| security | 17 | 0 | 0 | 0 |
| local seed service | 1 | 0 | 0 | 0 |
| service | 182 | 0 | 0 | 0 |
| รวม | 446 | 0 | 0 | 0 |

อ่าน actual testcase elements ใน Surefire XML 49 report files แล้วได้ 446 ตรงกับ console ไม่บวกแค่ testsuite.tests: TimeEntryServiceImplTest report มี tests=0 ที่ root แต่มี nested Queries testcase 40 รายการ จึงนับรายการจริงและเก็บ declaredTests ไว้ใน evidence สำหรับตรวจที่มา

## 4. Frontend checks

รันจาก code/Frontend:

```powershell
npm.cmd test
npm.cmd run typecheck
npm.cmd run build
npm.cmd run lint
```

Node tests ครอบคลุม API envelope/auth retry/refresh/request deduplication, report filters/pagination, Task mapping/reopen/reorder, Time Entry mapping, graph seconds/month labels, date/duration/CSV utilities และ Vercel rewrite configuration รายชื่อขอบเขตดู [Test Plan](test-plan.md)

npm test ครั้งแรกใน sandbox สร้าง Node subprocess ไม่ได้ (spawn EPERM) จึงรันคำสั่งเดิมนอก sandbox แล้วผ่าน 30 tests ส่วน Vite build รันนอก sandbox เช่นกัน นี่เป็นข้อจำกัดของเครื่องมือรัน ไม่ใช่การเปลี่ยน assertions หรือข้าม failing test

Frontend lint ล้มด้วย ERR_MODULE_NOT_FOUND ของ @eslint/js ทั้งในและนอก sandbox การขอ npm ci เพื่อ restore dependency ไม่ได้เริ่มเพราะ approval review หมดเวลา จึงเก็บสถานะตามจริง ไม่แก้ eslint.config.js หรือ package files เพื่อให้ผ่าน

ก่อนส่ง ให้รัน npm ci ตาม lockfile บนเครื่องที่อนุญาตติดตั้ง dependencies แล้วรัน npm run lint ซ้ำ หากมี lint errors ของ source ต้องให้ทีมแก้และอัปเดตรายงานนี้ ไม่ใช้ผล blocked รอบนี้แทน PASS

## 5. Warnings และข้อจำกัด

- Compiler แจ้ง unchecked/unsafe operations ใน ReportQueryRepository และ ClientServiceImplTest; OpenApiContractTest ใช้ deprecated API การ build ผ่านไม่ได้แปลว่าไม่มี warnings
- Vite แสดง PLUGIN_TIMINGS performance diagnostic แต่ build จบ exit 0 ไม่แก้ build configuration ในงานเอกสารนี้
- H2 tests ปิด Flyway จึงไม่ยืนยัน PostgreSQL V1–V20, partial unique index หรือ runtime concurrency บน PostgreSQL จริง
- รายงานนี้ไม่มี stress/load/security penetration tests, browser acceptance หรือหลักฐาน deployment CI ล่าสุด
- Coverage Frontend ที่วัดได้ไม่รวมไฟล์ที่ไม่ถูก import/React .tsx; Backend coverage ยังต้องเก็บแยก ดู [Coverage Report](coverage-report.md)
- Known requirement gaps ใน [Use Case Description](../doc/use-case-description.md) ยังอยู่ ไม่ประกาศว่า feature ครบเพียงเพราะ regression tests ผ่าน

## 6. หลักฐานและการรันซ้ำ

- [Backend execution summary](evidence/backend-results.json): version/command/exit/totals และราย suite ที่สรุปจาก XML โดยไม่คัดลอก environment properties หรือ application logs ลง repo
- [Frontend execution summary](evidence/frontend-results.json): ผล commands และค่า coverage จาก Node output
- Raw Backend reports อยู่ใน code/Backend/target/surefire-reports/ ซึ่งถูก gitignore; เมื่อรันซ้ำไฟล์เหล่านี้เปลี่ยนได้ evidence ใน test/ เป็น snapshot ของรอบนี้
- dist/ และ executable JAR เป็น build artifacts ไม่ใช่ production source changes และไม่เพิ่มเข้า Git ในรอบนี้
- ดู [Test Plan](test-plan.md) สำหรับ manual/release checks ที่ต้องทำเพิ่มและ [CI/CD](../doc/ci-cd.md) สำหรับ gates ของ Java 17/Node 22/PostgreSQL

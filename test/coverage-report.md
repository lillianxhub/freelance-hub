# Coverage Report - Freelance Hub

วันที่ 9 ตุลาคม 2026, source `ca77d74` รายงานนี้เก็บ Frontend coverage จริงด้วย Node.js v24.12.0 และแยกสิ่งที่ยังไม่ได้วัด ไม่อ้างเปอร์เซ็นต์จากจำนวน tests

## Frontend: Node test-runner coverage

คำสั่งจาก code/Frontend:

```powershell
node --experimental-test-coverage "--test-coverage-include=src/**/*.ts" "--test-coverage-exclude=**/*.test.ts" --import tsx --test src/api/*.test.ts src/services/*.test.ts src/utils/*.test.ts src/lib/*.test.ts
```

ผลรัน: 30 tests ผ่าน, fail/error 0, exit code 0; coverage ต่อไปนี้เป็น production TypeScript files **11 ไฟล์ที่ถูกโหลดโดยชุดทดสอบนี้** ไม่ใช่ทุกไฟล์ใน src/ และไม่ได้รวม React pages/components .tsx ที่ไม่ถูกรัน

| File (เริ่มจาก code/Frontend/src/) | Line % | Branch % | Function % |
|---|---:|---:|---:|
| api/apiClient.ts | 97.32 | 86.11 | 92.00 |
| api/apiError.ts | 90.00 | 100.00 | 80.00 |
| lib/analytics.ts | 100.00 | 68.75 | 100.00 |
| lib/dashboard.ts | 97.06 | 83.33 | 100.00 |
| lib/timeTracking.ts | 100.00 | 75.00 | 100.00 |
| services/report.ts | 91.53 | 75.00 | 75.00 |
| services/task.ts | 65.38 | 100.00 | 75.00 |
| services/timeTracking.ts | 65.49 | 46.15 | 40.00 |
| utils/csv.ts | 70.59 | 83.33 | 80.00 |
| utils/date.ts | 58.82 | 75.00 | 66.67 |
| utils/duration.ts | 100.00 | 90.00 | 100.00 |
| รวมเฉพาะไฟล์ที่ Node รายงาน | 86.55 | 80.47 | 80.43 |

ค่า all files จาก Node ใช้เฉพาะ denominator ของไฟล์ที่ถูกโหลดและผ่าน include/exclude ไม่เพิ่มไฟล์ที่ไม่เคยถูก import ให้มี coverage=0 จึงไม่ควรนำ 86.55% ไปเขียนว่า Frontend ทั้งโปรเจกต์มี line coverage เท่านี้

## Backend

Maven verify ผ่าน 446 tests แต่ **ยังไม่มี Backend code-coverage percentage ในรอบนี้** pom.xml ปัจจุบันไม่ได้ตั้ง JaCoCo และ Surefire XML เป็น execution results ไม่ใช่ coverage report

ไม่แก้ pom/dependencies เพื่อเพิ่ม instrumentation ในงานเอกสารนี้ หากทีมต้องส่ง Backend coverage ให้ตกลง JaCoCo configuration/agent กับทีม แล้วรัน JDK 17 ตาม CI และแนบ HTML/XML/CSV ของ release จริงก่อนส่ง ห้ามใส่เปอร์เซ็นต์สมมุติหรือใช้ 446 tests แทน coverage

## ข้อเสนอจากผลที่วัดได้

- เพิ่ม tests สำหรับ branches ของ services/timeTracking.ts โดยเฉพาะ create/update/delete/timer operations ที่ชุดปัจจุบันยังไม่ครอบคลุมทั้งหมด
- แยก unit tests ของ CSV serialization ออกจาก browser download behavior; toCsv ผ่านแล้วไม่ได้พิสูจน์ downloadCsv บน browser
- เพิ่ม React component/browser tests สำหรับ Dashboard/Client/Project/Timer/Reports เพราะ npm test ปัจจุบันเลือกแต่ .test.ts ในสี่โฟลเดอร์
- เก็บ Backend coverage, PostgreSQL integration/concurrency และ deployment acceptance เพิ่มก่อนอ้าง whole-system test coverage

ดู [Test Report](test-report.md), [Test Plan](test-plan.md) และ [Frontend execution evidence](evidence/frontend-results.json)

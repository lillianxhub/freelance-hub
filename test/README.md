# Test Artifacts

โฟลเดอร์นี้เก็บเอกสารและหลักฐานการทดสอบสำหรับส่งรายวิชา ส่วน automated test source อยู่ใน `code/Backend/src/test/` และ `code/Frontend/src/` ไม่ใช่ test/ นี้

| เอกสาร | เนื้อหา |
|---|---|
| [Test Plan](test-plan.md) | Automated regression matrix, วิธีรัน และ manual/deployment scenarios ที่ยังต้องตรวจ |
| [Test Report](test-report.md) | ผลวันที่ 10 ตุลาคม 2026: tests, lint, typecheck และ build ผ่าน; เก็บผลรอบ 9 ตุลาคมไว้แยกกัน |
| [Coverage Report](coverage-report.md) | Frontend Node coverage แบบจำกัดขอบเขต; Backend coverage ยังไม่ได้เก็บ |
| [Backend evidence](evidence/backend-results.json) | ผลรอบ 9 ตุลาคม: counts ราย suite จาก Surefire |
| [Frontend evidence](evidence/frontend-results.json) | ผลรอบ 9 ตุลาคม: commands และ coverage summary |

ผล local ไม่แทน CI, PostgreSQL migrations หรือ cloud deployment verification ต้องเก็บหลักฐาน release จริงเพิ่มเติมก่อนส่ง และไม่ใช้ผล coverage บางไฟล์อ้างว่า whole-project coverage

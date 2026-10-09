# เอกสารส่งมอบ - Freelance Hub

เอกสารฉบับรวมสำหรับรายวิชา CP353002 ตรวจจาก implementation ณ commit `131305f` วันที่ 9 ตุลาคม 2026 เนื้อหารวมจากสมาชิกทั้ง 5 คน โดยคงต้นฉบับใน V1 ไว้และไม่แก้ production code ในการรวมเอกสารรอบนี้

## เอกสารหลัก

| เอกสาร | เนื้อหา |
|---|---|
| [SOLID Analysis](solid-analysis.md) | หลักฐานทั้ง 5 principles พร้อมไฟล์/บรรทัดและข้อจำกัด |
| [Design Patterns](design-patterns.md) | Architectural patterns และ Behavioral GoF patterns ที่มีจริง พร้อมตำแหน่งใน diagram |
| [Use Case Description](use-case-description.md) | Actors, endpoint contracts, flows, alternative flows และ requirement boundaries ของทุก feature |
| [Data Dictionary](data-dictionary.md) | Schema หลัง Flyway V1-V20, constraints, indexes, ความสัมพันธ์และ archive/soft-delete semantics |
| [Diagram Index](diagrams/README.md) | Use Case, Domain, Class, ER, Sequence 6 scenarios, Activity, Component, Deployment และ State |
| [Reports API](reports-summary-api.md) | Contract ของข้อมูลหน้า Reports ที่มีอยู่เดิม |
| [CI/CD](ci-cd.md) | Pipeline, Docker/Flyway checks, quality gates และเงื่อนไข deploy จากเอกสาร Petpinyo |
| [Test Plan / Report / Coverage](../test/README.md) | ผลรันทดสอบจริงและขอบเขตหลักฐานที่ยังไม่ได้เก็บ |

วิธีติดตั้ง/รันระบบ, Swagger, test commands, สมาชิกและ Deployment URL ดู [README ของ repository](../README.md) และข้อกำหนดผลิตภัณฑ์ดู [REQUIREMENTS](../REQUIREMENTS.md)

หลัง pull ล่าสุดตรวจเทียบ source ที่ `131305f` และนำการแก้ Time Tracking documentation ใน `f0a949c` (PR #125) เข้าฉบับรวมแล้ว โค้ดใน code/ และ workflows ไม่เปลี่ยนจาก snapshot `cb8002d` ที่ใช้รวมเอกสารครั้งก่อน แต่ตรวจคำอธิบายซ้ำและแก้รายละเอียดที่ไม่ครบด้วย

ไฟล์หลักใน doc/ และ diagrams/ เป็นฉบับอ้างอิงปัจจุบัน ส่วน V1/ เป็นต้นฉบับงานแยกของสมาชิกที่คงไว้เพื่อดูที่มา บางส่วนยังมี endpoint/path/คำอธิบายรุ่นเก่า จึงไม่ควรใช้ V1 ทุกไฟล์เป็น current API contract โดยไม่เทียบฉบับรวม

## ที่มาของงานสมาชิก

| สมาชิก / Branch | ขอบเขต | ต้นฉบับ |
|---|---|---|
| Petpinyo / `petpinyo_673380073-7_02` | Authentication, Profile, Security, CI/CD | [SOLID](V1/SOLID/petpinyo-solid.md), [Design](V1/DESIGN/petpinyo-design.md), [Use Cases](V1/USECASE/petpinyo-usecase.md), [CI/CD](V1/CICD/petpinyo-cicd.md) |
| Thirawat / `thirawat_673380039-7_02` | Client Management | [SOLID](V1/SOLID/thirawat-solid.md), [Design](V1/DESIGN/thirawat-design.md), [Use Cases](V1/USECASE/thirawat-usecase.md) |
| Kantavit / `kantavit_673380027-4_01` | Project, Task และ Progress Events | [SOLID](V1/SOLID/kantavit-solid.md), [Design](V1/DESIGN/kantavit-design.md), [Use Cases](V1/USECASE/kantavit-usecase.md) |
| Kompat / `kompat_673380262-4_02` | Time Tracking | [SOLID](V1/SOLID/kompat-solid.md), [Design](V1/DESIGN/kompat-design.md), [Use Cases](V1/USECASE/kompat-usecase.md) |
| Nattadol / `nattadol_673380511-9_02` | Dashboard, Reports และ Frontend | [SOLID](V1/SOLID/nattadol-solid.md), [Design](V1/DESIGN/nattadol-design.md), [Use Cases](V1/USECASE/nattadol-usecase.md) |

## Diagram และผลทดสอบที่เพิ่มหลังฉบับรวม

ตรวจ source ณ `ca77d74` แล้วเพิ่ม [Use Case Diagram](diagrams/use-case-diagram.md), [Conceptual Domain Model](diagrams/domain-model.md), [Deployment Diagram](diagrams/deployment-diagram.md) และ [State Diagrams](diagrams/state-diagram.md) โดยไม่เพิ่ม feature หรือ production code

## ขอบเขตการส่งมอบที่ยังต้องตรวจจากทีม

- สไลด์นำเสนอใน slide/ ยังมีเพียง README ไม่สร้างสไลด์ตามขอบเขตที่ผู้ใช้กำหนด
- Test Report มีผล local Backend 446/Frontend 30 tests ผ่าน พร้อม typecheck/build แต่ Frontend lint ยังติด local dependency; ต้องรัน lint ซ้ำก่อนอ้างว่า gates ทุกตัวผ่าน
- Coverage มีเฉพาะ 11 Frontend TypeScript files ที่ชุดทดสอบโหลด; Backend/React UI coverage ยังไม่ได้เก็บ และยังไม่ได้แนบผล CI/PostgreSQL/cloud verification ใหม่
- Deployment Diagram อธิบาย configuration ไม่ใช่หลักฐานว่า public URL ใช้งานได้จริง; ทีมต้องตรวจ environment/settings/health ของ release ก่อนส่ง
- ข้อจำกัดของ feature เช่นเวลาราย Project ใน Client detail, การตีความ FR-CLI-05, UI บาง metric และ notification แยกไว้ใน Use Case/Design ไม่ถูกประกาศว่าเสร็จจากการรวมเอกสาร

เอกสารและ diagram หลักที่เคยขาดจัดทำแล้ว แต่ข้อจำกัดข้างต้นต้องตรวจเพิ่มก่อนประกาศว่างานส่งมอบทั้งโครงการสมบูรณ์

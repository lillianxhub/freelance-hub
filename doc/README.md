# เอกสารส่งมอบ - Freelance Hub

เอกสารฉบับรวมสำหรับรายวิชา CP353002 ตรวจจาก implementation ณ commit `5f55faf` วันที่ 9 ตุลาคม 2026 เนื้อหารวมจากสมาชิกทั้ง 5 คน โดยไม่แก้ production code ในการรวมเอกสารรอบนี้

## เอกสารหลัก

| เอกสาร | เนื้อหา |
|---|---|
| [SOLID Analysis](solid-analysis.md) | หลักฐานทั้ง 5 principles พร้อมไฟล์/บรรทัดและข้อจำกัด |
| [Design Patterns](design-patterns.md) | Architectural patterns และ Behavioral GoF patterns ที่มีจริง พร้อมตำแหน่งใน diagram |
| [Use Case Description](use-case-description.md) | Actors, endpoint contracts, flows, alternative flows และ requirement boundaries ของทุก feature |
| [Data Dictionary](data-dictionary.md) | Schema หลัง Flyway V1-V20, constraints, indexes, ความสัมพันธ์และ archive/soft-delete semantics |
| [Diagram Index](diagrams/README.md) | Use Case, Domain, Class, ER, Sequence 6 scenarios, Activity, Component, Deployment และ State |
| [Error Contract](error-contract.md) | HTTP status, details schemas และ flow ของ Chain/Factory พร้อม source references |
| [Reports API](reports-summary-api.md) | Contract ของข้อมูลหน้า Reports ที่มีอยู่เดิม |
| [CI/CD](ci-cd.md) | Pipeline, Docker/Flyway checks, quality gates และเงื่อนไข deploy จากเอกสาร Petpinyo |
| [Test Plan / Report / Coverage](../test/README.md) | ผลรันทดสอบจริงและขอบเขตหลักฐานที่ยังไม่ได้เก็บ |

วิธีติดตั้ง/รันระบบ, Swagger, test commands, สมาชิกและ Deployment URL ดู [README ของ repository](../README.md) และข้อกำหนดผลิตภัณฑ์ดู [REQUIREMENTS](../REQUIREMENTS.md)

หลัง pull ล่าสุดตรวจ source ที่ `5f55faf` และนำเอกสาร Auth/Profile/SOLID/Design/CI-CD ที่ Petpinyo ปรับใน `4e96e4b` (PR #127) มาเทียบและปรับฉบับรวมแล้ว รวมถึงคงการแก้ Time Tracking ของ PR #125 โค้ดใน code/ และ workflows ไม่เปลี่ยนจาก snapshot `ca77d74` ที่ใช้รันทดสอบก่อนหน้านี้; Test Report จึงยังระบุ commit ของรอบที่รันจริง ไม่เปลี่ยนเป็นผลทดสอบใหม่

ไฟล์หลักใน doc/ และ diagrams/ เป็นฉบับอ้างอิงปัจจุบัน ให้ใช้ร่วมกับ Swagger และ source code เมื่อตรวจ API contract

## ผู้รับผิดชอบเอกสาร

| สมาชิก / Branch | ขอบเขต | เอกสารที่รับผิดชอบ |
|---|---|---|
| Petpinyo / `petpinyo_673380073-7_02` | Authentication, Profile, Security, CI/CD | SOLID, Design, Use Cases, CI/CD |
| Thirawat / `thirawat_673380039-7_02` | Client Management | SOLID, Design, Use Cases |
| Kantavit / `kantavit_673380027-4_01` | Project, Task และ Progress Events | SOLID, Design, Use Cases |
| Kompat / `kompat_673380262-4_02` | Time Tracking | SOLID, Design, Use Cases |
| Nattadol / `nattadol_673380511-9_02` | Dashboard, Reports และ Frontend | SOLID, Design, Use Cases |

## Diagram และผลทดสอบที่เพิ่มหลังฉบับรวม

ตรวจ source ณ `ca77d74` แล้วเพิ่ม [Use Case Diagram](diagrams/use-case-diagram.md), [Conceptual Domain Model](diagrams/domain-model.md), [Deployment Diagram](diagrams/deployment-diagram.md) และ [State Diagrams](diagrams/state-diagram.md) โดยไม่เพิ่ม feature หรือ production code ปัจจุบันรวม Sequence ทั้ง 6 scenarios ใน [ไฟล์เดียว](diagrams/sequence-diagram.md) และข้อความภายใน Mermaid diagrams ของฉบับส่งมอบใช้ภาษาอังกฤษ โดยคงคำอธิบายภาษาไทยไว้

## ขอบเขตการส่งมอบที่ยังต้องตรวจจากทีม

- สไลด์นำเสนอใน slide/ ยังมีเพียง README ไม่สร้างสไลด์ตามขอบเขตที่ผู้ใช้กำหนด
- Test Report มีผล local Backend 446/Frontend 30 tests ผ่าน พร้อม typecheck/build แต่ Frontend lint ยังติด local dependency; ต้องรัน lint ซ้ำก่อนอ้างว่า gates ทุกตัวผ่าน
- Coverage มีเฉพาะ 11 Frontend TypeScript files ที่ชุดทดสอบโหลด; Backend/React UI coverage ยังไม่ได้เก็บ และยังไม่ได้แนบผล CI/PostgreSQL/cloud verification ใหม่
- Deployment Diagram อธิบาย configuration ไม่ใช่หลักฐานว่า public URL ใช้งานได้จริง; ทีมต้องตรวจ environment/settings/health ของ release ก่อนส่ง
- ข้อจำกัดของ feature เช่นเวลาราย Project ใน Client detail, การตีความ FR-CLI-05, UI บาง metric และ notification แยกไว้ใน Use Case/Design ไม่ถูกประกาศว่าเสร็จจากการรวมเอกสาร

เอกสารและ diagram หลักที่เคยขาดจัดทำแล้ว แต่ข้อจำกัดข้างต้นต้องตรวจเพิ่มก่อนประกาศว่างานส่งมอบทั้งโครงการสมบูรณ์

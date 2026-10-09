# เอกสารส่งมอบ - Freelance Hub

เอกสารฉบับรวมสำหรับรายวิชา CP353002 ตรวจจาก implementation ณ commit `cb8002d` วันที่ 9 ตุลาคม 2026 เนื้อหารวมจากสมาชิกทั้ง 5 คน โดยคงต้นฉบับใน V1 ไว้และไม่แก้ production code ในการรวมเอกสารรอบนี้

## เอกสารหลัก

| เอกสาร | เนื้อหา |
|---|---|
| [SOLID Analysis](solid-analysis.md) | หลักฐานทั้ง 5 principles พร้อมไฟล์/บรรทัดและข้อจำกัด |
| [Design Patterns](design-patterns.md) | Architectural patterns และ Behavioral GoF patterns ที่มีจริง พร้อมตำแหน่งใน diagram |
| [Use Case Description](use-case-description.md) | Actors, endpoint contracts, flows, alternative flows และ requirement boundaries ของทุก feature |
| [Data Dictionary](data-dictionary.md) | Schema หลัง Flyway V1-V20, constraints, indexes, ความสัมพันธ์และ archive/soft-delete semantics |
| [Diagram Index](diagrams/README.md) | Domain/class, ER, Sequence 6 scenarios, Activity และ component view จากต้นฉบับที่มี |
| [Reports API](reports-summary-api.md) | Contract ของข้อมูลหน้า Reports ที่มีอยู่เดิม |
| [CI/CD](ci-cd.md) | Pipeline, Docker/Flyway checks, quality gates และเงื่อนไข deploy จากเอกสาร Petpinyo |

วิธีติดตั้ง/รันระบบ, Swagger, test commands, สมาชิกและ Deployment URL ดู [README ของ repository](../README.md) และข้อกำหนดผลิตภัณฑ์ดู [REQUIREMENTS](../REQUIREMENTS.md)

## ที่มาของงานสมาชิก

| สมาชิก / Branch | ขอบเขต | ต้นฉบับ |
|---|---|---|
| Petpinyo / `petpinyo_673380073-7_02` | Authentication, Profile, Security, CI/CD | [SOLID](V1/SOLID/petpinyo-solid.md), [Design](V1/DESIGN/petpinyo-design.md), [Use Cases](V1/USECASE/petpinyo-usecase.md), [CI/CD](V1/CICD/petpinyo-cicd.md) |
| Thirawat / `thirawat_673380039-7_02` | Client Management | [SOLID](V1/SOLID/thirawat-solid.md), [Design](V1/DESIGN/thirawat-design.md), [Use Cases](V1/USECASE/thirawat-usecase.md) |
| Kantavit / `kantavit_673380027-4_01` | Project, Task และ Progress Events | [SOLID](V1/SOLID/kantavit-solid.md), [Design](V1/DESIGN/kantavit-design.md), [Use Cases](V1/USECASE/kantavit-usecase.md) |
| Kompat / `kompat_673380262-4_02` | Time Tracking | [SOLID](V1/SOLID/kompat-solid.md), [Design](V1/DESIGN/kompat-design.md), [Use Cases](V1/USECASE/kompat-usecase.md) |
| Nattadol / `nattadol_673380511-9_02` | Dashboard, Reports และ Frontend | [SOLID](V1/SOLID/nattadol-solid.md), [Design](V1/DESIGN/nattadol-design.md), [Use Cases](V1/USECASE/nattadol-usecase.md) |

## ขอบเขตการส่งมอบที่ยังต้องรวมจากทีม

รอบนี้รวมเฉพาะเนื้อหา/diagram ที่มีต้นฉบับ ไม่สร้างงานที่ยังไม่มีแทนสมาชิก:

- Use Case Diagram ยังไม่มีต้นฉบับแยก: ต้องเทียบ actors/ชื่อ use case กับคำอธิบายก่อนส่ง
- Domain Class Model มีแล้วใน class-diagram.md; Domain Model แบบ conceptual แยกยังไม่มี
- Deployment Diagram ของ cloud nodes และ State Diagram แบบแยกยังไม่มี; component view ไม่ถือว่าแทนสองชนิดนี้ได้
- สไลด์นำเสนอใน slide/ ยังมีเพียง README ไม่สร้างสไลด์ในรอบนี้
- Test Report/coverage/หลักฐาน CI ต้องใช้ผลรันของ release จริงจาก test/ หรือ CI; รายชื่อ test ในเอกสารไม่ใช่รายงานผลผ่าน
- ข้อจำกัดของ feature เช่นเวลาราย Project ใน Client detail, การตีความ FR-CLI-05, UI บาง metric และ notification แยกไว้ใน Use Case/Design ไม่ถูกประกาศว่าเสร็จจากการรวมเอกสาร

เอกสารที่มีอยู่ถูกจัดเป็นฉบับรวมใช้อ่านและรีวิวสำหรับส่ง แต่รายการข้างต้นต้องครบและตรวจ deployment/test จริงก่อนประกาศว่างานส่งมอบทั้งโครงการสมบูรณ์

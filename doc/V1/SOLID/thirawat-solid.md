# SOLID Analysis: Client Management

**เจ้าของ feature:** `thirawat_673380039-7_02`  
**ขอบเขต:** Client API และ business logic สำหรับผู้ใช้ที่เข้าสู่ระบบ

เลขบรรทัดอ้างอิง implementation ปัจจุบันภายใต้ `code/Backend/src/main/java/th/ac/kku/freelance_hub/` และควรตรวจซ้ำเมื่อรวมเข้าเอกสารหลักหรือแก้โค้ด

| Principle | ไฟล์/คลาสและบรรทัด | เหตุผลและขอบเขตหลักฐาน |
|---|---|---|
| Single Responsibility | `controller/ClientController.java:35-105` | Controller รับ request, ดึง owner จากผู้ใช้ที่ล็อกอิน และคืน HTTP response; ไม่สร้าง query หรือแก้ entity โดยตรง |
| Single Responsibility | `mapper/ClientMapper.java:15-69` | Mapper แปลง request/response กับ `Client` และจัดการ semantics ของ PATCH; ไม่ติดต่อฐานข้อมูล |
| Single Responsibility | `repository/ClientRepository.java:19-32` | Repository รับผิดชอบ data-access contract ของ Client โดยเฉพาะ |
| Open/Closed | `service/ClientService.java:13-25`, `controller/ClientController.java:41,50-103` | Controller พึ่ง service contract; สามารถเปลี่ยน implementation ของ service หรือใช้ test double โดยไม่แก้ controller ทั้งนี้การเพิ่มรูปแบบ filter ใหม่ยังต้องแก้ `ClientServiceImpl` ปัจจุบัน |
| Liskov Substitution | `service/ClientService.java:13-25`, `service/impl/ClientServiceImpl.java:29,49-119` | `ClientServiceImpl` implement operation ทั้งห้าตามชนิดผลลัพธ์ใน interface และถูกเรียกผ่าน `ClientService`; ปัจจุบันมี production implementation เดียว จึงยังไม่มีการพิสูจน์การแทนที่ระหว่าง implementation หลายตัว |
| Interface Segregation | `service/ClientService.java:13-25`, `controller/ClientController.java:41` | Contract นี้มีเฉพาะงาน Client ที่ controller ใช้ ไม่รวม Auth, Project หรือ Time Entry |
| Dependency Inversion | `controller/ClientController.java:38-42`, `service/impl/ClientServiceImpl.java:34-48`, `repository/ClientRepository.java:19-20` | Controller รับ `ClientService` interface และ service รับ repository interfaces ผ่าน constructor แทนการสร้าง dependency เอง; `ClientMapper` และ `UserService` ยังเป็น concrete class จึงเป็นการใช้ DIP บางส่วน |

## Evidence จาก Client flow

- Owner ID มาจาก authenticated user ใน controller ไม่รับจาก request: `ClientController.java:50-52,62-64,73-75,85-90,100-102`
- การอ่าน/แก้/archive ใช้ `findByIdAndOwnerId`; ไม่พบหรือเป็นของผู้อื่นจะได้ `ClientNotFoundException`: `ClientServiceImpl.java:61-62,107-125`
- รายการลูกค้าเริ่มด้วย predicate ของ owner และต่อ status/search ก่อน pagination: `ClientServiceImpl.java:67-102`
- Mapper คงฟิลด์เดิมเมื่อ PATCH ส่ง `null`: `ClientMapper.java:34-46,67-69`
- Archive เปลี่ยน `ClientStatus` โดยไม่ลบ record: `ClientServiceImpl.java:115-119`, `Client.java:123-126`

## ข้อสังเกตสำหรับรวมเอกสารหลัก

1. ตัวอย่าง OCP/LSP ของส่วน Client แสดง extension point ผ่าน interface แต่ไม่ควรอ้างว่ามีหลาย algorithm/implementation แล้ว
2. ถ้าต้องการให้ DIP เข้มขึ้น อาจพิจารณา abstraction สำหรับ mapper หรือ current-user provider ภายหลัง; เอกสารนี้ไม่ถือว่าเป็นงานที่ทำเสร็จแล้ว
3. ตรวจเลขบรรทัดหลัง code freeze ก่อนนำตารางนี้ไปรวมใน `doc/solid-analysis.md`

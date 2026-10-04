# SOLID Analysis: Client Management

**เจ้าของ feature:** `thirawat_673380039-7_02`  
**ขอบเขต:** Client API และ business logic สำหรับผู้ใช้ที่เข้าสู่ระบบ

ไฟล์อ้างอิงอยู่ภายใต้ `code/Backend/src/main/java/th/ac/kku/freelance_hub/` โดยไม่ผูกกับเลขบรรทัดที่เปลี่ยนเมื่อแก้โค้ด

| Principle | ไฟล์/คลาส | เหตุผลและขอบเขตหลักฐาน |
|---|---|---|
| Single Responsibility | `controller/ClientController.java`, `exception/ClientExceptionHandler.java` | Controller รับ request และคืน success response; handler เฉพาะ Client แปลง exception เป็น error envelope โดยไม่ให้ controller จัดการ error เอง |
| Single Responsibility | `mapper/ClientMapper.java` | Mapper แปลง DTO กับ `Client` รวม semantics ของ PUT/PATCH; ไม่ติดต่อฐานข้อมูล |
| Single Responsibility | `repository/ClientRepository.java` | Repository รับผิดชอบ data-access contract ของ Client โดยเฉพาะ |
| Open/Closed | `service/ClientService.java`, `controller/ClientController.java` | Controller พึ่ง service contract จึงเปลี่ยน implementation หรือใช้ test double ได้โดยไม่แก้ controller ทั้งนี้การเพิ่มรูปแบบ filter ใหม่ยังต้องแก้ `ClientServiceImpl` ปัจจุบัน |
| Liskov Substitution | `service/ClientService.java`, `service/impl/ClientServiceImpl.java` | `ClientServiceImpl` implement operation ของ interface ครบ รวม `replace`, `changeStatus` และ `softDelete`; ปัจจุบันมี production implementation เดียว จึงยังไม่มีหลักฐานการแทนที่ระหว่าง implementation หลายตัว |
| Interface Segregation | `service/ClientService.java`, `controller/ClientController.java` | Contract รวมงาน Client API กับ method สรุปเวลาตามลูกค้าสำหรับ service อื่น; ยังไม่รวม Auth หรือ CRUD ของ Project/Time Entry แต่ไม่ควรอ้างว่าทุก method ใช้โดย controller |
| Dependency Inversion | `controller/ClientController.java`, `service/impl/ClientServiceImpl.java`, `repository/ClientRepository.java` | Controller รับ `ClientService` interface และ service รับ repository interfaces ผ่าน constructor แทนการสร้าง dependency เอง; `ClientMapper` และ `UserService` ยังเป็น concrete class จึงเป็นการใช้ DIP บางส่วน |

## Evidence จาก Client flow

- Owner ID มาจาก authenticated user ใน controller ไม่รับจาก request: `ClientController.java`
- การอ่าน/แก้/เปลี่ยนสถานะ/soft delete ใช้ `findByIdAndOwnerId` และตรวจ `deletedAt == null`; ไม่พบ ไม่ใช่เจ้าของ หรือถูก soft delete แล้วจะได้ `ClientNotFoundException`: `ClientServiceImpl.java`
- รายการลูกค้าเริ่มด้วย predicate ของ owner และ `deletedAt IS NULL` แล้วต่อ status/search ก่อน pagination: `ClientServiceImpl.java`
- Mapper คงฟิลด์เดิมเมื่อ PATCH ส่ง `null` แต่ PUT ใช้ข้อมูลใหม่แทนที่และล้าง optional fields ที่ไม่ส่งมา: `ClientMapper.java`
- `changeStatus(false)` เปลี่ยน Client เป็น inactive และเรียก `Project.changeStatus(ARCHIVED)` สำหรับ Project ของลูกค้านั้นที่ยังไม่ถูก soft delete ภายใน transaction เดียวกัน; เปิด Client กลับมาไม่คืนสถานะ Project อัตโนมัติ ส่วน `softDelete` ตั้งเฉพาะ `deletedAt` ของ Client โดยไม่ลบ record: `ClientServiceImpl.java`, `ClientRepository.java`, `Client.java`, `Project.java`
- Response ที่มี body ใช้ `ApiResult`; 400/404 ของ Client ผ่าน `ClientExceptionHandler`, ส่วน GET รายการมี `PaginationMeta`: `ClientController.java`, `ClientExceptionHandler.java`
- Client detail เลือกแนบ Project/Task ด้วย `include=projects` หรือ `include=projects.tasks` และ repository อ่านเฉพาะฟิลด์ที่แสดง; ส่วน `summarizeTimeByClient` เป็น service method ภายในที่ใช้ projection รวมเวลาของ Time Entry ตาม Client: `ClientController.java`, `ClientRepository.java`, `ClientServiceImpl.java`
- `ClientServiceImpl` เติม `totalTrackedSeconds` ใน GET รายการและรายละเอียดผ่าน aggregate projection โดย query ทีเดียวเฉพาะ IDs ในหน้าปัจจุบัน; mapper ยังรับผิดชอบเฉพาะการแปลง entity และไม่มีการ query ใน mapper กติกายอดใหม่นับประวัติแบบเดียวกับ Time Entry summary ไม่ใช้ตัวกรอง Project/Task inactive ของ method Analytics เดิม: `ClientRepository.java`, `ClientServiceImpl.java`, `dto/response/client/ClientResponse.java`

## ข้อสังเกตสำหรับรวมเอกสารหลัก

1. ตัวอย่าง OCP/LSP ของส่วน Client แสดง extension point ผ่าน interface แต่ไม่ควรอ้างว่ามีหลาย algorithm/implementation แล้ว
2. ถ้าต้องการให้ DIP เข้มขึ้น อาจพิจารณา abstraction สำหรับ mapper หรือ current-user provider ภายหลัง; เอกสารนี้ไม่ถือว่าเป็นงานที่ทำเสร็จแล้ว
3. เมื่อนำไปรวมใน `doc/solid-analysis.md` ควรเทียบกับ implementation ล่าสุดอีกครั้ง โดยเฉพาะความต่างระหว่าง `isActive` และ `deletedAt`

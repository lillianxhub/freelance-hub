# SOLID Analysis: Client Management

**เจ้าของ feature:** `thirawat_673380039-7_02`  
**ขอบเขต:** Client API และ business logic สำหรับผู้ใช้ที่เข้าสู่ระบบ

ไฟล์อ้างอิงอยู่ภายใต้ `code/Backend/src/main/java/th/ac/kku/freelance_hub/` เลขบรรทัดด้านล่างตรวจจากโค้ดหลัง merge PR #107 และต้องตรวจใหม่ก่อนรวมเอกสารหลักหรือส่งงาน

| Principle | ไฟล์/คลาส | บรรทัดอ้างอิง | เหตุผลและขอบเขตหลักฐาน |
|---|---|---|---|
| Single Responsibility | `controller/ClientController.java`, `exception/ClientExceptionHandler.java`, `common/response/ApiErrorFactory.java` | 43, 24, 28 | Controller รับ HTTP และคืน success response; handler เลือกสถานะ/code จาก exception; factory รับผิดชอบสร้าง error envelope และ metadata ร่วม |
| Single Responsibility | `mapper/ClientMapper.java` | 14, 50, 62 | Mapper แปลง DTO กับ `Client` รวม semantics ของ PUT/PATCH; ไม่ติดต่อฐานข้อมูลและไม่คำนวณ aggregate |
| Single Responsibility | `repository/ClientRepository.java` | 25, 41, 80, 95 | รับผิดชอบ data access สำหรับ Client use cases รวมการอ่าน Project/Task/Time Entry ที่สัมพันธ์กับ Client; ไม่ใช่ repository ที่อ่านเฉพาะตาราง clients เท่านั้น |
| Open/Closed — extension point บางส่วน | `service/ClientService.java`, `service/CurrentUserProvider.java`, `controller/ClientController.java` | 14, 6, 45 | Controller พึ่ง service/current-user contracts จึงรองรับ implementation ที่รักษา contract โดยไม่เปลี่ยน controller; ยังไม่มีตัวอย่างเพิ่ม algorithm ใหม่ในส่วน Client และการเพิ่ม filter ใหม่ยังต้องแก้ service เดิม |
| Liskov Substitution — หลักฐานของ implementation ปัจจุบัน | `service/ClientService.java`, `service/impl/ClientServiceImpl.java` | 14, 169, 177, 204 | Service รักษา owner scope, ใช้ not-found เมื่ออ่านข้อมูลผู้อื่น/ข้อมูลที่ลบ และรักษา semantics ของ PUT/PATCH ตาม contract; อ้าง test ด้านล่าง ไม่ใช้การ implement method ครบเป็นข้อพิสูจน์ LSP และไม่อ้างว่า implementation ในอนาคตผ่านแล้ว |
| Interface Segregation — มีขอบเขตที่ยังปรับปรุงได้ | `service/CurrentUserProvider.java`, `service/ClientService.java` | 6, 14, 27 | Controller ต้องการเพียง currentUserId จึงไม่ต้องพึ่งงาน profile/password ใน UserService; ส่วน ClientService ยังรวม CRUD กับ summarizeTimeByClient ที่ controller ไม่ใช้ จึงยังไม่ใช่หลักฐาน ISP ที่สมบูรณ์ของทั้ง service |
| Dependency Inversion | `controller/ClientController.java`, `service/CurrentUserProvider.java`, `service/impl/ClientServiceImpl.java` | 45, 6, 47 | Controller รับ ClientService และ CurrentUserProvider interfaces ผ่าน constructor ที่ Lombok สร้าง; service รับ repository interfaces ผ่าน constructor โดยไม่สร้างเอง ส่วน ClientMapper และ ApiErrorFactory ยังเป็น concrete dependencies จึงไม่อ้างว่าใช้ abstraction ทุกจุด |

## Evidence จาก Client flow

- Owner ID มาจาก `CurrentUserProvider.currentUserId()` ใน controller ไม่รับจาก request; production implementation คือ `UserService` ส่วน `ClientControllerTest` mock provider interface โดยตรง: `ClientController.java`, `CurrentUserProvider.java`
- การอ่าน/แก้/เปลี่ยนสถานะ/soft delete ใช้ `findByIdAndOwnerId` และตรวจ `deletedAt == null`; ไม่พบ ไม่ใช่เจ้าของ หรือถูก soft delete แล้วจะได้ `ClientNotFoundException`: `ClientServiceImpl.java`
- รายการลูกค้าเริ่มด้วย predicate ของ owner และ `deletedAt IS NULL` แล้วต่อ status/search ก่อน pagination: `ClientServiceImpl.java`
- Mapper คงฟิลด์เดิมเมื่อ PATCH ส่ง `null` แต่ PUT ใช้ข้อมูลใหม่แทนที่และล้าง optional fields ที่ไม่ส่งมา: `ClientMapper.java`
- `changeStatus(false)` เปลี่ยน Client เป็น inactive และเรียก `Project.changeStatus(ARCHIVED)` สำหรับ Project ของลูกค้านั้นที่ยังไม่ถูก soft delete ภายใน transaction เดียวกัน; เปิด Client กลับมาไม่คืนสถานะ Project อัตโนมัติ ส่วน `softDelete` ตั้งเฉพาะ `deletedAt` ของ Client โดยไม่ลบ record: `ClientServiceImpl.java`, `ClientRepository.java`, `Client.java`, `Project.java`
- Response ที่มี body ใช้ `ApiResult`; handler ของ Client ส่งต่อให้ `ApiErrorFactory` สร้าง `code`, `details`, `status`, UTC `timestamp`, `fieldErrors` และ `traceId`; validation ใส่รายฟิลด์ใน `fieldErrors` ไม่ใช่ `details` ส่วน GET รายการมี `PaginationMeta`: `ClientController.java`, `ClientExceptionHandler.java`, `ApiErrorFactory.java`
- `RequestTraceFilter` สร้าง trace ID และ header `X-Request-ID` ก่อน security/MVC; กรณี 401 ใช้ `JwtAuthenticationEntryPoint` และ factory เดียวกัน ไม่ผ่าน Client handler: `common/response/RequestTraceFilter.java`, `security/JwtAuthenticationEntryPoint.java`
- Client detail เลือกแนบ Project/Task ด้วย `include=projects` หรือ `include=projects.tasks` และ repository อ่านเฉพาะฟิลด์ที่แสดง; ส่วน `summarizeTimeByClient` เป็น service method ภายในที่ใช้ projection รวมเวลาของ Time Entry ตาม Client: `ClientController.java`, `ClientRepository.java`, `ClientServiceImpl.java`
- `ClientServiceImpl` เติม `totalTrackedSeconds` ใน GET รายการและรายละเอียดผ่าน aggregate projection โดย query ทีเดียวเฉพาะ IDs ในหน้าปัจจุบัน; mapper ยังรับผิดชอบเฉพาะการแปลง entity และไม่มีการ query ใน mapper กติกายอดใหม่นับประวัติแบบเดียวกับ Time Entry summary ไม่ใช้ตัวกรอง Project/Task inactive ของ method Analytics เดิม: `ClientRepository.java`, `ClientServiceImpl.java`, `dto/response/client/ClientResponse.java`

## ข้อสังเกตสำหรับรวมเอกสารหลัก

1. OCP เป็น extension point บางส่วน; LSP ต้องพิจารณาพฤติกรรมตาม contract ไม่ใช่เพียงมี interface หรือมี implementation หลายตัว
2. Current-user abstraction มีแล้วจากงานร่วม PR #107; abstraction สำหรับ mapper และการแยก analytics contract ออกจาก ClientService ยังเป็นข้อเสนอ ไม่ใช่งานที่ทำเสร็จแล้ว
3. เมื่อนำไปรวมใน `doc/solid-analysis.md` ควรเทียบกับ implementation ล่าสุดอีกครั้ง โดยเฉพาะความต่างระหว่าง `isActive` และ `deletedAt`

## หลักฐานการทดสอบ

ไฟล์อยู่ภายใต้ `code/Backend/src/test/java/th/ac/kku/freelance_hub/`

- `service/ClientServiceImplTest.java`: `anotherOwnersClientIsReportedAsNotFound`, `deletedClientCannotBeReadOrChanged`, `updateKeepsUnspecifiedFields`, `replaceClearsUnspecifiedOptionalFields` แสดงพฤติกรรมตาม contract ของ service ปัจจุบัน
- `controller/ClientControllerTest.java`: ใช้ mock `ClientService`/`CurrentUserProvider` และ Client handler/factory/filter จริง; ตรวจ validation, parameter errors, 404, 409 และ 500 ที่ไม่เปิดเผยข้อความภายใน
- `integration/ClientIntegrationTest.java`: ตรวจ 401 ของทุก Client endpoint ผ่าน security จริง รวม trace ID, UTC timestamp, validation `fieldErrors` และ OpenAPI error schema

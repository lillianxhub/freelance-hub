# Design Patterns - Freelance Hub

ฉบับรวมสำหรับส่งรายวิชา CP353002 ตรวจจาก implementation ณ commit `cb8002d` วันที่ 9 ตุลาคม 2026 ขอบเขตคือ Authentication/Profile, Client, Project/Task, Time Tracking และ Dashboard/Reports; ไม่รวม Finance, Invoice หรือ Payment

## 1. Enterprise / Architectural Patterns

| Pattern | ปัญหาที่แก้ | ไฟล์/คลาสที่ใช้ | Diagram |
|---|---|---|---|
| Layered Architecture | ไม่ให้ HTTP handler รับผิดชอบ business logic และ persistence พร้อมกัน | `controller/` → `service/` / `service/impl/` → `repository/` → `domain/` | [Application layers](diagrams/class-diagram.md#application-layers) |
| MVC / REST Presentation | แยก routing/status/validation ออกจาก business use cases และ React View | AuthController, ClientController, ProjectController, TimerController, DashboardController, ReportController; `code/Frontend/src/` | [Component view](diagrams/component.md) |
| Repository | ซ่อน JPA data access, derived queries, JPQL, Specification และ aggregate projections จาก controller | UserRepository, ClientRepository, ProjectRepository, TaskRepository, TimeEntryRepository, ReportQueryRepository | [Application layers](diagrams/class-diagram.md#application-layers) |
| Service Layer | รวม owner checks, กฎสถานะ, transaction และการประสานงานหลาย entity | AuthServiceImpl, ClientServiceImpl, ProjectServiceImpl, TaskServiceImpl, TimerServiceImpl, TimeEntryServiceImpl, DashboardServiceImpl, ReportServiceImpl | [Project status sequence](diagrams/sequence-03-project-status.md) |
| DTO + Mapper | กำหนด API contract ไม่ส่ง JPA entities หรือ password/token hashes ออกไป | `dto/request/`, `dto/response/`; UserMapper, ClientMapper, ProjectMapper, TaskMapper, TimeEntryMapper | [Domain model และ layers](diagrams/class-diagram.md) |
| Dependency Injection | เปลี่ยน dependency/test double โดยไม่สร้าง repository หรือ clock ใน use case | Constructor injection ใน services/controllers; PasswordEncoder bean; Clock จาก TimeConfiguration; CurrentUserProvider | [Authentication patterns](diagrams/class-diagram.md#authentication-patterns) |

เส้นทาง Backend ในตารางเริ่มจาก `code/Backend/src/main/java/th/ac/kku/freelance_hub/` Repository ใช้ Spring Data JPA; `@Query` ใน ClientRepository เป็น JPQL ไม่ใช่ native SQL ส่วน migration scripts เป็น SQL สำหรับสร้าง schema ไม่ใช่ query ใน controller

## 2. GoF Patterns: กลุ่ม Behavioral

ระบบใช้ Behavioral group โดยมี Strategy, Template Method และ Chain of Responsibility ผ่าน abstractions ของ Spring Security และมี State กับ Observer ใน domain/workflow ของ Project/Time Tracking แยกหลักฐาน framework reuse ออกจาก pattern ที่ทีมเขียนเองดังนี้

| Pattern | ปัญหาที่แก้ | Implementation และที่มา | Class Diagram / Flow |
|---|---|---|---|
| Strategy | Auth/Profile เปลี่ยน password encoding โดยไม่เปลี่ยน use case | AuthServiceImpl และ UserService พึ่ง PasswordEncoder; SecurityConfig กำหนด BCryptPasswordEncoder เป็น implementation ปัจจุบัน **เป็นการใช้ strategy ของ framework ไม่ใช่ระบบ metric strategies ที่ทีมสร้าง** | [Authentication patterns](diagrams/class-diagram.md#authentication-patterns) |
| Template Method | ใช้ filter lifecycle ที่ framework จัดไว้และกำหนดเฉพาะขั้นตรวจ JWT | JwtAuthenticationFilter สืบทอด OncePerRequestFilter และ override doFilterInternal | [Authentication patterns](diagrams/class-diagram.md#authentication-patterns) |
| Chain of Responsibility | ส่ง HTTP request ผ่าน security filters ก่อน Controller | SecurityConfig สร้าง SecurityFilterChain และเพิ่ม JwtAuthenticationFilter ก่อน UsernamePasswordAuthenticationFilter; filter ส่งต่อผ่าน FilterChain หรือคืน error ตามหน้าที่ | [Authentication patterns](diagrams/class-diagram.md#authentication-patterns) |
| State | แยกกฎ transition และสิทธิ์ของ Project แต่ละสถานะ | ProjectState, PlannedState, ActiveState, OnHoldState, CompletedState, ArchivedState; ProjectStates.from เลือก state ตาม ProjectStatus ที่เก็บใน DB | [Project State](diagrams/class-diagram.md#project-state-pattern) |
| Observer | เมื่อ timer หยุด ให้ workflow ความคืบหน้าตอบสนองโดยไม่ฝัง logic ใน TimerService | TimerServiceImpl เผยแพร่ TimerStoppedEvent; TimerStoppedProgressListener รับหลัง commit และเผยแพร่ ProjectProgressThresholdEvent; ProjectProgressThresholdListener เขียน log | [Observer sequence](diagrams/sequence-06-progress-events.md) |

### ขอบเขตของ State

| Project status | เปลี่ยนไปได้ (นอกจากสถานะเดิม) | เริ่ม timer | แก้ Task |
|---|---|---|---|
| PLANNED | ACTIVE, ARCHIVED | ไม่ได้ | ได้ |
| ACTIVE | ON_HOLD, COMPLETED, ARCHIVED | ได้ | ได้ |
| ON_HOLD | ACTIVE, ARCHIVED | ไม่ได้ | ได้ |
| COMPLETED | ARCHIVED | ไม่ได้ | ไม่ได้ |
| ARCHIVED | ACTIVE, PLANNED เมื่อ Client ใช้งานและไม่ถูก soft delete | ไม่ได้ | ไม่ได้ |

Project API ตรวจ running timer ก่อนเปลี่ยนสถานะหรือ soft delete; ก่อน ACTIVE → COMPLETED ตรวจว่าไม่มี Task ที่ยังใช้งานและไม่เสร็จ แล้วเรียก TimeEntryService.lockByProject ใน transaction เดียวกัน กฎตรวจ Task/ล็อกเวลาอยู่ใน Service ไม่ใช่ State class

การเปลี่ยนกฎของสถานะเดิมแก้ใน State นั้นได้ แต่การเพิ่มสถานะใหม่ยังต้องแก้ ProjectStatus enum และ ProjectStates.from จึงไม่อ้างว่า OCP สมบูรณ์ทุกจุด TaskStatus และ ClientStatus เป็น enum ไม่ใช่ GoF State อีกสองชุด

### ขอบเขตของ Observer

- TimerStoppedProgressListener ใช้ `@TransactionalEventListener(AFTER_COMMIT)` อ่านยอดหลัง timer ถูกบันทึก
- ProjectProgressThresholds.newlyReached ตรวจเฉพาะเกณฑ์ 80%/100% ที่ข้ามใหม่; ไม่มี targetMinutes จะไม่มี threshold event
- ผู้รับ threshold event ปัจจุบันเขียน log เท่านั้น ไม่มี notification UI, email หรือการเก็บ notification ลงฐานข้อมูล
- การสร้าง manual entry ไม่ใช่ flow หยุด timer จึงไม่อ้างว่าทำให้ TimerStoppedEvent เกิดโดยอัตโนมัติ

## 3. รูปแบบเฉพาะแต่ละ Feature

| Feature | รูปแบบที่ใช้และพฤติกรรมสำคัญ |
|---|---|
| Authentication/Profile | Password strategy, JWT filter, refresh-token rotation/hash/family revocation, User/Profile mapping; email normalization และ login throttling แยกจาก controller |
| Client | Specification จำกัด owner/deletedAt/status/prefix search; DTO projections สำหรับ include; aggregate query เติม totalTrackedSeconds ทีเดียวตาม IDs ของหน้าปัจจุบัน |
| Project/Task | State ตรวจ transition; service ประสาน Task completion, running timer และการล็อก Time Entry; Task ใช้ enum/domain rules และจัด sortOrder |
| Time Tracking | Inject Clock สำหรับเวลา UTC; unique running-timer index และ pessimistic write lock คุม concurrency; แยก TimerService กับ TimeEntryService |
| Dashboard/Reports | Service ประกอบ read models; repository/query projections รวมเวลา; React Context/hook/component composition; CSV และการแปลงจุดกราฟเป็น utilities แยกต่างหาก |

### Client: archive ไม่ใช่ soft delete

`PATCH /api/clients/{id}/status` ใช้ isActive; เมื่อ false จะเปลี่ยน Project ของ Client ที่ยังไม่ถูก soft delete เป็น ARCHIVED โดยเรียก Project.changeStatus และไม่ตั้ง deletedAt การเปิด Client กลับมาไม่คืนสถานะ Project อัตโนมัติ

`DELETE /api/clients/{id}` ตั้งเฉพาะ Client.deletedAt คง isActive เดิม และคืน 204 โดยไม่มี body เส้นนี้ไม่เปลี่ยนสถานะ Project และไม่ผ่าน running-timer guard ของ Project API

ClientStatus ใน response คำนวณจาก isActive ไม่มี status column ใน clients; `include=projects` / `include=projects.tasks` ยืมรูปแบบ relationship path แต่ response ยังคง ApiResult ไม่ใช่ JSON:API เต็มรูปแบบ

### Analytics และเวลา

Client GET totalTrackedSeconds รวม Time Entry ที่ active และจบแล้วตลอดช่วงข้อมูล รวมประวัติบน Project/Task ที่ archive/soft delete เช่นเดียวกับ Time Entry summary ส่วน service method summarizeTimeByClient และ analytics queries บางชุดตัด inactive/deleted Project/Task ออก จึงต้องเลือกกติกาให้ตรง use case ไม่ถือว่าทุกยอดมีความหมายเดียวกัน

Dashboard และ Reports ไม่สร้างตารางสรุปใหม่ กราฟใช้วินาทีจนถึงชั้นแสดงผล; React utilities อยู่ใน `lib/dashboard.ts`, `utils/duration.ts` และ `utils/csv.ts` ไม่ใช่ชื่อไฟล์เก่าที่ถูกย้ายไปแล้ว

## 4. รูปแบบที่ไม่ควรนับเป็น GoF เพิ่ม

- Static factories เช่น TimeEntry.startTimer/createManual ไม่ใช่ GoF Factory Method ที่มี creator hierarchy
- ApiErrorFactory เป็น shared error-construction component ไม่ใช่หลักฐาน GoF Factory Method
- JPA Specification, row locks, optimistic locking, DTO projections และ soft delete เป็น API/กลไกออกแบบ ไม่ใช่ GoF Strategy/State โดยอัตโนมัติ
- Refresh-token rotation, rate limiter และ ClientStatus ไม่ถูกนับเป็น State/Strategy เพิ่มเพื่อให้ครบจำนวน
- ยังไม่มี ProductivityMetricStrategy หลาย implementations ตามแผนใน REQUIREMENTS จึงไม่อ้างว่า feature นี้เสร็จแล้ว

## 5. หลักฐานการทดสอบ

- Authentication: AuthServiceTest, JwtAuthenticationFilterTest, LoginAttemptLimiterTest, RefreshTokenServiceTest และ UserAuthIntegrationTest
- State/Task: ProjectStateTest, ProjectServiceImplTest, TaskServiceImplTest
- Observer: TimerStoppedProgressListenerTest, ProjectProgressThresholdsTest, ProjectProgressThresholdListenerTest
- Client: ClientRepositoryTest, ClientServiceImplTest, ClientControllerTest, ClientIntegrationTest
- Time Tracking: TimeEntryTest, TimerServiceImplTest, TimeEntryServiceImplTest, TimeEntryRepositoryTest, TimeEntryIntegrationTest
- Analytics: DashboardIntegrationTest, ReportServiceImplTest, ReportIntegrationTest; Frontend dashboard/report/csv utility tests

อ้างอิง test source ที่มีอยู่ ไม่ใช่ผลรันใหม่หรือ Test Report ของการส่งมอบ

## 6. แหล่งที่มาของฉบับรวม

- [Petpinyo - Authentication/Profile](V1/DESIGN/petpinyo-design.md)
- [Thirawat - Client](V1/DESIGN/thirawat-design.md)
- [Kantavit - Project/Task และ Progress](V1/DESIGN/kantavit-design.md)
- [Kompat - Time Tracking](V1/DESIGN/kompat-design.md)
- [Nattadol - Dashboard/Reports](V1/DESIGN/nattadol-design.md)

Diagram ที่มีต้นฉบับถูกคัดลอกและตรวจชื่อ method/path ใน [diagrams/](diagrams/README.md) โดยคงเอกสารสมาชิกเดิมไว้ ส่วน diagram ที่ไม่มีต้นฉบับและสไลด์ยังไม่ถูกสร้างในงานรวมเอกสารรอบนี้


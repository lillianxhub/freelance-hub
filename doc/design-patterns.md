# Design Patterns - Freelance Hub

ฉบับรวมสำหรับส่งรายวิชา CP353002 ตรวจจาก implementation ณ commit `dbcc4b9` วันที่ 10 ตุลาคม 2026 ขอบเขตคือ Authentication/Profile, Client, Project/Task, Time Tracking และ Dashboard/Reports; ไม่รวม Finance, Invoice หรือ Payment

## 1. Enterprise / Architectural Patterns

| Pattern | ปัญหาที่แก้ | ไฟล์/คลาสที่ใช้ | Diagram |
|---|---|---|---|
| Layered Architecture | ไม่ให้ HTTP handler รับผิดชอบ business logic และ persistence พร้อมกัน | `controller/` → `service/` / `service/impl/` → `repository/` → `domain/` | [Application layers](diagrams/class-diagram.md#application-layers) |
| MVC / REST Presentation | แยก routing/status/validation ออกจาก business use cases และ React View | AuthController, ClientController, ProjectController, TimerController, DashboardController, ReportController; `code/Frontend/src/` | [Component view](diagrams/component.md) |
| Repository | ซ่อน JPA data access, derived queries, JPQL, Specification และ aggregate projections จาก controller | UserRepository, ClientRepository, ProjectRepository, TaskRepository, TimeEntryRepository, ReportQueryRepository | [Application layers](diagrams/class-diagram.md#application-layers) |
| Service Layer | รวม owner checks, กฎสถานะ, transaction และการประสานงานหลาย entity | AuthServiceImpl, ClientServiceImpl, ProjectServiceImpl, TaskServiceImpl, TimerServiceImpl, TimeEntryServiceImpl, DashboardServiceImpl, ReportServiceImpl | [Project status sequence](diagrams/sequence-diagram.md#scenario-03-change-project-status) |
| DTO + Mapper | กำหนด API contract ไม่ส่ง JPA entities หรือ password/token hashes ออกไป | `dto/request/`, `dto/response/`; UserMapper, ClientMapper, ProjectMapper, TaskMapper, TimeEntryMapper | [Domain model และ layers](diagrams/class-diagram.md) |
| Dependency Injection | เปลี่ยน dependency/test double โดยไม่สร้าง repository หรือ clock ใน use case | Constructor injection ใน services/controllers; PasswordEncoder bean; Clock จาก TimeConfiguration; CurrentUserProvider | [Authentication patterns](diagrams/class-diagram.md#authentication-patterns) |

เส้นทาง Backend ในตารางเริ่มจาก `code/Backend/src/main/java/th/ac/kku/freelance_hub/` Repository interfaces ใช้ Spring Data JPA; `@Query` ใน ClientRepository เป็น JPQL ไม่ใช่ native SQL ส่วน ReportQueryRepository เป็น concrete repository ที่ใช้ JPA EntityManager ทั้ง JPQL และ native SQL สำหรับจัดกลุ่มวัน/ชั่วโมงใน Asia/Bangkok จึงไม่อ้างว่าทุก repository ใช้แต่ JPQL ส่วน migration scripts เป็น SQL สำหรับสร้าง schema ไม่ใช่ query ใน controller

## 2. GoF Patterns: กลุ่ม Behavioral

ระบบใช้ Behavioral group โดยมี Strategy, Template Method และ Chain of Responsibility ผ่าน abstractions ของ Spring Security และมี Chain สำหรับแปลง error ที่ทีมเขียนเอง รวมถึง State กับ Observer ใน domain/workflow ของ Project/Time Tracking แยกหลักฐาน framework reuse ออกจาก pattern ที่ทีมเขียนเองดังนี้

| Pattern | ปัญหาที่แก้ | Implementation และที่มา | Class Diagram / Flow |
|---|---|---|---|
| Strategy | Auth/Profile เปลี่ยน password encoding โดยไม่เปลี่ยน use case | AuthServiceImpl และ UserService พึ่ง PasswordEncoder; SecurityConfig กำหนด BCryptPasswordEncoder เป็น implementation ปัจจุบัน **เป็นการใช้ strategy ของ framework ไม่ใช่ระบบ metric strategies ที่ทีมสร้าง** | [Authentication patterns](diagrams/class-diagram.md#authentication-patterns) |
| Template Method | ใช้ filter lifecycle ที่ framework จัดไว้และกำหนดเฉพาะขั้นตรวจ JWT | JwtAuthenticationFilter สืบทอด OncePerRequestFilter และ override doFilterInternal | [Authentication patterns](diagrams/class-diagram.md#authentication-patterns) |
| Chain of Responsibility | ส่ง HTTP request ผ่าน security filters ก่อน Controller | SecurityConfig สร้าง SecurityFilterChain และวาง JwtAuthenticationFilter ด้วย addFilterBefore โดยอ้างตำแหน่ง UsernamePasswordAuthenticationFilter; ไม่ได้เปิด form login จึงไม่ถือว่า filter อ้างอิงนี้ต้องมี instance ใน chain; JWT filter ส่งต่อผ่าน FilterChain หรือคืน error ตามหน้าที่ | [Authentication patterns](diagrams/class-diagram.md#authentication-patterns) |
| Chain of Responsibility (Error) | รวมการแปลง exception ของ MVC และ Security | ErrorHandlerChain เรียง ErrorHandler ตาม order และหยุดที่ตัวแรกที่คืน descriptor; fallback ตัวเดียวท้ายสุด; ApiErrorFactory ประกอบ response | [Error flow และ source](error-contract.md#flow) |
| State | แยกกฎ transition และสิทธิ์ของ Project แต่ละสถานะ | ProjectState, PlannedState, ActiveState, OnHoldState, CompletedState, ArchivedState; ProjectStates.from เลือก state ตาม ProjectStatus ที่เก็บใน DB | [Project State](diagrams/class-diagram.md#project-state-pattern) |
| Observer | เมื่อ timer หยุด ให้ workflow ความคืบหน้าตอบสนองโดยไม่ฝัง logic ใน TimerService | TimerServiceImpl เผยแพร่ TimerStoppedEvent; TimerStoppedProgressListener รับหลัง commit และเผยแพร่ ProjectProgressThresholdEvent; ProjectProgressThresholdListener เขียน log | [Observer sequence](diagrams/sequence-diagram.md#scenario-06-progress-threshold-events) |

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

### Authentication/Profile: ขอบเขต dependency ตาม PR #127

[AuthController](../code/Backend/src/main/java/th/ac/kku/freelance_hub/controller/AuthController.java) รับ AuthService, RefreshTokenCookie และ TrustedOriginValidator แยก HTTP/cookie/origin validation ออกจาก use case ส่วน [AuthServiceImpl](../code/Backend/src/main/java/th/ac/kku/freelance_hub/service/impl/AuthServiceImpl.java) รับ AuthenticationManager, LoginAttemptLimiter และ PlatformTransactionManager เพิ่มจาก repository/encoder/mapper/token components

[UserService](../code/Backend/src/main/java/th/ac/kku/freelance_hub/service/UserService.java) โหลด/บันทึก User และ Profile ผ่าน UserRepository กับ cascade ของ User.profile; UserProfileRepository มีอยู่แต่ไม่ได้ถูกเรียกใน flow นี้ [RefreshTokenService](../code/Backend/src/main/java/th/ac/kku/freelance_hub/service/RefreshTokenService.java) พึ่ง RefreshTokenRepository และจัด issue/rotate/revoke ภายใน token lifecycle

[SecurityConfig](../code/Backend/src/main/java/th/ac/kku/freelance_hub/config/SecurityConfig.java) กำหนด DaoAuthenticationProvider ที่พึ่ง CustomUserDetailsService และ PasswordEncoder; ไม่ใช่ AuthServiceImpl เรียก UserDetailsService โดยตรง หลักฐานเหล่านี้แสดงใน [Authentication Class Diagram](diagrams/class-diagram.md#authentication-patterns) และ [Login Sequence](diagrams/sequence-diagram.md#scenario-01-login-and-protected-request) ไม่ได้นับ token rotation หรือ limiter เป็น GoF pattern เพิ่ม

### Client: archive ไม่ใช่ soft delete

`PATCH /api/clients/{id}/status` ใช้ isActive; เมื่อ false จะตรวจว่าไม่มี timer ทำงานใน Project ของลูกค้านั้น (หากมีคืน 409) แล้วเปลี่ยน Project ของ Client ที่ยังไม่ถูก soft delete เป็น ARCHIVED โดยเรียก Project.changeStatus และไม่ตั้ง deletedAt การเปิด Client กลับมาไม่คืนสถานะ Project อัตโนมัติ

`DELETE /api/clients/{id}` ตั้งเฉพาะ Client.deletedAt คง isActive เดิม และคืน 204 โดยไม่มี body เส้นนี้ไม่เปลี่ยนสถานะ Project และไม่ผ่าน running-timer guard ของ Project API

ClientStatus ใน response คำนวณจาก isActive ไม่มี status column ใน clients; `include=projects` / `include=projects.tasks` ยืมรูปแบบ relationship path แต่ response ยังคง ApiResult ไม่ใช่ JSON:API เต็มรูปแบบ

### Analytics และเวลา

Client GET totalTrackedSeconds รวม Time Entry ที่ active และจบแล้วตลอดช่วงข้อมูล รวมประวัติบน Project/Task ที่ archive/soft delete เช่นเดียวกับ Time Entry summary ส่วน service method summarizeTimeByClient และ analytics queries บางชุดตัด inactive/deleted Project/Task ออก จึงต้องเลือกกติกาให้ตรง use case ไม่ถือว่าทุกยอดมีความหมายเดียวกัน

กติกาแต่ละชุดเทียบไว้ใน [ตารางขอบเขตยอดเวลา](use-case-description.md#ตารางขอบเขตยอดเวลา) โดย Reports ยังรวม Project/Client ที่ archive แต่ไม่ถูก soft delete และรวมประวัติของ Task ที่ถูก soft delete ต่างจาก Dashboard; การจัดกลุ่มใช้ startedAt ไม่ได้แบ่งรายการที่ข้ามวันเป็นหลายช่วง

Dashboard และ Reports ไม่สร้างตารางสรุปใหม่ กราฟใช้วินาทีจนถึงชั้นแสดงผล; React utilities อยู่ใน `lib/dashboard.ts`, `utils/duration.ts` และ `utils/csv.ts` ไม่ใช่ชื่อไฟล์เก่าที่ถูกย้ายไปแล้ว

## 4. รูปแบบที่ไม่ควรนับเป็น GoF เพิ่ม

- Static factories เช่น TimeEntry.startTimer/createManual ไม่ใช่ GoF Factory Method ที่มี creator hierarchy
- ApiErrorFactory เป็น Factory สำหรับสร้าง error envelope จาก ErrorDescriptor โดยไม่ตัดสิน mapping; ไม่ใช่ GoF Factory Method ที่ใช้ subclass override การสร้าง object ดู [Error Contract](error-contract.md)
- JPA Specification, row locks, optimistic locking, DTO projections และ soft delete เป็น API/กลไกออกแบบ ไม่ใช่ GoF Strategy/State โดยอัตโนมัติ
- Refresh-token rotation, rate limiter และ ClientStatus ไม่ถูกนับเป็น State/Strategy เพิ่มเพื่อให้ครบจำนวน

## 5. หลักฐานการทดสอบ

- Authentication: AuthServiceTest, JwtAuthenticationFilterTest, LoginAttemptLimiterTest, RefreshTokenServiceTest และ UserAuthIntegrationTest
- State/Task: ProjectStateTest, ProjectServiceImplTest, TaskServiceImplTest
- Observer: TimerStoppedProgressListenerTest, ProjectProgressThresholdsTest, ProjectProgressThresholdListenerTest
- Client: ClientRepositoryTest, ClientServiceImplTest, ClientControllerTest, ClientIntegrationTest
- Time Tracking: TimeEntryTest, TimerServiceImplTest, TimeEntryServiceImplTest, TimeEntryRepositoryTest, TimeEntryIntegrationTest
- Analytics: DashboardIntegrationTest, ReportServiceImplTest, ReportIntegrationTest; Frontend dashboard/report/csv utility tests

อ้างอิง test source ที่มีอยู่ ไม่ใช่ผลรันใหม่หรือ Test Report ของการส่งมอบ

## 6. ผู้รับผิดชอบและเอกสารประกอบ

- Petpinyo - Authentication/Profile
- Thirawat - Client
- Kantavit - Project/Task และ Progress
- Kompat - Time Tracking
- Nattadol - Dashboard/Reports

Diagram ใน [diagrams/](diagrams/README.md) ตรวจชื่อ method/path จาก implementation แล้ว รวมถึง Conceptual Domain/Use Case/Deployment/State diagrams มี [สไลด์นำเสนอ PDF](slide/slide-freelance-hub.pdf) ใน slide/ แล้ว ผลรันทดสอบจริงดู [Test Report](../test/test-report.md) แยกจากรายชื่อ test source ในเอกสารนี้


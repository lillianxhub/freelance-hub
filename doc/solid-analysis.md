# SOLID Analysis - Freelance Hub

ฉบับรวมสำหรับส่งรายวิชา CP353002 จากงาน Authentication/Profile, Client, Project/Task, Time Tracking และ Dashboard/Reports ของสมาชิกทั้ง 5 คน ตรวจอ้างอิง source ณ commit `dbcc4b9` วันที่ 10 ตุลาคม 2026

## 1. วิธีอ่านหลักฐาน

- เส้นทาง Backend ในตารางเริ่มจาก `code/Backend/src/main/java/th/ac/kku/freelance_hub/`
- เส้นทาง Frontend เริ่มจาก `code/Frontend/src/`
- เลขบรรทัดอ้างอิง declaration แรกในแต่ละไฟล์
- รายละเอียดการออกแบบดู [Design Patterns](design-patterns.md) และพฤติกรรมดู [Use Case Description](use-case-description.md)

## 2. Single Responsibility Principle (SRP)

แยก HTTP contract, business use case, mapping, persistence และ error construction ไม่รวมทุกหน้าที่ใน controller

| Feature | ไฟล์/คลาส | บรรทัด | เหตุผลและขอบเขต |
|---|---|---:|---|
| Authentication/Profile | `controller/AuthController.java`; `mapper/UserMapper.java`; `service/RefreshTokenService.java` | 32; 14; 24 | Controller จัด HTTP/cookie, mapper แปลง DTO, token service ดูแล issue/rotate/revoke; ไม่เก็บ password หรือ token hash ใน response |
| Client | `controller/ClientController.java`; `mapper/ClientMapper.java`; `common/response/ApiErrorFactory.java` | 45; 14; 12 | แยก HTTP, PUT/PATCH mapping และการสร้าง error metadata; aggregate เวลาอยู่ใน service/repository ไม่อยู่ใน mapper |
| Project/Task | `mapper/ProjectMapper.java`; `mapper/TaskMapper.java`; `domain/state/ProjectState.java`; `domain/progress/ProjectProgressThresholds.java` | 12; 8; 5; 5 | แยก mapping, กฎสถานะ และการคำนวณเกณฑ์ออกจาก service ที่ประสาน use case |
| Time Tracking | `controller/TimerController.java`; `controller/TimeEntryController.java`; `mapper/TimeEntryMapper.java`; `exception/handling/ApiExceptionHandler.java` | 35; 47; 14; 10 | แยก HTTP contract, DTO mapping และการแปลง exception; ไม่ใช้ TimeEntryServiceImpl ที่รวม CRUD/query/analytics/locking เป็นตัวอย่างว่ามีหน้าที่เดียว |
| Dashboard/Reports | `controller/DashboardController.java`; `service/impl/DashboardServiceImpl.java`; `service/impl/ReportServiceImpl.java` | 24; 43; 44 | แยก HTTP ออกจากการกำหนดช่วงเวลา สูตร KPI และการประกอบ read models |
| Analytics Frontend | `Analytics/DashboardContext.tsx`; `services/report.ts`; `utils/csv.ts`; `lib/dashboard.ts` | 7; 12; 1; 5 | แยกการโหลดข้อมูล, API contract, CSV serialization และการแปลงจุดกราฟออกจาก component |

ข้อจำกัด: `ClientServiceImpl`, `ProjectServiceImpl` และ `TimeEntryServiceImpl` ยังมีหลาย operations ใน feature เดียว ส่วน ReportsPage รวม filter/loading/กราฟ/CSV ไว้ในหน้าเดียว จึงไม่อ้างว่า SRP สมบูรณ์ทุก class

## 3. Open/Closed Principle (OCP)

จุดต่อขยายที่มีจริงคือ strategy ของ password encoding, Project State และ event listeners

| Feature | ไฟล์/คลาส | บรรทัด | เหตุผลและขอบเขต |
|---|---|---:|---|
| Authentication | `config/SecurityConfig.java`; `service/impl/AuthServiceImpl.java` | 32; 31 | Service เรียก PasswordEncoder interface; เปลี่ยน encoder ที่ configuration โดยไม่แก้ขั้นตอน login/register |
| Client | `service/ClientService.java`; `controller/ClientController.java` | 14; 45 | Controller พึ่ง contract จึงเปลี่ยน implementation ที่รักษาพฤติกรรมได้; เป็น extension point บางส่วน ไม่ใช่หลักฐานเพิ่ม algorithm ใหม่แล้ว |
| Project | `domain/state/ProjectState.java`; `domain/state/ProjectStates.java`; `domain/entity/Project.java` | 5; 8; 68 | เปลี่ยนกฎสถานะเดิมใน State class; เพิ่มสถานะใหม่ยังต้องแก้ enum และตัวเลือกใน ProjectStates |
| Time Tracking/Progress | `service/impl/TimerServiceImpl.java`; `event/TimerStoppedProgressListener.java`; `event/ProjectProgressThresholdListener.java` | 35; 16; 9 | หยุด timer แล้ว publish event; เพิ่มผู้รับ event ที่รักษา contract ได้โดยไม่ใส่ logic ปลายทางใน TimerService |
| Dashboard/Reports | `service/DashboardService.java`; `service/ReportService.java`; `Analytics/DashboardContext.tsx` | 7; 17; 7 | แยก contracts และการโหลดข้อมูล; การเพิ่ม KPI/period/response fields ยังต้องแก้ service/type/page ไม่ได้เป็น Strategy ของ analytics |

## 4. Liskov Substitution Principle (LSP)

implementation ต้องรักษา preconditions, ผลลัพธ์และข้อผิดพลาดที่ผู้ใช้ contract คาดหวัง ไม่ใช่เพียง implement method ครบ

| Contract | ไฟล์/คลาส | บรรทัด | พฤติกรรมที่ต้องรักษา |
|---|---|---:|---|
| Authentication | `service/AuthService.java`; `service/impl/AuthServiceImpl.java` | 7; 31 | Email normalization, password verification/hash และ session/refresh semantics |
| Current user | `service/CurrentUserProvider.java`; `service/UserService.java` | 6; 28 | คืน UUID ของ authenticated user ไม่รับ owner ID ที่ผู้เรียกระบุเอง |
| Client | `service/ClientService.java`; `service/impl/ClientServiceImpl.java` | 14; 42 | Owner isolation, not-found สำหรับข้อมูลของผู้อื่น/ที่ลบ, PUT ล้าง optional fields แต่ PATCH คงค่าที่ไม่ส่ง |
| Project/Task | `service/ProjectService.java`; `service/TaskService.java`; `service/impl/ProjectServiceImpl.java`; `service/impl/TaskServiceImpl.java` | 16; 13; 53; 41 | ตรวจ owner, transition, running timer, Task completion และการล็อกเวลาเมื่อปิด Project |
| Time Tracking | `service/TimerService.java`; `service/TimeEntryService.java`; `service/impl/TimerServiceImpl.java`; `service/impl/TimeEntryServiceImpl.java` | 8; 21; 35; 48 | หนึ่ง running timer ต่อ owner, ระยะเวลาเป็นวินาที, lock/delete/update rules และ transaction ของ lockByProject |
| Dashboard/Reports | `service/DashboardService.java`; `service/ReportService.java`; `service/impl/DashboardServiceImpl.java`; `service/impl/ReportServiceImpl.java` | 7; 17; 43; 44 | อ่านเฉพาะ owner, ไม่นับ timer ที่ยังไม่จบ และคืน empty/zero/null ตามความหมายของ metric |

แต่ละ service มี production implementation หลักเพียงตัวเดียว หลักฐานจึงเป็น contract และ test ของ implementation ปัจจุบัน ไม่ใช่ข้อพิสูจน์ว่า implementation ในอนาคตทดแทนได้เสมอ โดยเฉพาะ Project State แต่ละตัวตั้งใจมีสิทธิ์ต่างกันตามสถานะ ไม่ควรอ้างว่าทุก State มีผลลัพธ์เหมือนกัน

## 5. Interface Segregation Principle (ISP)

| Feature | ไฟล์/คลาส | บรรทัด | เหตุผลและข้อจำกัด |
|---|---|---:|---|
| Authentication/Profile | `service/AuthService.java`; `service/CurrentUserProvider.java` | 7; 6 | Auth contract ไม่รวม CRUD ของ business features; consumer ที่ต้องการ owner ID ใช้ interface method เดียว |
| Client | `service/CurrentUserProvider.java`; `service/ClientService.java` | 6; 14 | Controller ไม่ต้องพึ่ง profile/password operations; ClientService ยังรวม CRUD กับ summarizeTimeByClient ที่ controller ไม่เรียก |
| Project/Task | `service/ProjectService.java`; `service/TaskService.java` | 16; 13 | แยก Project และ Task contracts; ProjectService ยังรวมงาน CRUD กับ summary สำหรับ Dashboard |
| Time Tracking | `service/TimerService.java`; `service/TimeEntryService.java` | 8; 21 | แยกวงจรชีวิต timer ออกจากงานบันทึก/อ่าน/รวม/ล็อกเวลา; TimeEntryService ยังเป็น contract ขนาดใหญ่ใน feature |
| Dashboard/Reports | `service/DashboardService.java`; `service/ReportService.java` | 7; 17 | Controller แต่ละหน้าพึ่งเฉพาะ contract ของตน ไม่ต้องรู้ทุก method ของอีกหน้า |

## 6. Dependency Inversion Principle (DIP)

| Feature | ไฟล์/คลาส | บรรทัด | Abstraction และวิธีรับ dependency |
|---|---|---:|---|
| Authentication | `service/impl/AuthServiceImpl.java`; `config/SecurityConfig.java` | 31; 32 | Constructor injection ผ่าน Lombok; พึ่ง PasswordEncoder, AuthenticationManager, UserRepository และ PlatformTransactionManager |
| Client | `controller/ClientController.java`; `service/impl/ClientServiceImpl.java` | 45; 42 | Controller รับ ClientService/CurrentUserProvider; service รับ repository interfaces ผ่าน constructor |
| Project/Task | `controller/ProjectController.java`; `controller/TaskDetailController.java`; `service/impl/ProjectServiceImpl.java` | 42; 34; 53 | Controller พึ่ง service/current-user interfaces; service รับ repository, TimeEntryService, TimerService และ ApplicationEventPublisher |
| Time Tracking | `service/impl/TimerServiceImpl.java`; `service/impl/TimeEntryServiceImpl.java` | 35; 48 | Inject repositories, mapper และ Clock; test เปลี่ยนเป็น Clock.fixed ได้โดยไม่แก้ business logic |
| Dashboard/Reports | `controller/DashboardController.java`; `controller/ReportController.java`; `service/impl/DashboardServiceImpl.java`; `service/impl/ReportServiceImpl.java` | 24; 36; 43; 44 | Controller พึ่ง interface; Dashboard รับ repository interfaces/TimeEntryService/Clock แต่ Report รับ ReportQueryRepository ซึ่งเป็น concrete class และ Clock จึงเป็น DIP บางส่วน |

ข้อจำกัด: UserService, mapper, ApiErrorFactory, RefreshTokenService และ JPA EntityManager บางจุดยังเป็น concrete dependencies; Frontend services เป็น concrete functions การแยกไฟล์หรือใช้ hook ไม่ใช่หลักฐาน DIP ของทั้งระบบโดยอัตโนมัติ

### Authentication/Profile: หลักฐานที่ตรวจเพิ่มจาก PR #127

- SRP: [AuthController](../code/Backend/src/main/java/th/ac/kku/freelance_hub/controller/AuthController.java) จัด HTTP/cookie และ TrustedOriginValidator ตรวจ origin; [UserMapper](../code/Backend/src/main/java/th/ac/kku/freelance_hub/mapper/UserMapper.java) แยก PATCH mapping; [LoginAttemptLimiter](../code/Backend/src/main/java/th/ac/kku/freelance_hub/security/LoginAttemptLimiter.java) แยก email/IP failure counters
- OCP/ISP: [AuthService](../code/Backend/src/main/java/th/ac/kku/freelance_hub/service/AuthService.java) เป็น auth contract และ [CurrentUserProvider](../code/Backend/src/main/java/th/ac/kku/freelance_hub/service/CurrentUserProvider.java) มี currentUserId เพียง method เดียว; ไม่ถือว่าการมี interface อย่างเดียวพิสูจน์ LSP/OCP สมบูรณ์
- DIP: [AuthServiceImpl](../code/Backend/src/main/java/th/ac/kku/freelance_hub/service/impl/AuthServiceImpl.java) inject UserRepository, PasswordEncoder, AuthenticationManager และ PlatformTransactionManager; JwtTokenProvider, UserMapper, RefreshTokenService และ LoginAttemptLimiter ยังเป็น concrete dependencies จึงไม่อ้างว่าทั้งหมดเป็น interface
- [UserService](../code/Backend/src/main/java/th/ac/kku/freelance_hub/service/UserService.java) พึ่ง UserRepository ไม่ใช่ UserProfileRepository สำหรับ profile/password; UserMapper และ RefreshTokenService เป็น concrete dependencies แม้รับผ่าน constructor
- ข้อจำกัด: AuthServiceImpl.login ยังใช้ RuntimeException เมื่อ authentication ผ่านแต่ค้น user ไม่พบ ส่วน UserService.getCurrentUserEmail ใช้ AuthenticationRequiredException เมื่อไม่มี authentication กรณีค้น user ไม่พบใน AuthServiceImpl ใช้ fallback 500; กรณีไม่มี authentication ใน UserService คืน 401

## 7. หลักฐานการทดสอบและขอบเขต

Test source อยู่ใน `code/Backend/src/test/java/th/ac/kku/freelance_hub/`:

| Feature | Test ที่ใช้อ้างอิง |
|---|---|
| Authentication/Profile | AuthServiceTest, AuthControllerTest, UserAuthIntegrationTest, UserServiceTest, RefreshTokenServiceTest, LoginAttemptLimiterTest |
| Client | ClientServiceImplTest, ClientControllerTest, ClientRepositoryTest, ClientIntegrationTest |
| Project/Task | ProjectStateTest, ProjectServiceImplTest, TaskServiceImplTest, ProjectProgressThresholdsTest |
| Time Tracking/Events | TimeEntryTest, TimerServiceImplTest, TimeEntryServiceImplTest, TimeEntryRepositoryTest, TimeEntryIntegrationTest, TimerStoppedProgressListenerTest |
| Dashboard/Reports | DashboardIntegrationTest, ReportControllerTest, ReportServiceImplTest, ReportIntegrationTest |
| Frontend Analytics | `code/Frontend/src/Analytics/dashboard.test.tsx`, `code/Frontend/src/lib/dashboard.test.ts`, `code/Frontend/src/services/report.test.ts`, `code/Frontend/src/utils/csv.test.ts` |

รายการนี้ระบุ test ที่มีใน repository ไม่ใช่ Test Report และไม่ยืนยันผลรันล่าสุด H2-based integration tests ไม่ใช่หลักฐานว่า PostgreSQL migrations ผ่าน ต้องตรวจ CI/Flyway และรายงานผลแยกก่อนส่ง

## 8. ผู้รับผิดชอบเอกสาร

- Petpinyo - Authentication/Profile
- Thirawat - Client
- Kantavit - Project/Task
- Kompat - Time Tracking
- Nattadol - Dashboard/Reports

## Error handling หลังย้าย Chain + Factory

- SRP: ApiException เก็บ public error contract, handlers แปลง exception, Chain เลือก handler, Factory สร้าง envelope/metadata และ entry points จัดการ transport
- OCP: เพิ่ม subclass ของ ApiException โดยไม่เพิ่ม mapping; เพิ่ม handler สำหรับ exception ภายนอกผ่าน ErrorHandler interface
- DIP: Chain พึ่ง ErrorHandler interface; MVC/Security ยังพึ่ง concrete chain/factory ตาม constructor injection ไม่อ้างว่าทั้งระบบใช้ abstraction ทุกชั้น
- ตรวจ order ไม่ซ้ำและ fallback ท้ายสุดตอนเริ่มแอป พร้อม tests ของ first-match/unknown failure

ดู [Error Contract พร้อม source references](error-contract.md) สำหรับ schema, flow และการเพิ่ม exception

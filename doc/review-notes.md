# บันทึกตรวจความสอดคล้องเอกสารกับระบบ

ตรวจวันที่ 9 ตุลาคม 2026 ณ commit `131305f` หลัง pull dev; การเปลี่ยนจากเพื่อนใน `f0a949c` (PR #125) แก้เอกสาร Time Tracking ของ Kompat 3 ไฟล์ ไม่เปลี่ยน production code, schema หรือ workflow จาก snapshot `cb8002d` ที่ใช้รวมเอกสารครั้งก่อน

บันทึกนี้เป็น source-based documentation review ไม่ใช่ Test Report หรือการรับรองว่า requirement ทั้งหมดถูก implement แล้ว รอบนี้แก้เฉพาะเอกสารฉบับรวม ไม่แก้ Backend/Frontend/migrations และคงต้นฉบับ V1 ของสมาชิก

## สิ่งที่แก้ในฉบับรวม

| หัวข้อ | จุดที่ไม่ตรงหรือไม่ชัดก่อนตรวจ | การแก้ |
|---|---|---|
| Timer Activity | สร้าง TimeEntry หลังตรวจ running timer ต่างจากโค้ดและ PR #125 | เปลี่ยนเป็น validate Project/Task → TimeEntry.startTimer/ตรวจกฎ domain → ตรวจ running timer → saveAndFlush → Task.start |
| SOLID | ยก TimeEntryServiceImpl เป็นตัวอย่าง SRP ทั้งที่รวม CRUD/query/analytics/lock และบางแถวไม่บอก implementation filename | ใช้ controller/mapper/exception handler เป็นหลักฐาน SRP; ระบุชื่อไฟล์และขอบเขต LSP/ISP/DIP ที่ยังเป็นบางส่วน |
| Data access | ผู้อ่านอาจเข้าใจว่าทั้งระบบใช้แต่ Spring Data/JPQL | แยก ClientRepository JPQL ออกจาก ReportQueryRepository ที่ใช้ EntityManager และ native SQL สำหรับวัน/ชั่วโมง |
| Client API | ขาด defaults/fields/filter ของ include และการค้นหา | ระบุ page 1, limit/size, sort keys, prefix LIKE, include เฉพาะ detail และ fields ของ Project/Task ที่คืนจริง |
| Time Entry API | ไม่ชัดว่า list รวม running timer และชื่อ Task ใน nested DTO ต่างจาก feature อื่น | ระบุ list รวม running timer, summary ไม่รวม และ nested DTO ใช้ task.title; ไม่เหมารวม service DTO กับ HTTP DTO |
| Dashboard | จำนวนรายการล่าสุดและฐานคำนวณ KPI ไม่ชัด | ระบุ recent entries 2 รายการตาม startedAt, projects/tasks ไม่เกิน 5, เป้าหมายใช้เวลาตลอดประวัติ และ activity รับ period ไม่ใช่ date-range chart endpoint |
| ยอดเวลาข้ามหน้า | ใช้คำว่า archive/soft delete กว้างเกินไปโดยไม่แจกแจง query | เพิ่มตารางเทียบ Client GET/internal summary, Dashboard และ Reports พร้อมความต่างของ Project/Task/Client visibility |
| วันที่และ trend | คำว่า “ถึงปัจจุบัน”/“ช่วงก่อน” อาจทำให้คาดหวังสูตรคนละแบบ | ระบุ startedAt attribution ไม่แบ่งข้ามวัน; Dashboard เทียบสัปดาห์นี้ถึงสิ้นวันนี้กับสัปดาห์ก่อนทั้งสัปดาห์; Reports เทียบช่วงติดกันที่ยาวเท่ากัน |
| Authentication | อายุ token ดูเหมือนตายตัว และ IP limiter ดูเหมือนจำกัดทุก request | ระบุค่าเริ่มต้น/configuration, limiter นับ BadCredentialsException ใน memory และเริ่ม 429 ที่คำขอถัดจากการครบจำนวน |
| Schema/ER | owner_id ของ Time Entry ไม่กล่าวถึง direct users FK และ running-timer index ดูเหมือนกรอง active | แก้ FK/index description; แยก application enum ออกจาก DB CHECK และระบุ V14 ไม่ทำให้ Project.status/is_active สอดคล้องย้อนหลังทุกแถว |
| CI/CD | กราฟแสดง Frontend gate → Vercel ทำให้ดูเหมือนมี deployment dependency | แสดง Vercel Git integration แยกจาก Actions และระบุ PR docs-only อาจ skipped tests แต่ gate ยังผ่าน |

## หลักฐานที่เทียบ

| ส่วน | Source ที่ใช้ตรวจ |
|---|---|
| Auth/Profile/Security | AuthController, UserController, AuthServiceImpl, UserService, RefreshTokenService, LoginAttemptLimiter, SecurityConfig, JwtTokenProvider และ shared error/trace components |
| Client | ClientController, ClientFilterRequest, ClientServiceImpl, ClientMapper, ClientRepository และ Client/Address entities |
| Project/Task | ProjectController, TaskController, TaskDetailController, ProjectServiceImpl, TaskServiceImpl, ProjectState classes, repositories และ progress event listeners |
| Time Tracking | TimerController, TimeEntryController, TimerServiceImpl, TimeEntryServiceImpl, TimeEntry, TimeEntryRepository และ HTTP response DTOs |
| Dashboard/Reports | DashboardController/ServiceImpl, ReportController/ServiceImpl, ReportQueryRepository, request/response DTOs, ReportsPage, DashboardProvider และ DashboardTimerCard |
| Schema/automation | Flyway V1–V20, JPA entities, backend.yml, frontend-ci.yml และ test source filenames |
| ข้อกำหนด/ที่มา | REQUIREMENTS.md, เอกสารสมาชิก V1 และ diff ของ PR #125; ไม่ถือว่าข้อเสนอใน requirement เป็น implementation ที่มีแล้ว |

Backend source เริ่มจาก `code/Backend/src/main/java/th/ac/kku/freelance_hub/`; Frontend source เริ่มจาก `code/Frontend/src/` ตำแหน่งหลักฐานราย principle ดู [SOLID](solid-analysis.md) และรายละเอียดพฤติกรรมดู [Use Cases](use-case-description.md)

## ข้อจำกัดจริงที่ยังไม่แก้ production code

- Client soft delete ตั้ง deletedAt อย่างเดียว แต่ timer guard ตรวจ Client.isActive ไม่ตรวจ Client.deletedAt โดยตรง; Client archive cascade ไม่ผ่าน ProjectService running-timer guard และไม่หยุด timer ที่มีอยู่
- Client detail ยังไม่ส่งเวลาราย Project ตาม FR-CLI-04; FR-CLI-05 ยังต้องตกลงเรื่อง soft delete ของลูกค้าที่มีธุรกรรม
- รายการเวลาใหม่หรือที่ย้ายเข้า Project ที่ COMPLETED ไม่ถูก lock อัตโนมัติจาก lockByProject ซึ่งล็อกเฉพาะรายการที่พบขณะเรียก
- Project list คำนวณ timeTracking แยก Project และ summary โหลด entries มารวมใน Java; Reports แบ่งหน้า Project ใน memory ยังไม่ใช่ optimized database pagination ทุกหน้า
- Client/Dashboard/Reports ใช้เงื่อนไขนับเวลาต่างกัน เป็นพฤติกรรมปัจจุบันที่บันทึกไว้ ไม่ใช่การตัดสินว่าเป็น business rule ที่ทีมยอมรับแล้ว หากต้องการตัวเลขตรงกันทุกหน้าต้องตกลงกฎและแก้โค้ดอีกงาน
- Dashboard ยังไม่มี KPI วันนี้/เดือนนี้/completed projects ตาม requirement บางข้อ; Reports work-trend/work-pattern ยังไม่มี UI และ CSV ส่งออก Project ของหน้าปัจจุบัน ไม่ใช่ Time Entry ทุกแถว
- Observer threshold listener เขียน log เท่านั้น ยังไม่ส่ง notification ถึงผู้ใช้ และยังไม่มี ProductivityMetricStrategy ตามแผนใน REQUIREMENTS
- REQUIREMENTS API table ยังใช้ PATCH สำหรับ Time Entry แต่โค้ดใช้ PUT และมี summary เพิ่ม; ต้องให้ทีมยืนยัน/ปรับ contract ไม่เปลี่ยน requirement ให้เองจากการตรวจเอกสาร
- Use Case/Deployment/State Diagram ฉบับแยก, slides และ Test Report ยังไม่ครบตามรายการใน [ดัชนีเอกสาร](README.md) รอบนี้ไม่สร้างแทนจากข้อมูลที่ไม่มี

## ขอบเขตการตรวจยืนยัน

ตรวจชื่อไฟล์/test source, Markdown links/anchors/tables/code fences, Mermaid syntax และ git diff ของเอกสาร การตรวจเหล่านี้ไม่แทนการรัน Maven/Frontend tests, PostgreSQL migrations, การลอง Swagger ด้วยบัญชีจริง หรือการตรวจ cloud deployment; ไม่มีการประกาศ CI/test ผ่านจากการตรวจ source เพียงอย่างเดียว

ผลตรวจรอบนี้: Markdown ฉบับหลัก/diagram 20 ไฟล์ผ่านการตรวจโครงสร้างและลิงก์; Mermaid 16 diagram ผ่าน syntax parser; SOLID มี 76 line references ที่อยู่ใน source จริงและตรวจบริบทแล้ว; git diff --check ผ่าน ไม่มีการเปลี่ยนไฟล์ใน code/, .github/ หรือ doc/V1/

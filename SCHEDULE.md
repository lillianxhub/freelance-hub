# MVP Agile/Scrum Schedule

แผนนี้แยกออกจาก `REQUIREMENTS.md` เพื่อให้ `REQUIREMENTS.md` ใช้ระบุ requirement ของ MVP เท่านั้น

## 1. เป้าหมาย

ส่งมอบ MVP และนำเสนอวันที่ **10 ตุลาคม 2026** ภายในเวลา 3 สัปดาห์ โดย MVP มี 5 โมดูล:

1. Authentication
2. Client Management
3. Project / Task Management
4. Time Tracking
5. Dashboard / Analytics / Productivity Insights

Income, Expense, Invoice และ Payment ไม่อยู่ในแผนงานรอบนี้

## 2. สมาชิกและความรับผิดชอบ

ชื่อด้านล่างอ้างอิงจาก Git history ให้ตรวจสอบชื่อ-นามสกุล รหัสนักศึกษา Section และ branch ก่อนส่งงาน

| สมาชิก | งานหลักที่ต้อง implement | งานย่อยและ Definition of Done | เอกสาร/หลักฐาน |
|---|---|---|---|
| เพชรภิญโญ ธนศิรินรากร | Authentication และ Security | User/UserProfile One-to-One, register/login/logout, password hash, session/token, authorization ตาม owner, `@RestControllerAdvice` ส่วน auth, unit/integration/security tests | Use Case auth, security flow, SOLID Dependency Inversion, auth sequence diagram |
| F-Thirawat-M | Client Management | Client entity/repository/service/DTO/mapper/controller, CRUD ครบ, validation email/contact, archive, pagination/sorting, ownership test, API test | Client use case/sequence, Client data dictionary, CRUD API examples |
| kantavit447 | Project และ Task Management | Project/Task relation, target hours, status `PLANNED/ACTIVE/ON_HOLD/COMPLETED/ARCHIVED`, state transition, CRUD, validation, not-found/conflict tests | Domain model, class diagram, Project state diagram, State pattern analysis |
| kompeez | Time Tracking | timer start/stop/current, manual time entry, duration calculation, one-running-timer constraint, UTC/timezone, duplicate request protection, tests | Time Tracking sequence/activity, SRP/OCP/LSP analysis, API test evidence |
| Nattadol | Dashboard/Analytics และ Frontend | summary queries, tracked hours, utilization, project progress, productivity metrics, empty/loading/error state, Thymeleaf screens, integration tests | Component/deployment diagram, ISP analysis, dashboard screenshots, demo script |

### งานที่ทุกคนต้องทำร่วมกัน

- ทุกคนต้องมี production code และ test code ใน PR ของตนเอง ไม่รับผิดชอบเฉพาะเอกสาร
- ทุก feature ต้องใช้ Controller -> Service -> Repository และ DTO + Mapper; ห้ามข้าม Layer
- ทุกคนเพิ่ม unit/integration/security tests ของ feature ตนเอง และ review PR ของเพื่อนอย่างน้อย 2 PR
- ทุกคนเขียนอย่างน้อย 15 meaningful commits ด้วย GitHub account ของตนเอง กระจายตลอด 3 สัปดาห์
- ทุกคนอัปเดต `solid-analysis.md`, `design-patterns.md`, diagram หรือ README ในส่วนที่ตนรับผิดชอบ

## 3. Backlog และลำดับงาน

### Epic A: Foundation และ Authentication

- A1: ตั้งค่า package Layered Architecture, profile config, error response และ logging
- A2: ออกแบบ migration 6 ตาราง: users, user_profiles, clients, projects, tasks, time_entries
- A3: register/login/logout, password hashing และ validation
- A4: owner isolation และ security test ว่าผู้ใช้ A อ่านข้อมูลผู้ใช้ B ไม่ได้
- A5: เพิ่ม OpenAPI/Swagger configuration และ health endpoint

### Epic B: Client Management

- B1: Client entity, FK ไป User และ index สำหรับ owner/name
- B2: request/response DTO และ mapper
- B3: create/list/detail/update/archive
- B4: pagination, sorting และ filter
- B5: validation, 400/404/409 และ integration tests

### Epic C: Project และ Task Management

- C1: Project/Task entity และ One-to-Many relation
- C2: Project status และ State pattern
- C3: create/list/detail/update/archive project
- C4: create/update/complete/reorder task
- C5: target hours และ project progress
- C6: validation, conflict handling และ repository tests

### Epic D: Time Tracking

- D1: TimeEntry entity และ relation กับ Project/Task/User
- D2: start timer พร้อม running state
- D3: stop timer และ duration calculation
- D4: current timer และ resume หลัง refresh/login
- D5: manual time entry, edit/delete และ date range filter
- D6: guard timer ซ้ำ, UTC/timezone และ concurrency tests

### Epic E: Dashboard และ Productivity

- E1: query tracked hours วันนี้/สัปดาห์/เดือน/ช่วงวันที่
- E2: utilization, average hours/day และ busiest period
- E3: project progress และ target 80%/100% alert
- E4: productivity comparison ระหว่างช่วงวันที่
- E5: Thymeleaf dashboard responsive พร้อม loading/empty/error state
- E6: analytics integration tests และ dashboard screenshot

### Epic F: Course Deliverables และ Release

- F1: ER, domain, class, use case, sequence 3 scenarios, activity, component, deployment, project-state diagrams
- F2: `doc/solid-analysis.md`, `doc/design-patterns.md`, `doc/data-dictionary.md`
- F3: Dockerfile, docker-compose, PostgreSQL health check และ environment variables
- F4: test report, README, slide และ public deployment
- F5: release regression, security review, demo rehearsal และ merge `main`

## 4. Sprint Plan

| ช่วงเวลา | เป้าหมาย | งานหลัก | หลักฐานตรวจรับ |
|---|---|---|---|
| 19–25 ก.ย. | Foundation + Authentication | A1-A5 และ schema/migration | auth ใช้งานได้, DB ต่อได้, security test ผ่าน |
| 26 ก.ย.–2 ต.ค. | Core Workflow | B1-B5, C1-C6, D1-D6 | Client -> Project -> Task -> TimeEntry ครบ |
| 3–9 ต.ค. | Analytics + Release | E1-E6, F1-F5, bug fix | MVP demo, diagrams, tests, Docker และ deployment ครบ |
| 10 ต.ค. | Presentation | นำเสนอและตอบคำถาม | ทุกคนอธิบาย feature/architecture/test ของตนเองได้ |

## 5. Progress และ Scrum Ceremony

- Daily async stand-up: ทุกวันก่อน 20:00 น. รายงาน `เมื่อวาน / วันนี้ / blocker`
- Progress ทุกวันเสาร์: 19 ก.ย., 26 ก.ย. และ 3 ต.ค. เวลา `TODO: เวลา` ผ่าน `TODO: ช่องทาง`
- Sprint Planning: หลัง Progress ทุกวันเสาร์หรือเช้าวันอาทิตย์
- Backlog Refinement: กลางสัปดาห์ 30 นาที
- Sprint Review: demo งานที่รันได้จริงใน Progress วันเสาร์
- Retrospective: บันทึก `ทำได้ดี / ต้องปรับ / action owner` หลัง Progress
- Feature freeze: หลัง Progress วันที่ 3 ต.ค. เพิ่มได้เฉพาะ bug, security และ deployment fix

## 6. Git/PR และหลักฐานคะแนนเต็ม

- `main` = production, `develop` = integration, branch ส่วนตัว = `ชื่อ_รหัสนักศึกษา_section`
- ทุก PR ต้องมี linked issue, test evidence, reviewer อย่างน้อย 1 คน และ merge เข้า `develop` ก่อน
- ผู้เขียนห้าม approve PR ของตนเอง; สมาชิกแต่ละคน review เพื่อนอย่างน้อย 2 PR
- Git history ของแต่ละคนต้องมีอย่างน้อย 15 commits ที่มีความหมายและกระจายตลอดช่วงเวลา
- ห้ามฝาก commit/push, outsource หรือคัดลอกโค้ดจากกลุ่มอื่น
- ใน slide ให้ระบุชื่อสมาชิก, feature owner, PR, test และ commit หลักของแต่ละคน

## 7. Release Checklist

- [ ] MVP 5 modules ผ่าน acceptance criteria ใน `REQUIREMENTS.md`
- [ ] CRUD Client และ Project พร้อม validation, pagination/sorting และ status code ถูกต้อง
- [ ] Authentication และ owner isolation ผ่าน security tests
- [ ] Time tracking มี start/stop/manual/timezone/duplicate guard
- [ ] Dashboard มี tracked hours, utilization, project progress และ productivity insights
- [ ] PostgreSQL migration มี 6 ตาราง, FK, index, One-to-One และ One-to-Many
- [ ] Swagger UI, Global Exception Handler และ Thymeleaf ใช้งานได้
- [ ] JUnit/Mockito/Spring Boot tests ผ่านและมี report
- [ ] Docker Compose ทำงานได้ และ public deployment URL เปิดได้
- [ ] diagrams, SOLID, patterns, data dictionary, README และ slide ครบ
- [ ] สมาชิกทั้ง 5 คนมี code, tests, 15 commits, PR/review และหลักฐานการนำเสนอ

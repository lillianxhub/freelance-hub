# SOLID Analysis: Project และ Task Management

**เจ้าของ feature:** `kantavit_673380027-4_01`  
**ขอบเขต:** Project/Task API, business logic, Project State และ listener ความคืบหน้าเวลา
**วิธีอ้างอิง:** `ไฟล์:บรรทัด` ด้านล่างนับจาก `code/Backend/src/main/java/th/ac/kku/freelance_hub/` ณ เวอร์ชันที่อัปเดตเอกสารนี้

| Principle | หลักฐานในโค้ด | เหตุผลและขอบเขต |
|---|---|---|
| Single Responsibility | `controller/ProjectController.java:95`, `controller/TaskController.java:82`, `controller/TaskDetailController.java:51` | Controller รับ request, อ่าน owner ปัจจุบัน และจัด HTTP/`ApiResult`; ไม่ทำ query ฐานข้อมูลโดยตรง |
| Single Responsibility | `mapper/ProjectMapper.java:16,33`, `mapper/TaskMapper.java:11` | Mapper แปลง entity เป็น DTO; ProjectMapper สร้าง Client summary, Task progress และชั่วโมงเป้าหมาย ส่วน Service เติมเวลาใช้งานจริง |
| Single Responsibility | `domain/state/ProjectState.java:5`, `domain/entity/Project.java:185`, `domain/entity/Task.java:179,194` | State แต่ละคลาสถือกฎของสถานะ Project; Project ประสานการเปลี่ยนสถานะ; Task จัดการ transition และ soft delete ของตัวเอง |
| Single Responsibility | `domain/progress/ProjectProgressThresholds.java:5,40`, `event/TimerStoppedProgressListener.java:34`, `event/ProjectProgressThresholdListener.java:14` | แยกการคำนวณเกณฑ์ การตอบสนองต่อ timer stop และการเขียน log เป็นคนละหน้าที่ |
| Open/Closed | `domain/state/ProjectState.java:5`, `domain/state/ProjectStates.java:13`, `domain/state/ArchivedState.java:18` | เปลี่ยนกฎของสถานะเดิมใน State นั้นได้โดยไม่ต้องแก้ Controller; การเพิ่มสถานะใหม่ยังต้องแก้ enum และ `ProjectStates.from()` จึงเป็น OCP บางส่วน |
| Open/Closed | `event/ProjectProgressThresholdEvent.java:5`, `event/ProjectProgressThresholdListener.java:14` | เพิ่ม listener ที่รับ threshold event ได้โดยไม่ต้องแก้ตัวเผยแพร่ event แต่กฎเกณฑ์ 80/100 ยังอยู่ใน `ProjectProgressThresholds` |
| Liskov Substitution | `service/ProjectService.java:19`, `service/TaskService.java:17`, `service/impl/ProjectServiceImpl.java:51`, `service/impl/TaskServiceImpl.java:38` | Controller เรียกผ่าน service interface และ implementation ทำตาม contract; แต่ละ interface มี production implementation เดียว จึงยังไม่ใช่หลักฐานว่ามีหลาย implementation ที่แทนกันได้จริง |
| Interface Segregation | `service/ProjectService.java:19-93`, `service/TaskService.java:17-55` | แยก Project กับ Task contract จึงไม่บังคับ ProjectController ให้พึ่ง TaskService; อย่างไรก็ดี ProjectService รวม CRUD, รายการ และ method สรุปสำหรับ Dashboard จึงไม่ได้แยก interface ย่อยตามผู้ใช้ทุกกลุ่ม |
| Dependency Inversion | `controller/ProjectController.java:52`, `controller/TaskController.java:47`, `controller/TaskDetailController.java:38` | Controller พึ่ง `ProjectService`/`TaskService` interface และรับ dependency จาก Spring |
| Dependency Inversion | `service/impl/ProjectServiceImpl.java:55-76`, `service/impl/TaskServiceImpl.java:45-65`, `event/TimerStoppedProgressListener.java:19-32` | Service และ listener รับ repository/`TimeEntryQueryService`/`ApplicationEventPublisher` ผ่าน constructor; mapper, JPA `EntityManager` และ `UserService` ยังเป็น concrete dependency จึงไม่อ้างว่า DIP ครอบคลุมทุกจุด |

## หลักฐานตามการทำงานจริง

- `ProjectController.java:95-155` ใช้ `ProjectFilterRequest` และส่งการค้นหาให้ Service; `status=ALL` รวม `ARCHIVED` ส่วนไม่ส่ง status จะซ่อน `ARCHIVED` โดยยังตัด `deletedAt` ออก (`dto/request/ProjectFilterRequest.java:24-30`, `service/impl/ProjectServiceImpl.java:203-222`)
- `ProjectServiceImpl.java:116-130,269-319,371-389` ประกอบ Client/Task progress และ `timeTracking` จาก Time Entry ที่จบแล้วให้ทั้ง Project detail และ list; เมื่อไม่มี `targetMinutes`, `usagePercent` เป็น `null`
- `ProjectServiceImpl.java:135-179` มี `countActiveAndCompleted()` กับ `getProgress()` สำหรับ Dashboard; `TaskServiceImpl.java:131-144` มี `getLatestTimeEntryTaskName()` ทั้งหมดเป็น service method ไม่ใช่ HTTP endpoint ใหม่
- `Project.java:185-219` ให้ State ปัจจุบันตัดสิน transition, สิทธิ์จับเวลา และสิทธิ์แก้ Task; `ARCHIVED` ย้อนเป็น `ACTIVE` หรือ `PLANNED` ได้ถ้าเป็นการเปลี่ยนสถานะปกติ ส่วน `Project.archive()` ตั้ง `deletedAt` สำหรับ soft delete
- `TaskServiceImpl.java:67-103,179-199,232-290,303-311` ตรวจเจ้าของ/State ก่อนแก้ Task, จัดลำดับ และใช้ `Task.softDelete()` แทนลบแถว (`Task.java:194-199`)
- `TimerStoppedProgressListener.java:34-70` รับ `TimerStoppedEvent` หลัง commit, คำนวณเกณฑ์ใหม่ แล้วเผยแพร่ `ProjectProgressThresholdEvent`; `ProjectProgressThresholdListener.java:14-22` เขียน log ยังไม่มี notification ถึงผู้ใช้

## ข้อจำกัดของการวิเคราะห์

1. `ProjectServiceImpl` รวมหลาย operation ของ Project เพื่อรองรับ feature ปัจจุบัน จึงไม่ควรกล่าวว่าแต่ละคลาสมีหน้าที่เดียวอย่างสมบูรณ์ เพียงแต่แยก HTTP, mapping, domain rule และ event handling ออกจากกันแล้ว
2. `TaskStatus` เป็น enum พร้อมกฎใน `Task.changeStatus()` ไม่ใช่ GoF State pattern ชุดเดียวกับ Project
3. ตัวอย่าง LSP/DIP เป็นหลักฐานเชิงโครงสร้าง ไม่ใช่ข้อพิสูจน์ว่าเปลี่ยน implementation ใด ๆ ได้โดยไม่กระทบพฤติกรรม
4. เลขบรรทัดควรตรวจซ้ำหลัง merge ก่อนนำไปคัดลอกลงเอกสารหลักของทีม

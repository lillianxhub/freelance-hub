# SOLID Analysis: Project และ Task Management

**เจ้าของ feature:** `kantavit_673380027-4_01`  
**ขอบเขต:** Project/Task API, business logic, Project State และ listener ความคืบหน้าเวลา
**วิธีอ้างอิง:** `ไฟล์:บรรทัด` ด้านล่างนับจาก `code/Backend/src/main/java/th/ac/kku/freelance_hub/` ณ เวอร์ชันที่อัปเดตเอกสารนี้

| Principle | หลักฐานในโค้ด | เหตุผลและขอบเขต |
|---|---|---|
| Single Responsibility | `controller/ProjectController.java:58,86,158`, `controller/TaskController.java:82,116`, `controller/TaskDetailController.java:49,76,106` | Controller รับ request, อ่าน owner ปัจจุบัน และจัด HTTP/`ApiResult`; ไม่ทำ query ฐานข้อมูลโดยตรง |
| Single Responsibility | `mapper/ProjectMapper.java:14,31`, `mapper/TaskMapper.java:10` | Mapper แปลง entity เป็น DTO; ProjectMapper สร้าง Client summary, Task progress และชั่วโมงเป้าหมาย ส่วน Service เติมเวลาใช้งานจริง |
| Single Responsibility | `domain/state/ProjectState.java:5`, `domain/entity/Project.java:185`, `domain/entity/Task.java:179,201` | State แต่ละคลาสถือกฎของสถานะ Project; Project ประสานการเปลี่ยนสถานะ; Task จัดการ transition และ soft delete ของตัวเอง |
| Single Responsibility | `domain/progress/ProjectProgressThresholds.java:5,40`, `event/TimerStoppedProgressListener.java:34`, `event/ProjectProgressThresholdListener.java:14` | แยกการคำนวณเกณฑ์ การตอบสนองต่อ timer stop และการเขียน log เป็นคนละหน้าที่ |
| Open/Closed | `domain/state/ProjectState.java:5`, `domain/state/ProjectStates.java:13`, `domain/state/ArchivedState.java:18` | เปลี่ยนกฎของสถานะเดิมใน State นั้นได้โดยไม่ต้องแก้ Controller; การเพิ่มสถานะใหม่ยังต้องแก้ enum และ `ProjectStates.from()` จึงเป็น OCP บางส่วน |
| Open/Closed | `event/ProjectProgressThresholdEvent.java:5`, `event/ProjectProgressThresholdListener.java:14` | เพิ่ม listener ที่รับ threshold event ได้โดยไม่ต้องแก้ตัวเผยแพร่ event แต่กฎเกณฑ์ 80/100 ยังอยู่ใน `ProjectProgressThresholds` |
| Liskov Substitution | `service/ProjectService.java:16`, `service/TaskService.java:13`, `service/impl/ProjectServiceImpl.java:51`, `service/impl/TaskServiceImpl.java:37` | Controller เรียกผ่าน service interface และ implementation ทำตาม contract; แต่ละ interface มี production implementation เดียว จึงยังไม่ใช่หลักฐานว่ามีหลาย implementation ที่แทนกันได้จริง |
| Interface Segregation | `service/ProjectService.java:16-92`, `service/TaskService.java:13-56` | แยก Project กับ Task contract จึงไม่บังคับ ProjectController ให้พึ่ง TaskService; อย่างไรก็ดี ProjectService รวม CRUD, รายการ และ method สรุปสำหรับ Dashboard จึงไม่ได้แยก interface ย่อยตามผู้ใช้ทุกกลุ่ม |
| Dependency Inversion | `controller/ProjectController.java:43-44`, `controller/TaskController.java:46-47`, `controller/TaskDetailController.java:36-37`, `service/CurrentUserProvider.java:6` | Controller พึ่ง `ProjectService`/`TaskService` และ `CurrentUserProvider` ผ่าน interface; `UserService` เป็น implementation ของ `CurrentUserProvider` |
| Dependency Inversion | `service/impl/ProjectServiceImpl.java:59-87`, `service/impl/TaskServiceImpl.java:44-65`, `event/TimerStoppedProgressListener.java:18-30` | Service และ listener รับ repository/`TimeEntryService`/`TimerService`/`ApplicationEventPublisher` ผ่าน constructor; mapper และ JPA `EntityManager` ยังเป็น concrete dependency จึงไม่อ้างว่า DIP ครอบคลุมทุกจุด |

## หลักฐานตามการทำงานจริง

- `ProjectController.java:87-145` ใช้ `ProjectFilterRequest` และส่งการค้นหาให้ Service; `status=ALL` รวม `ARCHIVED` ส่วนไม่ส่ง status จะซ่อน `ARCHIVED` โดยยังตัด `deletedAt` ออก (`dto/request/project/ProjectFilterRequest.java:23-30`, `service/impl/ProjectServiceImpl.java:203-225`)
- `ProjectServiceImpl.java:116-130,269-319,384-403` ประกอบ Client/Task progress และ `timeTracking` จาก Time Entry ที่จบแล้วให้ทั้ง Project detail และ list; เมื่อไม่มี `targetMinutes`, `usagePercent` เป็น `null`
- `ProjectServiceImpl.java:138-183` มี `countActiveAndCompleted()` กับ `getProgress()` สำหรับ Dashboard; `TaskServiceImpl.java:130-145` มี `getLatestTimeEntryTaskName()` ทั้งหมดเป็น service method ไม่ใช่ HTTP endpoint ใหม่
- `Project.java:185-226` ให้ State ปัจจุบันตัดสิน transition, สิทธิ์จับเวลา และสิทธิ์แก้ Task; `ARCHIVED` ย้อนเป็น `ACTIVE` หรือ `PLANNED` ผ่าน API ได้เฉพาะเมื่อ Project ยังไม่ถูก soft delete และ Client ยังใช้งานและไม่ถูก soft delete `ProjectServiceImpl.java:328-350` ห้ามแก้รายละเอียด Project ที่ `ARCHIVED` ส่วน `Project.archive()` ตั้ง `deletedAt` สำหรับ soft delete
- `ProjectServiceImpl.java:356-384,426-432` ประสานกฎสถานะกับ Task และ Time Entry: เมธอดเปลี่ยนสถานะและลบตรวจ running timer ของ Project; ก่อน `ACTIVE → COMPLETED` นับเฉพาะ Task ที่ยังใช้งานผ่าน `TaskRepository.summarizeProgressByProjectIds()` (`TaskRepository.java:79-95`) และปฏิเสธเมื่อยังมี Task ไม่เสร็จ จากนั้นจึงเรียก `TimeEntryService.lockByProject()` ใน transaction เดียวกัน โดยไม่ใส่กฎนับ Task หรือการล็อกเวลาไว้ใน State class; รายการเวลาที่ล็อกแล้วแก้หรือลบไม่ได้
- `TaskServiceImpl.java:67-103,179-227,300-304` ตรวจเจ้าของและสิทธิ์แก้ Task ตามสถานะ Project ก่อนบันทึก; Service ปฏิเสธ `OPEN → COMPLETED` สำหรับทั้งการเปลี่ยนสถานะและเส้น `/complete` ขณะที่ `Task.changeStatus()` ยังอนุญาต transition นี้เมื่อเรียกตรง ๆ; Task ที่ `COMPLETED` ย้อนเป็น `IN_PROGRESS` ได้โดยล้าง `completedAt` แต่ย้อนเป็น `OPEN` ไม่ได้ การลบใช้ `Task.softDelete()` แทนลบแถว (`Task.java:179-208`)
- `TimerStoppedProgressListener.java:34-70` รับ `TimerStoppedEvent` หลัง commit, คำนวณเกณฑ์ใหม่ แล้วเผยแพร่ `ProjectProgressThresholdEvent`; `ProjectProgressThresholdListener.java:14-22` เขียน log ยังไม่มี notification ถึงผู้ใช้
- `ClientServiceImpl.java:185-202` เปลี่ยน Project เป็น `ARCHIVED` เมื่อ Client ถูกตั้ง inactive โดยเรียก `Project.changeStatus()` ตรง ๆ ไม่ผ่านการตรวจ running timer ใน `ProjectServiceImpl`; ส่วนการ soft delete Client ไม่เปลี่ยนสถานะ Project

## ข้อจำกัดของการวิเคราะห์

1. `ProjectServiceImpl` รวมหลาย operation ของ Project เพื่อรองรับ feature ปัจจุบัน จึงไม่ควรกล่าวว่าแต่ละคลาสมีหน้าที่เดียวอย่างสมบูรณ์ เพียงแต่แยก HTTP, mapping, domain rule และ event handling ออกจากกันแล้ว
2. `TaskStatus` เป็น enum พร้อมกฎใน `Task.changeStatus()` และมีเงื่อนไขห้าม `OPEN → COMPLETED` เพิ่มใน `TaskServiceImpl` สำหรับ API จึงยังไม่ใช่ GoF State pattern ชุดเดียวกับ Project
3. ตัวอย่าง LSP/DIP เป็นหลักฐานเชิงโครงสร้าง ไม่ใช่ข้อพิสูจน์ว่าเปลี่ยน implementation ใด ๆ ได้โดยไม่กระทบพฤติกรรม
4. เลขบรรทัดควรตรวจซ้ำหลัง merge ก่อนนำไปคัดลอกลงเอกสารหลักของทีม

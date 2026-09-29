# SOLID Analysis: Project และ Task Management

**เจ้าของ feature:** `kantavit_673380027-4_01`  
**ขอบเขต:** Project และ Task API รวมถึง business logic สำหรับผู้ใช้ที่เข้าสู่ระบบ

เลขบรรทัดอ้างอิง implementation ปัจจุบันภายใต้ `code/Backend/src/main/java/th/ac/kku/freelance_hub/` และควรตรวจซ้ำเมื่อรวมเข้าเอกสารหลักหรือแก้โค้ด

| Principle | ไฟล์/คลาสและบรรทัด | เหตุผลและขอบเขตหลักฐาน |
|---|---|---|
| Single Responsibility | `controller/ProjectController.java:33-157`, `controller/TaskController.java:39-223` | Controller รับ request, ตรวจ DTO, อ่านผู้ใช้ปัจจุบัน และคืน HTTP response; ไม่เข้าถึง repository โดยตรง |
| Single Responsibility | `mapper/ProjectMapper.java:9-26`, `mapper/TaskMapper.java:9-24` | Mapper แปลง entity เป็น response DTO โดยไม่จัดการ HTTP หรือ persistence |
| Single Responsibility | `domain/entity/Project.java:171-196`, `domain/state/ProjectState.java:5-14`, `domain/state/ProjectStates.java:13-24`, `domain/entity/Task.java:141-170` | `Project` เก็บสถานะและประสานการเปลี่ยนสถานะ ส่วนคลาส State แต่ละตัวรับผิดชอบกฎของสถานะนั้น และ `Task` จัดการวงจรสถานะของตนเอง |
| Open/Closed | `domain/state/ProjectState.java:5-14`, `domain/state/ProjectStates.java:13-24`, `domain/state/PlannedState.java`, `domain/state/ActiveState.java`, `domain/state/OnHoldState.java`, `domain/state/CompletedState.java`, `domain/state/ArchivedState.java` | การปรับกฎของสถานะเดิมทำในคลาส State ของสถานะนั้น โดยไม่ต้องแก้ Controller หรือ TaskService; หากเพิ่มสถานะใหม่ ยังต้องเพิ่มค่าใน `ProjectStatus` และ mapping ใน `ProjectStates.from()` จึงเป็น OCP บางส่วน |
| Liskov Substitution | `service/impl/ProjectServiceImpl.java:35-36,59-202`, `service/impl/TaskServiceImpl.java:35-36,62-217` | Implementation ทำ operation ตาม interface และ Controller เรียกผ่าน interface ได้ แต่แต่ละ interface มี production implementation เดียว จึงยังไม่มีการพิสูจน์การแทนที่ระหว่าง implementation หลายตัว |
| Interface Segregation | `service/ProjectService.java:15-42`, `service/TaskService.java:13-40` | แยก contract ของ Project กับ Task; Controller ของแต่ละ feature ไม่ต้องพึ่งเมธอดของอีก feature |
| Dependency Inversion | `controller/ProjectController.java:35-36`, `controller/TaskController.java:41-42`, `service/impl/ProjectServiceImpl.java:41-57`, `service/impl/TaskServiceImpl.java:42-60` | Controller รับ service interface และ service รับ repository interface ผ่าน constructor แทนการสร้าง dependency เอง; mapper และ `UserService` ยังเป็น concrete class จึงเป็นการใช้ DIP บางส่วน |

## Evidence จาก Project และ Task flow

- Owner ID มาจากผู้ใช้ที่เข้าสู่ระบบ ไม่รับจาก request: `controller/ProjectController.java:155-157`, `controller/TaskController.java:222-224`
- การอ่าน Project รายตัวใช้ `projectId` ร่วมกับ `ownerId`; การอ่าน Task ใช้ `taskId`, `projectId` และเจ้าของโปรเจกต์: `service/impl/ProjectServiceImpl.java:204-210`, `service/impl/TaskServiceImpl.java:246-261`
- Service โหลดและบันทึก Project; `Project.changeStatus()` ให้ State ปัจจุบันตรวจ transition ผ่าน `ProjectStates.from(status)`: `service/impl/ProjectServiceImpl.java:183-193`, `domain/entity/Project.java:171-196`, `domain/state/ProjectStates.java:13-24`
- การเริ่ม timer ใช้ `Project.canTrackTime()` และการเปลี่ยนข้อมูล Task ใช้ `Project.canEditTasks()` ซึ่งมอบการตัดสินใจให้ State: `domain/entity/TimeEntry.java:125,262-269`, `service/impl/TaskServiceImpl.java:226-241`, `domain/entity/Project.java:191-196`
- การเริ่ม Task ตรวจสถานะ `OPEN` ใน entity ส่วน service ประสานการค้นหาและบันทึก: `domain/entity/Task.java:141-149`, `service/impl/TaskServiceImpl.java:130-140`
- การลบ Task ตรวจ Time Entry ก่อนลบ และจัดลำดับ Task ที่เหลือใหม่: `service/impl/TaskServiceImpl.java:191-216`

## ข้อสังเกตสำหรับรวมเอกสารหลัก

1. ตัวอย่าง OCP/LSP แสดงจุดขยายผ่าน interface แต่ไม่ควรอ้างว่ามีหลาย production implementation แล้ว
2. DIP ยังไม่ครอบคลุมทุก dependency เพราะ mapper และ `UserService` เป็น concrete class
3. ตรวจเลขบรรทัดอีกครั้งหลัง code freeze ก่อนนำตารางนี้ไปรวมใน `doc/solid-analysis.md`
